# Multi-Agent Code Reviewer

AI-powered parallel code review tool that orchestrates multiple specialized agents using the GitHub Copilot SDK for Java.

## Prerequisites

- Java: GraalVM 26 EA (Java 26, preview features enabled)
- Build: Maven 3.9+
- Auth: GitHub CLI (`gh`) and GitHub Copilot CLI (`github-copilot` or `copilot`)

This repository uses `.sdkmanrc` for local JDK alignment:

```bash
sdk env install
sdk env
```

## Quick Start

```bash
mvn clean package
java --enable-preview -jar target/multi-agent-reviewer-1.0.0-SNAPSHOT.jar run --repo owner/repo --all
```

## Architecture

Execution flow:

1. `ReviewApp` parses CLI arguments and dispatches commands.
2. `ReviewCommand` resolves target/agents/models/options.
3. `ReviewOrchestrator` runs each agent in parallel (virtual threads + structured concurrency).
4. `ReviewAgent` invokes Copilot SDK and generates per-agent review results.
5. `ReportGenerator` and `SummaryGenerator` build markdown outputs.

Main directories:

- `src/main/java/dev/logicojp/reviewer/cli`: command parsing and command handlers
- `src/main/java/dev/logicojp/reviewer/orchestrator`: parallel execution pipeline
- `src/main/java/dev/logicojp/reviewer/agent`: agent loading and prompt construction
- `templates/`: markdown templates used for report and summary generation
- `agents/`: built-in `.agent.md` definitions

## Configuration

Core configuration lives in `src/main/resources/application.yml`.

- `reviewer.execution.*`: parallelism, timeout, retry, buffer settings
- `reviewer.models.*`: review/report/summary model selection
- `reviewer.templates.*`: template directory and template filenames
- `reviewer.summary.*`: prompt sizing and fallback behavior
- `reviewer.skills.*`: global skill discovery and executor cache settings

Useful runtime environment variables:

- `GITHUB_TOKEN`: token for SDK authentication (optional when `gh` login is available)
- `COPILOT_CLI_PATH`: explicit path to Copilot CLI executable
- `GH_CLI_PATH`: explicit path to GitHub CLI executable

## Security Runtime Notes

For production JVM runs that handle GitHub tokens, consider enabling these flags:

```bash
java --enable-preview \
	-XX:+DisableAttachMechanism \
	-XX:-HeapDumpOnOutOfMemoryError \
	-jar target/multi-agent-reviewer-1.0.0-SNAPSHOT.jar run --repo owner/repo --all
```

## Development

```bash
# Build fat jar
mvn clean package

# Build native image (GraalVM required)
mvn clean package -Pnative

# Run tests
mvn test

# Run one test class
mvn test -Dtest=ModelConfigTest
```

## Documentation

- English: [README_en.md](./README_en.md)
- 日本語: [README_ja.md](./README_ja.md)
- Release Notes (EN): [RELEASE_NOTES_en.md](./RELEASE_NOTES_en.md)
- リリースノート (JA): [RELEASE_NOTES_ja.md](./RELEASE_NOTES_ja.md)
