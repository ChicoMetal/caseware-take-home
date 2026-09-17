package domain.model;

/**
 * Single JSON Patch-style operation from a template diff.
 *
 * @param op    the operation type: "add", "replace", or "remove"
 * @param path  JSON Pointer path, e.g. {@code /sections/materiality/threshold}
 * @param value the new value (for "add" operations)
 */
public record DiffOperation(
    String op,
    String path,
    Object value,
    Object oldValue,
    Object newValue
) {

    /** Extracts the section key from {@code path} (second segment after {@code /sections/}). */
    public String sectionKey() {
        String[] segments = path.split("/");
        // path format: /sections/{sectionKey}/...
        if (segments.length >= 3 && "sections".equals(segments[1])) {
            return segments[2];
        }
        return "unknown";
    }

    /** Returns the path segments after the section key, i.e. the field within that section. */
    public String fieldPath() {
        String[] segments = path.split("/");
        if (segments.length > 3) {
            return String.join("/", java.util.Arrays.copyOfRange(segments, 3, segments.length));
        }
        return path;
    }
}
