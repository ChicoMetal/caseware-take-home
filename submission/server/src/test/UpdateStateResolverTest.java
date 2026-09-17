package test;

import domain.model.*;
import domain.port.DiffSummaryTransformer;
import domain.port.TemplateDiffProvider;
import domain.port.TemplateVersionProvider;
import domain.service.UpdateStateResolver;

import java.time.Instant;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Focused tests for UpdateStateResolver.
 * Ports are stubbed to isolate domain logic.
 */
public class UpdateStateResolverTest {

    private static final Instant NOW = Instant.parse("2026-08-18T13:00:00Z");

    /**
     * Engagement at v5, latest is v5 -> UP_TO_DATE, no diffs invoked.
     */
    @Test
    void testEngagementUpToDate() {
        var engagement = new EngagementRecord("ENG-1001", "FIRM-001", "Northstar Manufacturing 2026", "AUDIT-CA", 5);
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
        EngagementUpdateSummary result = resolver.resolveUpdateState(engagement, null);

        assertEquals(UpdateStatus.UP_TO_DATE, result.status(), "status");
        assertEquals(0, result.pendingUpdateCount(), "pendingUpdateCount");
        assertEquals(5, result.currentVersion(), "currentVersion");
        assertEquals(5, result.latestVersion(), "latestVersion");
        assertEquals(false, result.summaryAvailable(), "summaryAvailable");
    }

