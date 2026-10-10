
package agentic_assistant.action;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class DesktopActionService {

    @Tool(description = """
            Execute a supported Windows desktop action.
            Use open_chrome to open Chrome.
            Use open_vscode to open Visual Studio Code.
            Use open_files to open File Explorer.
            Use open_calculator to open Calculator.
            Use open_website to open an allowed website.
            Use search_youtube to search YouTube for a topic or song.
            Never invent unsupported actions.
            """)
    public String executeAction(
            @ToolParam(description = """
                    Action name: open_chrome, open_vscode,
                    open_files, open_calculator, open_website,
                    or search_youtube.
                    """)
            String action,

            @ToolParam(description = """
                    For open_website, provide an allowed website URL.
                    For search_youtube, provide a search query.
                    For other actions, use an empty string.
                    """)
            String argument) {

        if (action == null || action.isBlank()) {
            return "Please specify an action.";
        }

        String value = argument == null ? "" : argument.trim();

        try {
            switch (action.trim().toLowerCase()) {

                case "open_chrome":
                    new ProcessBuilder(
                            "cmd", "/c", "start", "", "chrome"
                    ).start();
                    return "Opening Chrome.";

                case "open_vscode":
                    new ProcessBuilder(
                            "cmd", "/c", "start", "", "code"
                    ).start();
                    return "Opening VS Code.";

                case "open_files":
                    new ProcessBuilder("explorer.exe").start();
                    return "Opening File Explorer.";

                case "open_calculator":
                    new ProcessBuilder("calc.exe").start();
                    return "Opening Calculator.";

                case "open_website":
                    if (!isAllowedWebsite(value)) {
                        return "That website isn't supported yet.";
                    }

                    new ProcessBuilder(
                            "cmd", "/c", "start", "", value
                    ).start();

                    return "Opening website: " + value;

                case "search_youtube":
                    if (value.isBlank()) {
                        return "Please provide a YouTube search query.";
                    }

                    String query = URLEncoder.encode(
                            value,
                            StandardCharsets.UTF_8
                    );

                    String url =
                            "https://www.youtube.com/results?search_query="
                                    + query;

                    new ProcessBuilder(
                            "cmd", "/c", "start", "", url
                    ).start();

                    return "Searching YouTube for: " + value;

                default:
                    return "That action isn't supported yet.";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "The action failed. Check the backend logs.";
        }
    }

    private boolean isAllowedWebsite(String url) {
        return url.equalsIgnoreCase("https://www.youtube.com")
                || url.equalsIgnoreCase("https://www.google.com")
                || url.equalsIgnoreCase("https://github.com")
                || url.equalsIgnoreCase("https://mail.google.com");
    }
}