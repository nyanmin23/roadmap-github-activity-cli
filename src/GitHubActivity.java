import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

public class GitHubActivity {
    private static final String VALID_COMMAND = "github-activity";

    private static final Set<String> ALLOWED_EVENTS = Set.of(
            "PushEvent",
            "PullRequestEvent",
            "CreateEvent",
            "DeleteEvent"
    );

    public static void main(String[] args) {
        try (var scanner = new java.util.Scanner(System.in)) {
            while (true) {
                System.out.print("> "); // CLI prompt
                String input = scanner.nextLine().strip();
                if (input.isEmpty()) continue;

                try {
                    String[] prompt = input.split("\\s+");
                    handleCommand(prompt);
                } catch (InvalidCommandException | InvalidArgumentsException | MissingArgumentException |
                         IOException | InterruptedException | UserNotFoundException | RateLimitExceededException |
                         JacksonException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private static void handleCommand(String[] prompt) throws InvalidCommandException, InvalidArgumentsException,
            MissingArgumentException, IOException, InterruptedException, UserNotFoundException, RateLimitExceededException,
            JacksonException, UncheckedIOException {

        if (!evalCommand(prompt)) return;

        String username = extractUsername(prompt);
        String uriString = "https://api.github.com/users/" + username + "/events";

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uriString))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response, username);

        } catch (UncheckedIOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private static void checkStatus(HttpResponse<String> response, String username) throws UserNotFoundException,
            RateLimitExceededException, JacksonException {

        switch (response.statusCode()) {
            case 200 -> parseGitHubEvents(response, username);
            case 404 -> throw new UserNotFoundException();
            case 403 -> throw new RateLimitExceededException();
            default -> System.out.println("Warning: Received HTTP " + response.statusCode());
        }
    }

    private static void parseGitHubEvents(HttpResponse<String> response, String username) throws JacksonException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response.body());

        if (!root.isArray() || root.isEmpty()) {
            System.out.println("No recent GitHub events found for " + username);
            return;
        }

        for (JsonNode event : root) {
            String type = event.has("type") ? event.get("type").asString() : "UnknownEvent";
            if (!ALLOWED_EVENTS.contains(type)) continue;

            String repoName = event.has("repo") && event.get("repo").has("name")
                    ? event.get("repo").get("name").asString()
                    : "unknown repository";

            switch (type) {
                case "PushEvent" -> System.out.println("Pushed commit(s) to " + repoName);
                case "PullRequestEvent" -> System.out.println("Opened a pull request in " + repoName);
                case "CreateEvent" -> handleCreateDeleteEvent(event, repoName, "Created");
                case "DeleteEvent" -> handleCreateDeleteEvent(event, repoName, "Deleted");
            }
        }
    }

    private static void handleCreateDeleteEvent(JsonNode event, String repoName, String action) {
        String refType = event.has("ref_type") ? event.get("ref_type").asString() : "unknown";
        System.out.println(action + " " + refType + " in " + repoName);
    }

    private static boolean evalCommand(String[] prompt) throws InvalidCommandException, InvalidArgumentsException {
        if (!prompt[0].equals(VALID_COMMAND)) {
            throw new InvalidCommandException(prompt[0]);
        }
        if (prompt.length > 2) {
            throw new InvalidArgumentsException();
        }
        return true;
    }

    private static String extractUsername(String[] arg) throws MissingArgumentException {
        if (arg.length != 2) {
            throw new MissingArgumentException();
        }
        return arg[1].strip();
    }

    // ======== Exception Classes ========
    static class InvalidCommandException extends Exception {
        public InvalidCommandException(String command) {
            super("Error: command not found: '" + command + "'.\n" +
                    "Usage: " + VALID_COMMAND + " <username>");
        }
    }

    static class MissingArgumentException extends Exception {
        public MissingArgumentException() {
            super("Error: missing username.\nUsage: " + VALID_COMMAND + " <username>");
        }
    }

    static class InvalidArgumentsException extends Exception {
        public InvalidArgumentsException() {
            super("Error: too many arguments.\nUsage: " + VALID_COMMAND + " <username>");
        }
    }

    static class UserNotFoundException extends Exception {
        public UserNotFoundException() {
            super("Error: 404 Not Found — GitHub user not found.");
        }
    }

    static class RateLimitExceededException extends Exception {
        public RateLimitExceededException() {
            super("Error: 403 Forbidden — request rejected by GitHub API. " + "\n" +
                    "You may have exceeded the unauthenticated rate limit (60 requests per hour).");
        }
    }
}
