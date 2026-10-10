package agentic_assistant.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5174")
public class AiController {

    private final ChatClient chatClient;

    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping
public String ask(@RequestParam String message) {

    System.out.println(">>> BEFORE GEMINI CALL");

    String response = chatClient.prompt()
            .user(message)
            .call()
            .content();

    System.out.println(">>> AFTER GEMINI CALL");

    return response;
}
}