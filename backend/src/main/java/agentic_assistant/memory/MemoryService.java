package agentic_assistant.memory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Tool(description = "Save information in long-term memory when the user explicitly asks the assistant to remember something. Provide a short key and the information to remember.")
    public String remember(
            @ToolParam(description = "Short topic or key for this memory, such as user_preference or project_name")
            String key,
            @ToolParam(description = "The information that should be remembered")
            String value) {

        if (key == null || key.isBlank()
                || value == null || value.isBlank()) {
            return "Please provide both a memory key and information.";
        }

        String cleanKey = key.trim();

        Optional<MemoryEntry> existing =
                memoryRepository.findFirstByMemoryKeyIgnoreCaseOrderByCreatedAtDesc(cleanKey);

        MemoryEntry entry;

        if (existing.isPresent()) {
            entry = existing.get();
            entry.setMemoryValue(value.trim());
        } else {
            entry = new MemoryEntry(cleanKey, value.trim());
        }

        memoryRepository.save(entry);
        return "Memory saved successfully: " + cleanKey;
    }

    @Tool(description = "Search long-term memory for information related to the user's question or topic.")
    public String recall(
            @ToolParam(description = "Topic or keywords to search for in saved memories")
            String query) {

        if (query == null || query.isBlank()) {
            return "Please specify what you want me to remember.";
        }

        List<MemoryEntry> results =
                memoryRepository.findByMemoryKeyContainingIgnoreCase(query.trim());

        if (results.isEmpty()) {
            return "No saved memories found for: " + query;
        }

        return results.stream()
                .map(entry -> entry.getMemoryKey() + ": "
                        + entry.getMemoryValue())
                .collect(Collectors.joining("\n"));
    }

    @Tool(description = "List all information currently stored in the assistant's long-term memory.")
    public String listMemories() {

        List<MemoryEntry> entries = memoryRepository.findAll();

        if (entries.isEmpty()) {
            return "The assistant's memory is currently empty.";
        }

        return entries.stream()
                .map(entry -> entry.getMemoryKey() + ": "
                        + entry.getMemoryValue())
                .collect(Collectors.joining("\n"));
    }
}