    /**
     * Engagement at v4, latest is v5 -> PENDING with 1 update.
     * resolveUpdateDetails produces 1 collapsed summary and 1 step-by-step entry.
     */
    @Test
    void testEngagementOneVersionBehind() {
        var engagement = new EngagementRecord("ENG-1002", "FIRM-001", "Maple Ridge Foods 2026", "AUDIT-CA", 4);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        var collapsedDiff = new TemplateDiff("AUDIT-CA", 4, 5, NOW, List.of(
            new DiffOperation("replace", "/sections/materiality/guidance/thresholdPercent", null, 4.5, 4.0)
        ));
        TemplateDiffProvider diffProvider = (templateId, from, to) -> {
            if (from == 4 && to == 5) return collapsedDiff;
            throw new AssertionError("Unexpected diff request: " + from + " -> " + to);
        };

        var stubSummary = new ChangeSummary(4, 5, NOW, List.of(), 1);
        DiffSummaryTransformer transformer = (diff) -> stubSummary;
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, List.of(v5));

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);

        // Test state resolution
        EngagementUpdateSummary state = resolver.resolveUpdateState(engagement, null);
        assertEquals(UpdateStatus.PENDING, state.status(), "status");
        assertEquals(1, state.pendingUpdateCount(), "pendingUpdateCount");

        // Test detail resolution
        EngagementUpdateDetails details = resolver.resolveUpdateDetails(engagement);
        assertEquals(4, details.currentVersion(), "currentVersion");
        assertEquals(5, details.latestVersion(), "latestVersion");
        assertNotNull(details.collapsedSummary(), "collapsedSummary");
        assertEquals(1, details.stepByStepSummaries().size(), "stepByStep size");
        assertNotNull(details.freshness(), "freshness");
        assertNotNull(details.freshness().computedAt(), "freshness.computedAt");
        assertEquals(NOW, details.freshness().templatePublishedAt(), "freshness.templatePublishedAt");
    }

    /**
     * Engagement at v3, latest is v5 -> PENDING with 2 updates.
     * resolveUpdateDetails produces 1 collapsed summary (v3->v5) and 2 step-by-step entries.
     */
    @Test
    void testEngagementTwoVersionsBehind() {
        var engagement = new EngagementRecord("ENG-1003", "FIRM-001", "Harbourview Logistics 2026", "AUDIT-CA", 3);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var v4 = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 4, Instant.parse("2026-07-07T13:00:00Z"));
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        TemplateDiffProvider diffProvider = (templateId, from, to) -> new TemplateDiff(
            templateId, from, to, NOW, List.of(
                new DiffOperation("add", "/sections/planning/questions/7", null, null, null)
            )
        );

        DiffSummaryTransformer transformer = (diff) -> new ChangeSummary(
            diff.fromVersion(), diff.toVersion(), diff.generatedAt(), List.of(), diff.changes().size()
        );
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, List.of(v4, v5));

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);

        // Test state resolution
        EngagementUpdateSummary state = resolver.resolveUpdateState(engagement, null);
        assertEquals(UpdateStatus.PENDING, state.status(), "status");
        assertEquals(2, state.pendingUpdateCount(), "pendingUpdateCount");

        // Test detail resolution
        EngagementUpdateDetails details = resolver.resolveUpdateDetails(engagement);
        assertEquals(3, details.currentVersion(), "currentVersion");
        assertEquals(5, details.latestVersion(), "latestVersion");

        // Collapsed: v3->v5
        assertEquals(3, details.collapsedSummary().fromVersion(), "collapsed fromVersion");
        assertEquals(5, details.collapsedSummary().toVersion(), "collapsed toVersion");

        // Step-by-step: v3->v4, v4->v5
        assertEquals(2, details.stepByStepSummaries().size(), "stepByStep size");
        assertEquals(3, details.stepByStepSummaries().get(0).fromVersion(), "step[0] fromVersion");
        assertEquals(4, details.stepByStepSummaries().get(0).toVersion(), "step[0] toVersion");
        assertEquals(4, details.stepByStepSummaries().get(1).fromVersion(), "step[1] fromVersion");
        assertEquals(5, details.stepByStepSummaries().get(1).toVersion(), "step[1] toVersion");
        assertNotNull(details.freshness(), "freshness");
    }

    /**
     * Engagement at v4, latest v5, declined v5 -> DECLINED.
     */
    @Test
    void testDeclinedEngagementStaysDeclined() {
        var engagement = new EngagementRecord("ENG-2001", "FIRM-001", "Declined Corp 2026", "AUDIT-CA", 4);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        TemplateDiffProvider diffProvider = (templateId, from, to) -> {
            throw new AssertionError("Diff provider should not be called for state resolution");
        };
        DiffSummaryTransformer transformer = (diff) -> {
            throw new AssertionError("Transformer should not be called for state resolution");
        };
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, emptyList());

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);
        EngagementUpdateSummary result = resolver.resolveUpdateState(engagement, 5);

        assertEquals(UpdateStatus.DECLINED, result.status(), "status");
        assertEquals(1, result.pendingUpdateCount(), "pendingUpdateCount");
        assertEquals(true, result.summaryAvailable(), "summaryAvailable");
        assertEquals(5, result.declinedVersion(), "declinedVersion");
    }

    /**
     * Engagement at v4, declined v5, but new v6 published -> back to PENDING.
     */
    @Test
    void testDeclinedEngagementReturnsToPendingOnNewVersion() {
        var engagement = new EngagementRecord("ENG-2002", "FIRM-001", "Re-pending Corp 2026", "AUDIT-CA", 4);
        var latestVersion = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 6, NOW);

        TemplateDiffProvider diffProvider = (templateId, from, to) -> {
            throw new AssertionError("Diff provider should not be called for state resolution");
        };
        DiffSummaryTransformer transformer = (diff) -> {
            throw new AssertionError("Transformer should not be called for state resolution");
        };
        TemplateVersionProvider templateProvider = stubTemplateProvider(latestVersion, emptyList());

        var resolver = new UpdateStateResolver(diffProvider, transformer, templateProvider);
        EngagementUpdateSummary result = resolver.resolveUpdateState(engagement, 5);

        assertEquals(UpdateStatus.PENDING, result.status(), "status");
        assertEquals(2, result.pendingUpdateCount(), "pendingUpdateCount");
        assertEquals(5, result.declinedVersion(), "declinedVersion");
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
}
