package test;

import adapter.RuleBasedDiffSummaryTransformer;
import domain.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Focused tests for RuleBasedDiffSummaryTransformer.
 * No mocks needed — this is a pure function.
 * Test data mirrors the provided fixture: template-diff-audit-ca-v3-v4.json
 */
public class RuleBasedDiffSummaryTransformerTest {

    private static final Instant NOW = Instant.parse("2026-07-07T13:02:18Z");

    public static void main(String[] args) {
        testTransformsAllThreeOperationTypes();
        testGroupsChangesBySection();
        testAddWithoutLabel();
        testReplaceWithShortStrings();
        testReplaceWithLongStrings();
        testRemoveWithoutLabel();
        testUnknownSectionFallsBackToCapitalized();
        testEmptyDiffProducesEmptySummary();
        testAddOptionalQuestionIsMediumImpact();
        testLabelReplaceIsLowImpact();

        System.out.println("All RuleBasedDiffSummaryTransformer tests passed.");
    }

    /**
     * Given a diff with add, replace, and remove operations,
     * verify each produces the correct ChangeType and a meaningful description.
     * Data from template-diff-audit-ca-v3-v4.json.
     */
    static void testTransformsAllThreeOperationTypes() {
        var diff = new TemplateDiff("AUDIT-CA", 3, 4, NOW, List.of(
            // ADD: new required question
            new DiffOperation("add", "/sections/planning/questions/7",
                Map.of(
                    "id", "Q-PLN-007",
                    "type", "yesNo",
                    "label", "Were any new fraud risk factors identified during planning?",
                    "required", true
                ),
                null, null),
            // REPLACE: numeric threshold change
            new DiffOperation("replace", "/sections/materiality/guidance/thresholdPercent",
                null, 5.0, 4.5),
            // REMOVE: deprecated procedure
            new DiffOperation("remove", "/sections/planning/procedures/legacy-risk-confirmation",
                null,
                Map.of(
                    "id", "PROC-PLN-004",
                    "label", "Confirm legacy risk classification",
                    "required", false
                ),
                null)
        ));

        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        assertEqual(3, summary.fromVersion(), "fromVersion");
        assertEqual(4, summary.toVersion(), "toVersion");
        assertEqual(3, summary.totalChanges(), "totalChanges");

        // Flatten all changes across sections
        List<HumanReadableChange> allChanges = summary.sections().stream()
            .flatMap(s -> s.changes().stream())
            .toList();

        // Verify ADD
        HumanReadableChange addChange = allChanges.stream()
            .filter(c -> c.type() == ChangeType.ADDED).findFirst().orElseThrow();
        assert addChange.description().contains("fraud risk") : "ADD description should mention fraud risk";
        assert addChange.description().contains("required") : "ADD description should mention required";
        assertEqual(Impact.HIGH, addChange.impact(), "ADD impact (required field)");

        // Verify REPLACE
        HumanReadableChange replaceChange = allChanges.stream()
            .filter(c -> c.type() == ChangeType.MODIFIED).findFirst().orElseThrow();
        assert replaceChange.description().contains("5.0") : "REPLACE should mention old value";
        assert replaceChange.description().contains("4.5") : "REPLACE should mention new value";
        assertEqual(Impact.HIGH, replaceChange.impact(), "REPLACE impact (threshold)");

        // Verify REMOVE
        HumanReadableChange removeChange = allChanges.stream()
            .filter(c -> c.type() == ChangeType.REMOVED).findFirst().orElseThrow();
        assert removeChange.description().contains("Confirm legacy risk") : "REMOVE should mention the label";
        assertEqual(Impact.MEDIUM, removeChange.impact(), "REMOVE impact");

        System.out.println("  ✓ testTransformsAllThreeOperationTypes");
    }

    /**
     * Given a diff with changes in two different sections (planning + materiality),
     * verify they are grouped into separate SectionChange objects.
     */
    static void testGroupsChangesBySection() {
        var diff = new TemplateDiff("AUDIT-CA", 3, 4, NOW, List.of(
            new DiffOperation("add", "/sections/planning/questions/7",
                Map.of("id", "Q-PLN-007", "label", "New question", "required", false),
                null, null),
            new DiffOperation("replace", "/sections/materiality/guidance/thresholdPercent",
                null, 5.0, 4.5),
            new DiffOperation("remove", "/sections/planning/procedures/legacy-risk-confirmation",
                null,
                Map.of("id", "PROC-PLN-004", "label", "Old procedure", "required", false),
                null)
        ));

        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        assertEqual(2, summary.sections().size(), "section count");

        SectionChange planningSection = summary.sections().stream()
            .filter(s -> "planning".equals(s.sectionPath())).findFirst().orElseThrow();
        assertEqual("Planning", planningSection.sectionDisplayName(), "planning display name");
        assertEqual(2, planningSection.changes().size(), "planning changes count");

        SectionChange materialitySection = summary.sections().stream()
            .filter(s -> "materiality".equals(s.sectionPath())).findFirst().orElseThrow();
        assertEqual("Materiality", materialitySection.sectionDisplayName(), "materiality display name");
        assertEqual(1, materialitySection.changes().size(), "materiality changes count");

        System.out.println("  ✓ testGroupsChangesBySection");
    }

