package dev.logicojp.reviewer.instruction;

/// Represents a loaded custom instruction with its source information.
/// Supports GitHub Copilot's per-scope instruction format with applyTo glob and description.
///
/// @param sourcePath The path where the instruction was loaded from
/// @param content The instruction content
/// @param source The type of source (local file, GitHub, merged)
/// @param applyTo The glob pattern specifying which files this instruction applies to (nullable)
/// @param description A brief description of this instruction (nullable)
public record CustomInstruction(
    String sourcePath,
    String content,
    InstructionSource source,
    String applyTo,
    String description
) {
    /// Checks if this instruction has no meaningful content.
    ///
    /// @return true if content is null or blank
    public boolean isEmpty() {
        return content == null || content.isBlank();
    }
    
    /// Returns true if this instruction has scope metadata (applyTo or description).
    public boolean hasMetadata() {
        return (applyTo != null && !applyTo.isBlank()) 
            || (description != null && !description.isBlank());
    }
    
    /// Creates a prompt section for this custom instruction.
    /// Includes applyTo and description metadata when present.
    ///
    /// @return Formatted prompt section string
    public String toPromptSection() {
        var sb = new StringBuilder();
        sb.append("## カスタムインストラクション\n\n");
        sb.append("以下のプロジェクト固有の指示に従ってください:\n\n");
        if (applyTo != null && !applyTo.isBlank()) {
            sb.append("**適用対象**: `").append(applyTo.trim()).append("`\n\n");
        }
        if (description != null && !description.isBlank()) {
            sb.append("**説明**: ").append(description.trim()).append("\n\n");
        }
        sb.append(content);
        return sb.toString();
    }
}
