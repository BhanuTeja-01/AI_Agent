import { useEffect, useRef, useState } from "react";
import "./App.css";

function App() {
  const [message, setMessage] = useState("");
  const [response, setResponse] = useState("");
  const [orbState, setOrbState] = useState("idle");
  const [listening, setListening] = useState(false);
  const [history, setHistory] = useState([]);
  const [focused, setFocused] = useState(false);
  const [online, setOnline] = useState(true);
  const [voiceLevel, setVoiceLevel] = useState(0);

  const inputRef = useRef(null);

  const recognitionRef = useRef(null);
  const micStreamRef = useRef(null);
  const audioContextRef = useRef(null);
  const analyserRef = useRef(null);
  const animationFrameRef = useRef(null);

  /* =========================
     COMMAND ICON
  ========================= */

  const getCommandIcon = (command) => {
    const value = command.toLowerCase();

    if (value.includes("chrome")) return "◉";
    if (value.includes("vscode") || value.includes("vs code")) return "</>";
    if (value.includes("file")) return "▣";
    if (value.includes("calculator") || value.includes("calc")) return "123";

    return "✦";
  };

  /* =========================
     HISTORY
  ========================= */

  const addHistory = (command, result, success) => {
    setHistory((previous) => [
      {
        command,
        result,
        success,
        time: new Date().toLocaleTimeString([], {
          hour: "2-digit",
          minute: "2-digit",
        }),
      },
      ...previous.slice(0, 4),
    ]);
  };

  /* =========================
     STOP MICROPHONE
  ========================= */

  const stopVoiceVisualization = () => {
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
      animationFrameRef.current = null;
    }

    if (micStreamRef.current) {
      micStreamRef.current.getTracks().forEach((track) => {
        track.stop();
      });

      micStreamRef.current = null;
    }

    if (audioContextRef.current) {
      audioContextRef.current.close().catch(() => {});
      audioContextRef.current = null;
    }

    analyserRef.current = null;

    setVoiceLevel(0);
  };

  /* =========================
     REAL VOICE VISUALIZATION
  ========================= */

  const startVoiceVisualization = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: true,
      });

      micStreamRef.current = stream;

      const AudioContext =
        window.AudioContext || window.webkitAudioContext;

      if (!AudioContext) {
        return;
      }

      const audioContext = new AudioContext();

      const analyser = audioContext.createAnalyser();

      analyser.fftSize = 256;
      analyser.smoothingTimeConstant = 0.75;

      const source =
        audioContext.createMediaStreamSource(stream);

      source.connect(analyser);

      audioContextRef.current = audioContext;
      analyserRef.current = analyser;

      const dataArray = new Uint8Array(
        analyser.frequencyBinCount
      );

      const updateVoiceLevel = () => {
        if (!analyserRef.current) {
          return;
        }

        analyser.getByteFrequencyData(dataArray);

        let total = 0;

        for (let i = 0; i < dataArray.length; i++) {
          total += dataArray[i];
        }

        const average = total / dataArray.length;

        const normalizedLevel = Math.min(
          1,
          average / 65
        );

        setVoiceLevel(normalizedLevel);

        animationFrameRef.current =
          requestAnimationFrame(updateVoiceLevel);
      };

      updateVoiceLevel();

    } catch (error) {
      console.error(
        "Microphone visualization error:",
        error
      );
    }
  };

  /* =========================
     SEND COMMAND
  ========================= */

  const sendCommand = async (command = message) => {
    const trimmedCommand = command.trim();

    if (!trimmedCommand) {
      setResponse("Please enter a command.");
      setOrbState("error");

      setTimeout(() => {
        setOrbState("idle");
      }, 1500);

      return;
    }

    setMessage(trimmedCommand);
    setFocused(false);

    setOrbState("processing");
    setResponse("Understanding your command...");

    try {
      const res = await fetch(
        `http://localhost:8080/api/command?command=${encodeURIComponent(
          trimmedCommand
        )}`
      );

      if (!res.ok) {
        throw new Error("Backend returned an error");
      }

      const data = await res.text();

      setOnline(true);

      const lowerData = data.toLowerCase();

      const success =
        !lowerData.includes("don't know") &&
        !lowerData.includes("failed");

      if (success) {
        setOrbState("executing");
        setResponse(data);

        addHistory(
          trimmedCommand,
          data,
          true
        );

        setTimeout(() => {
          setOrbState("success");
        }, 600);

        setTimeout(() => {
          setOrbState("idle");
        }, 2800);

      } else {
        setOrbState("error");
        setResponse(data);

        addHistory(
          trimmedCommand,
          data,
          false
        );

        setTimeout(() => {
          setOrbState("idle");
        }, 2200);
      }

    } catch (error) {
      console.error(error);

      setOnline(false);

      setOrbState("error");

      setResponse(
        "Could not connect to the backend."
      );

      addHistory(
        trimmedCommand,
        "Could not connect to the backend.",
        false
      );

      setTimeout(() => {
        setOrbState("idle");
      }, 2200);
    }
  };

  /* =========================
     VOICE COMMAND
  ========================= */

  const startVoiceCommand = async () => {
    const SpeechRecognition =
      window.SpeechRecognition ||
      window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      setResponse(
        "Voice recognition is not supported in this browser."
      );

      setOrbState("error");

      return;
    }

    if (listening) {
      return;
    }

    const recognition = new SpeechRecognition();

    recognitionRef.current = recognition;

    recognition.lang = "en-US";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.continuous = false;

    setListening(true);
    setFocused(false);

    setOrbState("listening");
    setResponse("Listening...");

    await startVoiceVisualization();

    try {
      recognition.start();
    } catch (error) {
      console.error(error);

      setListening(false);
      stopVoiceVisualization();

      setOrbState("error");
      setResponse("Could not start voice recognition.");

      setTimeout(() => {
        setOrbState("idle");
      }, 2000);
    }

    recognition.onresult = (event) => {
      stopVoiceVisualization();

      const transcript =
        event.results[0][0].transcript;

      setMessage(transcript);
      setListening(false);

      setOrbState("processing");
      setResponse("Understanding your command...");

      sendCommand(transcript);
    };

    recognition.onerror = (event) => {
      console.error(
        "Speech recognition error:",
        event.error
      );

      stopVoiceVisualization();

      setListening(false);

      if (event.error === "not-allowed") {
        setResponse(
          "Microphone permission was denied."
        );
      } else {
        setResponse(
          "Could not understand the voice command."
        );
      }

      setOrbState("error");

      setTimeout(() => {
        setOrbState("idle");
      }, 2200);
    };

    recognition.onend = () => {
      setListening(false);
      stopVoiceVisualization();
    };
  };

  /* =========================
     INPUT FOCUS
  ========================= */

  const handleInputFocus = () => {
    setFocused(true);

    if (orbState === "idle") {
      setOrbState("input");
    }
  };

  const handleInputBlur = () => {
    if (
      !message.trim() &&
      orbState === "input"
    ) {
      setFocused(false);
      setOrbState("idle");
    }
  };

  /* =========================
     ORB TEXT
  ========================= */

  const getOrbText = () => {
    switch (orbState) {
      case "input":
        return "Ready for your command";

      case "listening":
        return "Listening...";

      case "processing":
        return "Understanding your command...";

      case "executing":
        return response || "Executing command...";

      case "success":
        return response || "Command completed";

      case "error":
        return response || "Something went wrong";

      default:
        return "How can I assist you today?";
    }
  };

  /* =========================
     ORB ICON
  ========================= */

  const getOrbIcon = () => {
    switch (orbState) {
      case "listening":
        return "◉";

      case "processing":
        return "✦";

      case "executing":
        return getCommandIcon(message);

      case "success":
        return "✓";

      case "error":
        return "!";

      default:
        return "✦";
    }
  };

  /* =========================
     CLEANUP
  ========================= */

  useEffect(() => {
    inputRef.current?.focus();

    return () => {
      stopVoiceVisualization();

      if (recognitionRef.current) {
        try {
          recognitionRef.current.abort();
        } catch (error) {
          console.error(error);
        }
      }
    };
  }, []);

  /* =========================
     UI
  ========================= */

  return (
    <div className="app-shell">

      {/* Background glow */}
      <div className="background-glow glow-one"></div>
      <div className="background-glow glow-two"></div>

      {/* =========================
          SIDEBAR
      ========================= */}

      <aside className="sidebar">

        <div className="brand-mark">
          <span>✦</span>
        </div>

        <div className="sidebar-menu">

          <button className="sidebar-button active">
            <span>✦</span>
            <small>Assistant</small>
          </button>

          <button
            className="sidebar-button"
            onClick={startVoiceCommand}
          >
            <span>◉</span>
            <small>Voice</small>
          </button>

          <button className="sidebar-button">
            <span>◷</span>
            <small>History</small>
          </button>

          <button className="sidebar-button">
            <span>⚙</span>
            <small>Settings</small>
          </button>

        </div>

        <div className="sidebar-bottom">
          <div
            className={`status-dot ${
              online ? "online" : "offline"
            }`}
          ></div>
        </div>

      </aside>

      {/* =========================
          MAIN
      ========================= */}

      <main className="main-content">

        {/* Top bar */}

        <header className="top-bar">

          <div>
            <div className="app-title">
              AI AGENT
            </div>

            <div className="app-subtitle">
              Your intelligent Windows assistant
            </div>
          </div>

          <div
            className={`connection-status ${
              online ? "" : "offline"
            }`}
          >
            <span className="connection-dot"></span>

            {online ? "ONLINE" : "OFFLINE"}
          </div>

        </header>

        {/* =========================
            ASSISTANT AREA
        ========================= */}

        <section className="assistant-area">

          {/* ORB */}

          <div
            className={`orb-container ${orbState} ${
              focused ? "focused" : ""
            }`}
            style={{
              "--voice-level":
                orbState === "listening"
                  ? voiceLevel
                  : 0,
            }}
          >

            {/* Rotating rings */}

            <div className="orb-ring ring-one"></div>
            <div className="orb-ring ring-two"></div>
            <div className="orb-ring ring-three"></div>

            {/* Voice waves */}

            {orbState === "listening" && (
              <>
                <div className="voice-wave wave-one"></div>
                <div className="voice-wave wave-two"></div>
                <div className="voice-wave wave-three"></div>
              </>
            )}

            {/* Main orb */}

            <div className="orb">

              <div className="orb-inner"></div>

              <div className="orb-core">
                <span>{getOrbIcon()}</span>
              </div>

              <div className="orb-particle particle-one"></div>
              <div className="orb-particle particle-two"></div>
              <div className="orb-particle particle-three"></div>

            </div>

          </div>

          {/* ORB STATUS */}

          <div
            className={`assistant-status ${orbState}`}
          >

            <div className="status-label">

              {orbState === "success"
                ? "COMPLETED"
                : orbState === "error"
                ? "ERROR"
                : orbState === "listening"
                ? "VOICE INPUT"
                : orbState === "processing"
                ? "PROCESSING"
                : orbState === "executing"
                ? "EXECUTING"
                : ""}

            </div>

            <h1>{getOrbText()}</h1>

          </div>

          {/* =========================
              RECENT ACTIVITY
          ========================= */}

          {history.length > 0 && (
            <div className="activity-section">

              <div className="section-heading">

                <span>Recent Activity</span>

                <span className="activity-count">
                  {history.length}
                </span>

              </div>

              <div className="activity-list">

                {history.map((item, index) => (

                  <div
                    className="activity-item"
                    key={index}
                  >

                    <div
                      className={`activity-icon ${
                        item.success
                          ? "success"
                          : "error"
                      }`}
                    >
                      {item.success ? "✓" : "!"}
                    </div>

                    <div className="activity-info">

                      <strong>
                        {item.command}
                      </strong>

                      <span>
                        {item.result}
                      </span>

                    </div>

                    <time>
                      {item.time}
                    </time>

                  </div>

                ))}

              </div>

            </div>
          )}

          {/* =========================
              COMMAND INPUT
          ========================= */}

          <div
            className={`command-wrapper ${
              focused ? "focused" : ""
            }`}
          >

            <div className="input-row">

              <button
                className="add-button"
                title="More options"
              >
                +
              </button>

              <input
                ref={inputRef}
                type="text"
                value={message}
                onChange={(event) =>
                  setMessage(event.target.value)
                }
                onFocus={handleInputFocus}
                onBlur={handleInputBlur}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    sendCommand();
                  }
                }}
                placeholder="Ask your assistant..."
              />

              <button
                className={`voice-button ${
                  listening ? "listening" : ""
                }`}
                onClick={startVoiceCommand}
                disabled={listening}
                title="Voice command"
              >
                {listening ? "◉" : "🎤"}
              </button>

              <button
                className="send-button"
                onClick={() => sendCommand()}
                title="Send command"
              >
                <span>➤</span>
              </button>

            </div>

            {/* Quick suggestions */}

            <div className="input-hints">

              <span>Try:</span>

              <button
                onClick={() =>
                  sendCommand("Open Chrome")
                }
              >
                Open Chrome
              </button>

              <button
                onClick={() =>
                  sendCommand("Open VS Code")
                }
              >
                Open VS Code
              </button>

              <button
                onClick={() =>
                  sendCommand("Open Files")
                }
              >
                Open Files
              </button>

              <button
                onClick={() =>
                  sendCommand("Open Calculator")
                }
              >
                Open Calculator
              </button>

            </div>

          </div>

        </section>

      </main>

    </div>
  );
}

export default App;