    /**
     * ADD a simple field (no label property) → "Added [readable field name]", MEDIUM impact.
     */
    static void testAddWithoutLabel() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("add", "/sections/planning/guidance/newFieldName",
                "some value", null, null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEqual(ChangeType.ADDED, change.type(), "type");
        assert change.description().contains("Added") : "Should say 'Added': " + change.description();
        assert change.description().contains("New field name") : "Should contain readable field name: " + change.description();
        assertEqual(Impact.MEDIUM, change.impact(), "impact for optional add");

        System.out.println("  ✓ testAddWithoutLabel");
    }

    /**
     * REPLACE with short strings → shows both old and new values in quotes.
     */
    static void testReplaceWithShortStrings() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/planning/questions/3/label",
                null, "Old question text", "New question text")
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEqual(ChangeType.MODIFIED, change.type(), "type");
        assert change.description().contains("Old question text") : "Should contain old value: " + change.description();
        assert change.description().contains("New question text") : "Should contain new value: " + change.description();
        assert change.description().contains("'") : "Short strings should be quoted: " + change.description();

        System.out.println("  ✓ testReplaceWithShortStrings");
    }

    /**
     * REPLACE with long strings (>80 chars) → falls back to "[field] text updated".
     */
    static void testReplaceWithLongStrings() {
        String longOld = "A".repeat(100);
        String longNew = "B".repeat(100);
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/planning/questions/3/label",
                null, longOld, longNew)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEqual(ChangeType.MODIFIED, change.type(), "type");
        assert change.description().contains("text updated") : "Long strings should fallback: " + change.description();
        assert !change.description().contains(longOld) : "Should NOT contain the full old string";

        System.out.println("  ✓ testReplaceWithLongStrings");
    }

    /**
     * REMOVE a simple field (no label) → "Removed [readable field name]".
     */
    static void testRemoveWithoutLabel() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("remove", "/sections/completion/guidance/obsoleteFlag",
                null, "true", null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEqual(ChangeType.REMOVED, change.type(), "type");
        assert change.description().contains("Removed") : "Should say 'Removed': " + change.description();
        assert change.description().contains("Obsolete flag") : "Should contain readable name: " + change.description();
        assertEqual(Impact.MEDIUM, change.impact(), "impact for remove");

        System.out.println("  ✓ testRemoveWithoutLabel");
    }

    /**
     * Unknown section key → display name is capitalized fallback.
     */
    static void testUnknownSectionFallsBackToCapitalized() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("add", "/sections/customNewSection/items/1",
                Map.of("label", "Test item", "required", false),
                null, null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        assertEqual(1, summary.sections().size(), "section count");
        assertEqual("customNewSection", summary.sections().get(0).sectionPath(), "sectionPath");
        assertEqual("CustomNewSection", summary.sections().get(0).sectionDisplayName(), "display name fallback");

        System.out.println("  ✓ testUnknownSectionFallsBackToCapitalized");
    }

    /**
     * Empty diff → zero sections, zero total changes.
     */
    static void testEmptyDiffProducesEmptySummary() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of());
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        assertEqual(0, summary.sections().size(), "sections");
        assertEqual(0, summary.totalChanges(), "totalChanges");
        assertEqual(4, summary.fromVersion(), "fromVersion");
        assertEqual(5, summary.toVersion(), "toVersion");

        System.out.println("  ✓ testEmptyDiffProducesEmptySummary");
    }

    /**
     * ADD optional question → MEDIUM impact (not HIGH, because required=false).
     */
    static void testAddOptionalQuestionIsMediumImpact() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("add", "/sections/planning/questions/9",
                Map.of("id", "Q-PLN-009", "label", "Optional question?", "required", false),
                null, null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEqual(Impact.MEDIUM, change.impact(), "optional add should be MEDIUM");
        assert !change.description().contains("required") : "Should NOT mention required: " + change.description();

        System.out.println("  ✓ testAddOptionalQuestionIsMediumImpact");
    }

    /**
     * REPLACE on a label path → LOW impact.
     */
    static void testLabelReplaceIsLowImpact() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/planning/questions/3/label",
                null, "Old text", "New text")
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEqual(Impact.LOW, change.impact(), "label replace should be LOW");

        System.out.println("  ✓ testLabelReplaceIsLowImpact");
    }

    // --- Helpers ---

    private static void assertEqual(Object expected, Object actual, String field) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "Expected %s = %s, got %s".formatted(field, expected, actual)
            );
        }
    }
}
