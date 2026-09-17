package domain.model;

public enum ChangeType {
    ADDED,
    MODIFIED,
    REMOVED;

    public static ChangeType fromDiffOp(String op) {
        return switch (op) {
            case "add" -> ADDED;
            case "replace" -> MODIFIED;
            case "remove" -> REMOVED;
            default -> throw new IllegalArgumentException("Unknown diff operation: " + op);
        };
    }
}
