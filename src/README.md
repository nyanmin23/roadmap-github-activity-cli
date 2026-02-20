---

# GitHub Activity CLI

## Overview

GitHub Activity CLI is a command-line Java application that retrieves and displays recent public activity for a given GitHub user.

The program communicates with the GitHub REST API (`/users/{username}/events` endpoint), parses the returned JSON using the Jackson library, and prints a simplified summary of selected event types.

The application runs in interactive CLI mode and continuously accepts commands until manually terminated.

---

## Features

The program:

* Accepts a command in the format:

  github-activity <username>

* Sends an HTTP GET request to:

  [https://api.github.com/users/{username}/events](https://api.github.com/users/{username}/events)

* Parses JSON responses using Jackson.

* Displays summaries for the following event types:

    * PushEvent
    * PullRequestEvent
    * CreateEvent
    * DeleteEvent

* Handles errors:

    * Invalid command
    * Missing or extra arguments
    * GitHub user not found (404)
    * GitHub API rate limit exceeded (403)

---

## Example Usage

```
> github-activity torvalds
Pushed commit(s) to torvalds/linux
Created branch in torvalds/subsurface
Opened a pull request in torvalds/linux
```

If the user does not exist:

```
Error: 404 Not Found — GitHub user not found.
```

If rate limit is exceeded:

```
Error: 403 Forbidden — request rejected by GitHub API.
You may have exceeded the unauthenticated rate limit (60 requests per hour).
```

---

## Requirements

* Java 17 or newer (required for `HttpClient`)
* Jackson Databind library
* Internet connection

---

## Dependencies

Jackson is required for JSON parsing.

Maven coordinates:

Group ID:
com.fasterxml.jackson.core

Artifact ID:
jackson-databind

You must also include:

* jackson-core
* jackson-annotations

If downloading manually from Maven Central, search:

jackson-databind

Download the `.jar` files and include them in your classpath.

---

## Compilation

If using manual JAR files:

```
javac -cp ".;lib/*" GitHubActivity.java
```

(macOS/Linux)

```
javac -cp ".:lib/*" GitHubActivity.java
```

Assumes:

* All Jackson JAR files are placed in a `lib/` directory.

---

## Running the Program

Windows:

```
java -cp ".;lib/*" GitHubActivity
```

macOS/Linux:

```
java -cp ".:lib/*" GitHubActivity
```

After starting, the CLI prompt will appear:

```
>
```

Enter commands in the format:

```
github-activity <username>
```

To exit, terminate manually (Ctrl+C).

---

## Design Notes

* Uses Java built-in `HttpClient` (java.net.http).
* Uses Jackson `ObjectMapper` and `JsonNode` (Tree Model).
* Filters only selected event types.
* Ignores unsupported event types.
* Implements custom checked exceptions for structured CLI error handling.
* Designed defensively to handle missing JSON fields.

---
