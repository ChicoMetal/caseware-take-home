package domain.model;

/** Semantic change type derived from a JSON Patch operation. */
public enum ChangeType {
    ADDED,
    MODIFIED,
    REMOVED;

    /** Maps JSON Patch ops to domain types: "add" -> ADDED, "replace" -> MODIFIED, "remove" -> REMOVED. */
    public static ChangeType fromDiffOp(String op) {
        return switch (op) {
            case "add" -> ADDED;
            case "replace" -> MODIFIED;
            case "remove" -> REMOVED;
            default -> throw new IllegalArgumentException("Unknown diff operation: " + op);
        };
    }
}
