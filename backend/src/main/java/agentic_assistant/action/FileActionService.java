
package agentic_assistant.action;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
public class FileActionService {

    @Tool(description = """
            Search for files and folders on the computer,
            list the contents of a folder, or read a text file.
            Use search_files for finding files by name.
            Use list_folder to list a folder's contents.
            Use read_file to read a text file.
            Use create_file to create a new text file.
            Do not use this tool to delete files or overwrite
            existing files.
            """)
    public String manageFiles(
            @ToolParam(description = """
                    Operation: search_files, list_folder,
                    read_file, or create_file.
                    """)
            String operation,

            @ToolParam(description = """
                    Absolute file or folder path.
                    For search_files, provide the folder to search.
                    """)
            String path,

            @ToolParam(description = """
                    Search term for search_files; file content
                    for create_file; otherwise leave empty.
                    """)
            String details) {

        if (operation == null || path == null || path.isBlank()) {
            return "Please provide an operation and a valid path.";
        }

        try {
            Path target = Paths.get(path).toAbsolutePath().normalize();
            String detail = details == null ? "" : details;

            switch (operation.trim().toLowerCase()) {

                case "list_folder":
                    if (!Files.isDirectory(target)) {
                        return "That folder does not exist.";
                    }

                    try (Stream<Path> entries = Files.list(target)) {
                        String result = entries
                                .limit(100)
                                .map(p -> (Files.isDirectory(p)
                                        ? "[Folder] "
                                        : "[File] ") + p.getFileName())
                                .reduce("", (a, b) -> a + b + "\n");

                        return result.isBlank()
                                ? "This folder is empty."
                                : result;
                    }

                case "search_files":
                    if (!Files.isDirectory(target)) {
                        return "The search folder does not exist.";
                    }
                    if (detail.isBlank()) {
                        return "Please provide a filename search term.";
                    }

                    try (Stream<Path> entries = Files.walk(target, 8)) {
                        String result = entries
                                .filter(p -> p.getFileName().toString()
                                        .toLowerCase()
                                        .contains(detail.toLowerCase()))
                                .limit(100)
                                .map(Path::toString)
                                .reduce("", (a, b) -> a + b + "\n");

                        return result.isBlank()
                                ? "No matching files or folders were found."
                                : result;
                    }

                case "read_file":
                    if (!Files.isRegularFile(target)) {
                        return "That file does not exist.";
                    }
                    if (Files.size(target) > 200_000) {
                        return "The file is too large to read in one request.";
                    }

                    return Files.readString(
                            target, StandardCharsets.UTF_8);

                case "create_file":
                    if (Files.exists(target)) {
                        return "The file already exists. I will not overwrite it.";
                    }
                    if (detail.isBlank()) {
                        return "Please provide the content for the new file.";
                    }
                    if (target.getParent() == null
                            || !Files.isDirectory(target.getParent())) {
                        return "The parent folder does not exist.";
                    }

                    Files.writeString(
                            target, detail, StandardCharsets.UTF_8);

                    return "Created file: " + target;

                default:
                    return "Unsupported file operation.";
            }

        } catch (IOException | SecurityException e) {
            return "File operation failed: " + e.getMessage();
        } catch (Exception e) {
            return "Could not process this file request.";
        }
    }
}