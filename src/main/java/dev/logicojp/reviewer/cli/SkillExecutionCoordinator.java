package dev.logicojp.reviewer.cli;

import dev.logicojp.reviewer.service.CopilotCliException;
import dev.logicojp.reviewer.service.CopilotService;
import dev.logicojp.reviewer.service.SkillService;
import dev.logicojp.reviewer.skill.SkillResult;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;

@Singleton
class SkillExecutionCoordinator {

    @FunctionalInterface
    interface Initializer {
        void initialize(String resolvedToken);
    }

    @FunctionalInterface
    interface SkillRunner {
        SkillResult run(String skillId, Map<String, String> parameters, String resolvedToken, String model,
                        long timeoutMinutes);
    }

    @FunctionalInterface
    interface Shutdowner {
        void shutdown();
    }

    private final Initializer initializer;
    private final SkillRunner skillRunner;
    private final Shutdowner shutdowner;
    private final CliOutput output;

    @Inject
    public SkillExecutionCoordinator(CopilotService copilotService,
                                     SkillService skillService,
                                     CliOutput output) {
        this(
            copilotService::initializeOrThrow,
            (skillId, parameters, resolvedToken, model, timeoutMinutes) ->
                skillService.executeSkill(skillId, parameters, resolvedToken, model),
            copilotService::shutdown,
            output
        );
    }

    SkillExecutionCoordinator(Initializer initializer,
                              SkillRunner skillRunner,
                              Shutdowner shutdowner,
                              CliOutput output) {
        this.initializer = initializer;
        this.skillRunner = skillRunner;
        this.shutdowner = shutdowner;
        this.output = output;
    }

    public int execute(String skillId,
                       Map<String, String> parameters,
                       String resolvedToken,
                       String model,
                       long timeoutMinutes) {
        try {
            return LifecycleRunner.executeWithLifecycle(
                () -> initializer.initialize(resolvedToken),
                () -> runAndHandleResult(skillId, parameters, resolvedToken, model, timeoutMinutes),
                shutdowner::shutdown
            );
        } catch (RuntimeException e) {
            throw toExecutionFailure(e);
        }
    }

    private int runAndHandleResult(String skillId,
                                   Map<String, String> parameters,
                                   String resolvedToken,
                                   String model,
                                   long timeoutMinutes) {
        printExecutionStart(skillId, parameters);
        SkillResult result = skillRunner.run(skillId, parameters, resolvedToken, model, timeoutMinutes);
        return toExitCode(result);
    }

    private void printExecutionStart(String skillId, Map<String, String> parameters) {
        output.println("Executing skill: " + skillId);
        output.println("Parameters: " + parameters.keySet());
    }

    private int toExitCode(SkillResult result) {
        if (result.success()) {
            return handleSuccessfulResult(result);
        }
        return handleFailedResult(result);
    }

    private int handleSuccessfulResult(SkillResult result) {
        output.println("=== Skill Result ===\n");
        output.println(result.content());
        return ExitCodes.OK;
    }

    private int handleFailedResult(SkillResult result) {
        output.errorln("Skill execution failed: " + result.errorMessage());
        return ExitCodes.SOFTWARE;
    }

    private CopilotCliException toExecutionFailure(Exception cause) {
        return new CopilotCliException("Skill execution failed", cause);
    }
}
