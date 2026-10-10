package agentic_assistant.controller;

import agentic_assistant.service.WindowsCommandService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/command")
@CrossOrigin(origins = "http://localhost:5173")
public class CommandController {

    private final WindowsCommandService commandService;

    public CommandController(WindowsCommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping
    public String execute(@RequestParam String command) {
        return commandService.executeCommand(command);
    }
}
