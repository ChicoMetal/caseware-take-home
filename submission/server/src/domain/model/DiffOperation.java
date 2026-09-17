package domain.model;

public record DiffOperation(
    String op,
    String path,
    Object value,
    Object oldValue,
    Object newValue
) {

    public String sectionKey() {
        String[] segments = path.split("/");
        // path format: /sections/{sectionKey}/...
        if (segments.length >= 3 && "sections".equals(segments[1])) {
            return segments[2];
        }
        return "unknown";
    }

    public String fieldPath() {
        String[] segments = path.split("/");
        if (segments.length > 3) {
            return String.join("/", java.util.Arrays.copyOfRange(segments, 3, segments.length));
        }
        return path;
    }
}
