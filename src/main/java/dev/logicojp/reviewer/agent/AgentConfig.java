package dev.logicojp.reviewer.agent;

import dev.logicojp.reviewer.config.ModelConfig;
import dev.logicojp.reviewer.skill.SkillDefinition;

import java.util.List;

/// Configuration model for a review agent.
/// Loaded from YAML files in the agents/ directory.
public record AgentConfig(
    String name,
    String displayName,
    String model,
    String systemPrompt,
    String instruction,
    String outputFormat,
    List<String> focusAreas,
    List<SkillDefinition> skills
) {

    private static final String DEFAULT_OUTPUT_FORMAT = """
        ## Output Format

        レビュー結果は必ず以下の形式で出力してください。複数の指摘がある場合は、それぞれについて以下の形式で記載してください。

        ---

        ### [指摘番号]. [タイトル]

        | 項目 | 内容 |
        |------|------|
        | **Priority** | Critical / High / Medium / Low のいずれか |
        | **指摘の概要** | 何が問題かの簡潔な説明 |
        | **修正しない場合の影響** | 放置した場合のリスクや影響 |
        | **該当箇所** | ファイルパスと行番号（例: `src/main/java/Example.java` L42-50） |

        **推奨対応**

        具体的な修正方法の説明。可能な場合はコード例を含める：

        ```
        // 修正前
        問題のあるコード

        // 修正後
        推奨されるコード
        ```

        **効果**

        この修正による改善効果の説明。

        ---

        ## Priority の基準
        - **Critical**: セキュリティ脆弱性、データ損失、本番障害につながる問題。即時対応必須
        - **High**: 重大なバグ、パフォーマンス問題、重要な機能の不具合。早急な対応が必要
        - **Medium**: コード品質の問題、保守性の低下、軽微なバグ。計画的に対応
        - **Low**: スタイルの問題、軽微な改善提案。時間があれば対応

        指摘がない場合は「指摘事項なし」と記載してください。
        """;

    public AgentConfig {
        name = name == null ? "" : name;
        displayName = (displayName == null || displayName.isBlank()) ? name : displayName;
        model = (model == null || model.isBlank()) ? ModelConfig.DEFAULT_MODEL : model;
        outputFormat = normalizeOutputFormat(outputFormat);
        focusAreas = focusAreas == null ? List.of() : List.copyOf(focusAreas);
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public AgentConfig withModel(String overrideModel) {
        return new AgentConfig(
            name,
            displayName,
            overrideModel,
            systemPrompt,
            instruction,
            outputFormat,
            focusAreas,
            skills
        );
    }

    public AgentConfig withSkills(List<SkillDefinition> newSkills) {
        return new AgentConfig(
            name,
            displayName,
            model,
            systemPrompt,
            instruction,
            outputFormat,
            focusAreas,
            newSkills
        );
    }

    /// Validates required fields. Delegates to {@link AgentConfigValidator}.
    public void validateRequired() {
        AgentConfigValidator.validateRequired(this);
    }

    private static String normalizeOutputFormat(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_OUTPUT_FORMAT;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("##")) {
            return trimmed;
        }
        return "## Output Format\n\n" + trimmed;
    }

    /// Builds the complete system prompt including output format instructions
    /// and output constraints (language, CoT suppression).
    public String buildFullSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            sb.append(systemPrompt.trim()).append("\n\n");
        }

        if (focusAreas != null && !focusAreas.isEmpty()) {
            sb.append("## Focus Areas\n\n");
            sb.append("以下の観点 **のみ** に基づいてレビューしてください。これ以外の観点での指摘は行わないでください。\n\n");
            for (String area : focusAreas) {
                sb.append("- ").append(area).append("\n");
            }
            sb.append("\n");
        }

        if (outputFormat != null && !outputFormat.isBlank()) {
            String outputText = outputFormat.trim();
            if (!outputText.startsWith("##")) {
                sb.append("## Output Format\n\n");
            }
            sb.append(outputText).append("\n");
        }

        return sb.toString();
    }

    public String buildInstruction(String repository) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalStateException("Instruction is not configured for agent: " + name);
        }

        String focusAreaText = formatFocusAreas();
        return instruction
            .replace("${repository}", repository)
            .replace("${displayName}", displayName != null ? displayName : name)
            .replace("${name}", name)
            .replace("${focusAreas}", focusAreaText);
    }

    /// Builds the instruction for a local directory review.
    /// Embeds the source code content directly in the prompt.
    /// @param targetName Display name of the target directory
    /// @param sourceContent Collected source code content
    /// @return The formatted instruction with embedded source code
    public String buildLocalInstruction(String targetName, String sourceContent) {
        if (instruction == null || instruction.isBlank()) {
            throw new IllegalStateException("Instruction is not configured for agent: " + name);
        }

        String focusAreaText = formatFocusAreas();
        
        // Replace placeholders
        String basePrompt = instruction
            .replace("${repository}", targetName)
            .replace("${displayName}", displayName != null ? displayName : name)
            .replace("${name}", name)
            .replace("${focusAreas}", focusAreaText);
        
        // Use StringBuilder to avoid large intermediate string copies
        return new StringBuilder(basePrompt.length() + sourceContent.length() + 64)
            .append(basePrompt)
            .append("\n\n以下は対象ディレクトリのソースコードです:\n\n")
            .append(sourceContent)
            .append("\n")
            .toString();
    }

    private String formatFocusAreas() {
        if (focusAreas == null || focusAreas.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (String area : focusAreas) {
            sb.append("- ").append(area).append("\n");
        }
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "AgentConfig{name='" + name + "', displayName='" + displayName + "'}";
    }
}
