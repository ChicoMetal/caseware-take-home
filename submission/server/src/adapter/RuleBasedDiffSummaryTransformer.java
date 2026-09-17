package adapter;

import domain.model.*;
import domain.port.DiffSummaryTransformer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic, rule-based implementation of {@link DiffSummaryTransformer}.
 *
 * <p>This is the default strategy. The port abstraction allows swapping in an LLM-based
 * transformer for richer natural-language descriptions without changing domain logic.
 *
 * <p><b>Description generation</b>: branches on operation type (add/replace/remove),
 * extracting labels and element types from the diff payload when available.
 *
 * <p><b>Impact assessment rules</b>:
 * <ul>
 *   <li>HIGH - new required fields, or changes to threshold/scoring paths</li>
 *   <li>MEDIUM - removals, or additions of new checklists/questions</li>
 *   <li>LOW - label/text replacements and all other modifications</li>
 * </ul>
 */
public class RuleBasedDiffSummaryTransformer implements DiffSummaryTransformer {

    private static final Map<String, String> SECTION_DISPLAY_NAMES = Map.of(
        "planning", "Planning",
        "materiality", "Materiality",
        "completion", "Completion",
        "inquiries", "Inquiries",
        "analytics", "Analytics",
        "riskAssessment", "Risk Assessment",
        "documentation", "Documentation",
        "monitoring", "Monitoring"
    );

    @Override
    public ChangeSummary transform(TemplateDiff diff) {
        Map<String, List<HumanReadableChange>> changesBySection = new LinkedHashMap<>();

        for (DiffOperation op : diff.changes()) {
            String sectionKey = op.sectionKey();
            HumanReadableChange change = transformOperation(op);
            changesBySection
                .computeIfAbsent(sectionKey, k -> new ArrayList<>())
                .add(change);
        }

        List<SectionChange> sections = changesBySection.entrySet().stream()
            .map(entry -> new SectionChange(
                entry.getKey(),
                resolveSectionName(entry.getKey()),
                entry.getValue()
            ))
            .collect(Collectors.toList());

        int totalChanges = diff.changes().size();

        return new ChangeSummary(
            diff.fromVersion(),
            diff.toVersion(),
            diff.generatedAt(),
            sections,
            totalChanges
        );
    }

    /** Converts a single diff operation into a human-readable change with assessed impact. */
    private HumanReadableChange transformOperation(DiffOperation op) {
        ChangeType type = ChangeType.fromDiffOp(op.op());
        String description = generateDescription(op);
        Impact impact = assessImpact(op);

        return new HumanReadableChange(type, description, impact);
    }

    /** Generates a description by dispatching to add/replace/remove-specific formatters. */
    private String generateDescription(DiffOperation op) {
        return switch (op.op()) {
            case "add" -> describeAdd(op);
            case "replace" -> describeReplace(op);
            case "remove" -> describeRemove(op);
            default -> "Unknown change at " + op.path();
        };
    }

    private String describeAdd(DiffOperation op) {
        Object value = op.value();
        if (hasLabel(value)) {
            String label = extractLabel(value);
            String requiredSuffix = isRequired(value) ? " (required)" : "";
            String elementType = inferElementType(op.fieldPath());
            return "New %s: '%s'%s".formatted(elementType, label, requiredSuffix);
        }
        String fieldName = readableFieldName(op.fieldPath());
        return "Added %s".formatted(fieldName);
    }

    private String describeReplace(DiffOperation op) {
        Object oldVal = op.oldValue();
        Object newVal = op.newValue();
        String fieldName = readableFieldName(op.fieldPath());

        if (oldVal instanceof Number && newVal instanceof Number) {
            return "%s changed from %s to %s".formatted(fieldName, oldVal, newVal);
        }
        if (oldVal instanceof String oldStr && newVal instanceof String newStr) {
            if (oldStr.length() <= 80 && newStr.length() <= 80) {
                return "%s updated from '%s' to '%s'".formatted(fieldName, oldStr, newStr);
            }
            return "%s text updated".formatted(fieldName);
        }
        return "%s updated".formatted(fieldName);
    }

    private String describeRemove(DiffOperation op) {
        Object oldValue = op.oldValue();
        if (hasLabel(oldValue)) {
            String label = extractLabel(oldValue);
            String elementType = inferElementType(op.fieldPath());
            return "Removed %s: '%s'".formatted(elementType, label);
        }
        String fieldName = readableFieldName(op.fieldPath());
        return "Removed %s".formatted(fieldName);
    }

    /**
     * Assesses business impact of a change using a priority-ordered rule chain.
     * Rules are evaluated top-down; the first match wins.
     */
    private Impact assessImpact(DiffOperation op) {
        // New required fields are high impact
        if ("add".equals(op.op()) && isRequired(op.value())) {
            return Impact.HIGH;
        }
        // Threshold/scoring changes are high impact
        String path = op.path().toLowerCase();
        if (path.contains("threshold") || path.contains("scoring")) {
            return Impact.HIGH;
        }
        // Removals are medium impact
        if ("remove".equals(op.op())) {
            return Impact.MEDIUM;
        }
        // New checklists or questions are medium impact
        if ("add".equals(op.op())) {
            return Impact.MEDIUM;
        }
        // Label/text changes are low impact
        if ("replace".equals(op.op()) && path.contains("label")) {
            return Impact.LOW;
        }
        return Impact.LOW;
    }

    private static boolean hasLabel(Object obj) {
        return obj instanceof Map<?, ?> map && map.containsKey("label");
    }

    private static String extractLabel(Object obj) {
        if (obj instanceof Map<?, ?> map && map.get("label") instanceof String label) {
            return label;
        }
        return null;
    }

    private static boolean isRequired(Object obj) {
        return obj instanceof Map<?, ?> map
            && Boolean.TRUE.equals(map.get("required"));
    }

    private String resolveSectionName(String sectionKey) {
        return SECTION_DISPLAY_NAMES.getOrDefault(sectionKey, capitalize(sectionKey));
    }

    private String inferElementType(String fieldPath) {
        if (fieldPath.contains("questions")) return "question";
        if (fieldPath.contains("checklists")) return "checklist";
        if (fieldPath.contains("procedures")) return "procedure";
        return "item";
    }

    private String readableFieldName(String fieldPath) {
        String[] parts = fieldPath.split("/");
        String lastPart = parts[parts.length - 1];
        // Convert camelCase to readable: "thresholdPercent" → "Threshold percent"
        String readable = lastPart.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase();
        return capitalize(readable);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
