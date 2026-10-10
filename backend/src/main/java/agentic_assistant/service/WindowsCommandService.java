package agentic_assistant.service;

import agentic_assistant.action.DesktopActionService;
import agentic_assistant.action.FileActionService;
import agentic_assistant.memory.MemoryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class WindowsCommandService {

    private final ChatClient chatClient;
    private final DesktopActionService desktopActionService;
    private final FileActionService fileActionService;
    private final MemoryService memoryService;

    public WindowsCommandService(
            ChatClient.Builder builder,
            DesktopActionService desktopActionService,
            FileActionService fileActionService,
            MemoryService memoryService) {
        this.chatClient = builder.build();
        this.desktopActionService = desktopActionService;
        this.fileActionService = fileActionService;
        this.memoryService = memoryService;
    }

    public String executeCommand(String command) {
        if (command == null || command.isBlank()) {
            return "Please enter a command.";
        }

        String input = command.trim().toLowerCase()
                .replaceAll("[.!?]+$", "").trim();

        System.out.println("VOICE/COMMAND RECEIVED: [" + input + "]");

        try {
            switch (input) {
                case "open chrome":
                    return desktopActionService.executeAction("open_chrome", "");
                case "open files":
                case "open file explorer":
                    return desktopActionService.executeAction("open_files", "");
                case "open vscode":
                case "open vs code":
                case "open visual studio code":
                case "open my code editor":
                    return desktopActionService.executeAction("open_vscode", "");
                case "open calculator":
                case "open calc":
                    return desktopActionService.executeAction("open_calculator", "");
                case "open youtube":
                    return desktopActionService.executeAction(
                            "open_website", "https://www.youtube.com");
                case "open google":
                    return desktopActionService.executeAction(
                            "open_website", "https://www.google.com");
                case "open github":
                    return desktopActionService.executeAction(
                            "open_website", "https://github.com");
                case "open gmail":
                    return desktopActionService.executeAction(
                            "open_website", "https://mail.google.com");
            }

            String song = extractSongRequest(command);
            if (song != null) {
                return desktopActionService.executeAction("search_youtube", song);
            }

            return chatClient.prompt()
                    .tools(desktopActionService, fileActionService, memoryService)
                    .system("""
                        You are a helpful AI desktop assistant running
                        on the user's Windows computer.

                        Understand requests written or spoken naturally.
                        Use tools when a request requires a real action.

                        DESKTOP CAPABILITIES:
                        - Open Chrome, VS Code, File Explorer and Calculator.
                        - Open supported websites.
                        - Search YouTube for a topic or song.

                        FILE CAPABILITIES:
                        - List files and folders.
                        - Search for filenames inside a specified folder.
                        - Read text files and explain their contents.
                        - Create new text files without overwriting existing files.
                        - Use absolute Windows paths.
                        - Ask which folder or file the user means if unclear.
                        - Never claim to inspect a file unless a tool returned its contents.

                        MEMORY CAPABILITIES:
                        - Save information only when the user explicitly asks you to remember it.
                        - Use remember to save a short topic key and the information.
                        - Use recall to search previously saved memories.
                        - Use listMemories when asked what you remember.
                        - Never claim information was saved unless the memory tool confirms it.
                        - Never invent memories.

                        SAFETY:
                        - Never attempt arbitrary shell commands.
                        - Do not delete files, overwrite existing files, or make system-wide changes.
                        - Use only available tools for actions and report results honestly.
                        - If a capability is unavailable, explain that it is not supported yet.

                        Answer general questions normally.
                        """)
                    .user(command)
                    .call()
                    .content();

        } catch (Exception e) {
            e.printStackTrace();
            return "I couldn't process that request. Check the backend logs.";
        }
    }

    private String extractSongRequest(String command) {
        String text = command.trim();
        String lower = text.toLowerCase();

        String[] prefixes = {
                "play ",
                "play song ",
                "play the song ",
                "play music "
        };

        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                String song = text.substring(prefix.length()).trim();
                if (!song.isEmpty()) {
                    return song;
                }
            }
        }

        return null;
    }
}