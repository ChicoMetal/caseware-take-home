package domain.model;

import java.util.Map;

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

    public boolean hasLabel(Object obj) {
        return obj instanceof Map<?, ?> map && map.containsKey("label");
    }

    public String extractLabel(Object obj) {
        if (obj instanceof Map<?, ?> map && map.get("label") instanceof String label) {
            return label;
        }
        return null;
    }

    public boolean isRequired(Object obj) {
        return obj instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("required"));
    }
}
