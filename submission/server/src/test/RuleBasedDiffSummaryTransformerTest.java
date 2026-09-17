package test;

import adapter.RuleBasedDiffSummaryTransformer;
import domain.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Focused tests for RuleBasedDiffSummaryTransformer.
 * No mocks needed -- this is a pure function.
 * Test data mirrors the provided fixture: template-diff-audit-ca-v3-v4.json
 */
public class RuleBasedDiffSummaryTransformerTest {

    private static final Instant NOW = Instant.parse("2026-07-07T13:02:18Z");

    /**
     * Given a diff with add, replace, and remove operations,
     * verify each produces the correct ChangeType and a meaningful description.
     * Data from template-diff-audit-ca-v3-v4.json.
     */
    @Test
    void testTransformsAllThreeOperationTypes() {
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

        assertEquals(3, summary.fromVersion(), "fromVersion");
        assertEquals(4, summary.toVersion(), "toVersion");
        assertEquals(3, summary.totalChanges(), "totalChanges");

        // Flatten all changes across sections
        List<HumanReadableChange> allChanges = summary.sections().stream()
            .flatMap(s -> s.changes().stream())
            .toList();

        // Verify ADD
        HumanReadableChange addChange = allChanges.stream()
            .filter(c -> c.type() == ChangeType.ADDED).findFirst().orElseThrow();
        assertTrue(addChange.description().contains("fraud risk"), "ADD description should mention fraud risk");
        assertTrue(addChange.description().contains("required"), "ADD description should mention required");
        assertEquals(Impact.HIGH, addChange.impact(), "ADD impact (required field)");

        // Verify REPLACE
        HumanReadableChange replaceChange = allChanges.stream()
            .filter(c -> c.type() == ChangeType.MODIFIED).findFirst().orElseThrow();
        assertTrue(replaceChange.description().contains("5.0"), "REPLACE should mention old value");
        assertTrue(replaceChange.description().contains("4.5"), "REPLACE should mention new value");
        assertEquals(Impact.HIGH, replaceChange.impact(), "REPLACE impact (threshold)");

        // Verify REMOVE
        HumanReadableChange removeChange = allChanges.stream()
            .filter(c -> c.type() == ChangeType.REMOVED).findFirst().orElseThrow();
        assertTrue(removeChange.description().contains("Confirm legacy risk"), "REMOVE should mention the label");
        assertEquals(Impact.MEDIUM, removeChange.impact(), "REMOVE impact");
    }

    /**
     * Given a diff with changes in two different sections (planning + materiality),
     * verify they are grouped into separate SectionChange objects.
     */
    @Test
    void testGroupsChangesBySection() {
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

        assertEquals(2, summary.sections().size(), "section count");

        SectionChange planningSection = summary.sections().stream()
            .filter(s -> "planning".equals(s.sectionPath())).findFirst().orElseThrow();
        assertEquals("Planning", planningSection.sectionDisplayName(), "planning display name");
        assertEquals(2, planningSection.changes().size(), "planning changes count");

        SectionChange materialitySection = summary.sections().stream()
            .filter(s -> "materiality".equals(s.sectionPath())).findFirst().orElseThrow();
        assertEquals("Materiality", materialitySection.sectionDisplayName(), "materiality display name");
        assertEquals(1, materialitySection.changes().size(), "materiality changes count");
    }

    /**
     * ADD a simple field (no label property) -> "Added [readable field name]", MEDIUM impact.
     */
    @Test
    void testAddWithoutLabel() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("add", "/sections/planning/guidance/newFieldName",
                "some value", null, null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEquals(ChangeType.ADDED, change.type(), "type");
        assertTrue(change.description().contains("Added"), "Should say 'Added': " + change.description());
        assertTrue(change.description().contains("New field name"), "Should contain readable field name: " + change.description());
        assertEquals(Impact.MEDIUM, change.impact(), "impact for optional add");
    }

    /**
     * REPLACE with short strings -> shows both old and new values in quotes.
     */
    @Test
    void testReplaceWithShortStrings() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/planning/questions/3/label",
                null, "Old question text", "New question text")
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEquals(ChangeType.MODIFIED, change.type(), "type");
        assertTrue(change.description().contains("Old question text"), "Should contain old value: " + change.description());
        assertTrue(change.description().contains("New question text"), "Should contain new value: " + change.description());
        assertTrue(change.description().contains("'"), "Short strings should be quoted: " + change.description());
    }

    /**
     * REPLACE with long strings (>80 chars) -> falls back to "[field] text updated".
     */
    @Test
    void testReplaceWithLongStrings() {
        String longOld = "A".repeat(100);
        String longNew = "B".repeat(100);
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/planning/questions/3/label",
                null, longOld, longNew)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEquals(ChangeType.MODIFIED, change.type(), "type");
        assertTrue(change.description().contains("text updated"), "Long strings should fallback: " + change.description());
        assertFalse(change.description().contains(longOld), "Should NOT contain the full old string");
    }

    /**
     * REMOVE a simple field (no label) -> "Removed [readable field name]".
     */
    @Test
    void testRemoveWithoutLabel() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("remove", "/sections/completion/guidance/obsoleteFlag",
                null, "true", null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEquals(ChangeType.REMOVED, change.type(), "type");
        assertTrue(change.description().contains("Removed"), "Should say 'Removed': " + change.description());
        assertTrue(change.description().contains("Obsolete flag"), "Should contain readable name: " + change.description());
        assertEquals(Impact.MEDIUM, change.impact(), "impact for remove");
    }

    /**
     * Unknown section key -> display name is capitalized fallback.
     */
    @Test
    void testUnknownSectionFallsBackToCapitalized() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("add", "/sections/customNewSection/items/1",
                Map.of("label", "Test item", "required", false),
                null, null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        assertEquals(1, summary.sections().size(), "section count");
        assertEquals("customNewSection", summary.sections().get(0).sectionPath(), "sectionPath");
        assertEquals("CustomNewSection", summary.sections().get(0).sectionDisplayName(), "display name fallback");
    }

    /**
     * Empty diff -> zero sections, zero total changes.
     */
    @Test
    void testEmptyDiffProducesEmptySummary() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of());
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        assertEquals(0, summary.sections().size(), "sections");
        assertEquals(0, summary.totalChanges(), "totalChanges");
        assertEquals(4, summary.fromVersion(), "fromVersion");
        assertEquals(5, summary.toVersion(), "toVersion");
    }

    /**
     * ADD optional question -> MEDIUM impact (not HIGH, because required=false).
     */
    @Test
    void testAddOptionalQuestionIsMediumImpact() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("add", "/sections/planning/questions/9",
                Map.of("id", "Q-PLN-009", "label", "Optional question?", "required", false),
                null, null)
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEquals(Impact.MEDIUM, change.impact(), "optional add should be MEDIUM");
        assertFalse(change.description().contains("required"), "Should NOT mention required: " + change.description());
    }

    /**
     * REPLACE on a label path -> LOW impact.
     */
    @Test
    void testLabelReplaceIsLowImpact() {
        var diff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/planning/questions/3/label",
                null, "Old text", "New text")
        ));
        var transformer = new RuleBasedDiffSummaryTransformer();
        ChangeSummary summary = transformer.transform(diff);

        var change = summary.sections().get(0).changes().get(0);
        assertEquals(Impact.LOW, change.impact(), "label replace should be LOW");
    }
}
