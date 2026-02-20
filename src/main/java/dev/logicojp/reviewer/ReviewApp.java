package dev.logicojp.reviewer;

import dev.logicojp.reviewer.cli.CliOutput;
import dev.logicojp.reviewer.cli.CliParsing;
import dev.logicojp.reviewer.cli.ExitCodes;
import dev.logicojp.reviewer.cli.ListAgentsCommand;
import dev.logicojp.reviewer.cli.ReviewCommand;
import dev.logicojp.reviewer.cli.SkillCommand;
import io.micronaut.context.ApplicationContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/// Multi-Agent Code Reviewer CLI Application.
@Singleton
public class ReviewApp {

    private static final Set<String> SUBCOMMANDS = Set.of("run", "list", "skill");

    private static final String GENERAL_USAGE = """
            Usage: review <command> [options]

            Commands:
                run          Execute a multi-agent code review
                list         List available review agents
                skill        Execute a specific agent skill

            Global options:
                -v, --verbose    Enable debug logging
                -V, --version    Show version
                -h, --help       Show this help

            Run 'review <command> --help' for command-specific help.
            """;

    private final ReviewCommand reviewCommand;
    private final ListAgentsCommand listAgentsCommand;
    private final SkillCommand skillCommand;
    private final CliOutput output;

    @Inject
    public ReviewApp(ReviewCommand reviewCommand,
                     ListAgentsCommand listAgentsCommand,
                     SkillCommand skillCommand,
                     CliOutput output) {
        this.reviewCommand = reviewCommand;
        this.listAgentsCommand = listAgentsCommand;
        this.skillCommand = skillCommand;
        this.output = output;
    }

    public static void main(String[] args) {
        try (var context = ApplicationContext.run()) {
            var app = context.getBean(ReviewApp.class);
            int exitCode = app.execute(args);
            if (exitCode != 0) {
                System.exit(exitCode);
            }
        }
    }

    public int execute(String[] args) {
        if (args == null || args.length == 0) {
            output.out().print(GENERAL_USAGE);
            return ExitCodes.USAGE;
        }

        GlobalOptions globalOptions = parseGlobalOptions(args);
        if (globalOptions.verbose()) {
            enableVerboseLogging();
        }

        if (globalOptions.versionRequested()) {
            String version = getClass().getPackage().getImplementationVersion();
            output.println("Multi-Agent Reviewer " + (version != null ? version : "dev"));
            return ExitCodes.OK;
        }

        String[] filteredArgs = globalOptions.remainingArgs().toArray(String[]::new);
        if (filteredArgs.length == 0) {
            output.out().print(GENERAL_USAGE);
            return ExitCodes.USAGE;
        }

        // --help / -h without a subcommand → general help
        boolean hasHelpFlag = CliParsing.hasHelpFlag(filteredArgs);
        boolean hasSubcommand = Arrays.stream(filteredArgs).anyMatch(SUBCOMMANDS::contains);
        if (hasHelpFlag && !hasSubcommand) {
            output.out().print(GENERAL_USAGE);
            return ExitCodes.OK;
        }

        int startIndex = 0;
        if ("review".equals(filteredArgs[0])) {
            if (filteredArgs.length == 1) {
                output.out().print(GENERAL_USAGE);
                return ExitCodes.USAGE;
            }
            startIndex = 1;
        }

        String command = filteredArgs[startIndex];
        String[] commandArgs = Arrays.copyOfRange(filteredArgs, startIndex + 1, filteredArgs.length);

        return switch (command) {
            case "run" -> reviewCommand.execute(commandArgs);
            case "list" -> listAgentsCommand.execute(commandArgs);
            case "skill" -> skillCommand.execute(commandArgs);
            default -> {
                output.errorln("Unknown command: " + command);
                output.out().print(GENERAL_USAGE);
                yield ExitCodes.USAGE;
            }
        };
    }

    private GlobalOptions parseGlobalOptions(String[] args) {
        boolean verbose = false;
        boolean versionRequested = false;
        List<String> remaining = new ArrayList<>();
        for (String arg : args) {
            switch (arg) {
                case "-v", "--verbose" -> verbose = true;
                case "-V", "--version" -> versionRequested = true;
                default -> remaining.add(arg);
            }
        }
        return new GlobalOptions(verbose, versionRequested, List.copyOf(remaining));
    }

    private record GlobalOptions(boolean verbose, boolean versionRequested, List<String> remainingArgs) {}

    private void enableVerboseLogging() {
        if (!LogbackLevelSwitcher.setDebug()) {
            output.errorln("Failed to enable verbose logging (Logback not available)");
        }
    }
}
