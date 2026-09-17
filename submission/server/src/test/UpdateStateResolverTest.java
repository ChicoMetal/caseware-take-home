package test;

import domain.model.*;
import domain.port.DiffSummaryTransformer;
import domain.port.TemplateDiffProvider;
import domain.port.TemplateVersionProvider;
import domain.service.UpdateStateResolver;

import java.time.Instant;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * Focused tests for UpdateStateResolver.
 * Ports are stubbed to isolate domain logic.
 *
 * In a real project these would use JUnit 5 + Mockito.
 * Here we use plain assertions to avoid framework dependencies.
 */
public class UpdateStateResolverTest {

    private static final Instant NOW = Instant.parse("2026-08-18T13:00:00Z");

    public static void main(String[] args) {
        testEngagementUpToDate();
        testEngagementOneVersionBehind();
        testEngagementTwoVersionsBehind();

        System.out.println("All UpdateStateResolver tests passed.");
    }

    /**
     * Engagement at v5, latest is v5 → UP_TO_DATE, no diffs invoked.
     */
    static void testEngagementUpToDate() {
        var engagement = new EngagementRecord("ENG-1001", "Northstar Manufacturing 2026", "AUDIT-CA", 5);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        // Diff provider should never be called for up-to-date engagements
        TemplateDiffProvider diffProvider = (templateId, from, to) -> {
            throw new AssertionError("Diff provider should not be called for up-to-date engagement");
        };
        DiffSummaryTransformer transformer = (diff) -> {
            throw new AssertionError("Transformer should not be called for up-to-date engagement");
        };
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, emptyList());

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);
        EngagementUpdateSummary result = resolver.resolveUpdateState(engagement);

        assertEqual(UpdateStatus.UP_TO_DATE, result.status(), "status");
        assertEqual(0, result.pendingUpdateCount(), "pendingUpdateCount");
        assertEqual(5, result.currentVersion(), "currentVersion");
        assertEqual(5, result.latestVersion(), "latestVersion");
        assertEqual(false, result.summaryAvailable(), "summaryAvailable");

        System.out.println("  ✓ testEngagementUpToDate");
    }

    /**
     * Engagement at v4, latest is v5 → PENDING with 1 update.
     * resolveUpdateDetails produces 1 collapsed summary and 1 step-by-step entry.
     */
    static void testEngagementOneVersionBehind() {
        var engagement = new EngagementRecord("ENG-1002", "Maple Ridge Foods 2026", "AUDIT-CA", 4);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        var collapsedDiff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/materiality/guidance/thresholdPercent", null, 4.5, 4.0)
        ));
        var stepDiff = collapsedDiff; // same for single-step case

        TemplateDiffProvider diffProvider = (templateId, from, to) -> {
            if (from == 4 && to == 5) return collapsedDiff;
            throw new AssertionError("Unexpected diff request: " + from + " → " + to);
        };

        var stubSummary = new ChangeSummary(4, 5, NOW, List.of(), 1);
        DiffSummaryTransformer transformer = (diff) -> stubSummary;
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, List.of(v5));

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);

        // Test state resolution
        EngagementUpdateSummary state = resolver.resolveUpdateState(engagement);
        assertEqual(UpdateStatus.PENDING, state.status(), "status");
        assertEqual(1, state.pendingUpdateCount(), "pendingUpdateCount");

        // Test detail resolution
        EngagementUpdateDetails details = resolver.resolveUpdateDetails(engagement);
        assertEqual(4, details.currentVersion(), "currentVersion");
        assertEqual(5, details.latestVersion(), "latestVersion");
        assertNotNull(details.collapsedSummary(), "collapsedSummary");
        assertEqual(1, details.stepByStepSummaries().size(), "stepByStep size");
        assertNotNull(details.freshness(), "freshness");
        assertNotNull(details.freshness().computedAt(), "freshness.computedAt");
        assertEqual(NOW, details.freshness().templatePublishedAt(), "freshness.templatePublishedAt");

        System.out.println("  ✓ testEngagementOneVersionBehind");
    }

    /**
     * Engagement at v3, latest is v5 → PENDING with 2 updates.
     * resolveUpdateDetails produces 1 collapsed summary (v3→v5) and 2 step-by-step entries.
     */
    static void testEngagementTwoVersionsBehind() {
        var engagement = new EngagementRecord("ENG-1003", "Harbourview Logistics 2026", "AUDIT-CA", 3);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var v4 = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 4, Instant.parse("2026-07-07T13:00:00Z"));
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        TemplateDiffProvider diffProvider = (templateId, from, to) -> new TemplateDiff(
            templateId, from, to, NOW, List.of(
                new DiffOperation("add", "/sections/planning/questions/7", null, null, null)
            )
        );

        var stubSummary = new ChangeSummary(0, 0, NOW, List.of(), 1);
        DiffSummaryTransformer transformer = (diff) -> new ChangeSummary(
            diff.fromVersion(), diff.toVersion(), diff.generatedAt(), List.of(), diff.changes().size()
        );
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, List.of(v4, v5));

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);

        // Test state resolution
        EngagementUpdateSummary state = resolver.resolveUpdateState(engagement);
        assertEqual(UpdateStatus.PENDING, state.status(), "status");
        assertEqual(2, state.pendingUpdateCount(), "pendingUpdateCount");

        // Test detail resolution
        EngagementUpdateDetails details = resolver.resolveUpdateDetails(engagement);
        assertEqual(3, details.currentVersion(), "currentVersion");
        assertEqual(5, details.latestVersion(), "latestVersion");

        // Collapsed: v3→v5
        assertEqual(3, details.collapsedSummary().fromVersion(), "collapsed fromVersion");
        assertEqual(5, details.collapsedSummary().toVersion(), "collapsed toVersion");

        // Step-by-step: v3→v4, v4→v5
        assertEqual(2, details.stepByStepSummaries().size(), "stepByStep size");
        assertEqual(3, details.stepByStepSummaries().get(0).fromVersion(), "step[0] fromVersion");
        assertEqual(4, details.stepByStepSummaries().get(0).toVersion(), "step[0] toVersion");
        assertEqual(4, details.stepByStepSummaries().get(1).fromVersion(), "step[1] fromVersion");
        assertEqual(5, details.stepByStepSummaries().get(1).toVersion(), "step[1] toVersion");
        assertNotNull(details.freshness(), "freshness");

        System.out.println("  ✓ testEngagementTwoVersionsBehind");
    }

    // --- Helpers ---

    private static TemplateVersionProvider stubTemplateProvider(
            TemplateVersion latest, List<TemplateVersion> intermediates) {
        return new TemplateVersionProvider() {
            @Override
            public TemplateVersion getLatestVersion(String templateId) {
                return latest;
            }

            @Override
            public List<TemplateVersion> getVersionsBetween(String templateId, int fromExclusive, int toInclusive) {
                return intermediates;
            }
        };
    }

    private static void assertEqual(Object expected, Object actual, String field) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                "Expected %s = %s, got %s".formatted(field, expected, actual)
            );
        }
    }

    private static void assertNotNull(Object value, String field) {
        if (value == null) {
            throw new AssertionError("Expected %s to be non-null".formatted(field));
        }
    }
}
