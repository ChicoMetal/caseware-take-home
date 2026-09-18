package test;

import domain.model.*;
import domain.port.*;
import domain.service.TemplateUpdateProcessor;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for TemplateUpdateProcessor — the "write side" that computes summaries
 * and materializes engagement state when a template is published.
 */
public class TemplateUpdateProcessorTest {

    private static final Instant NOW = Instant.parse("2026-08-18T13:00:00Z");
    private static final Instant EARLIER = Instant.parse("2026-07-07T13:00:00Z");

    @Test
    void testProcessTemplatePublishedComputesSummariesAndUpdatesState() {
        var engAtV3 = record("ENG-1", "FIRM-1", "Corp A", "AUDIT-CA", 3);
        var engAtV4 = record("ENG-2", "FIRM-1", "Corp B", "AUDIT-CA", 4);

        var v4 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 4, EARLIER);
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 5, NOW);

        var savedSummaries = new ArrayList<String>();
        var stateUpdates = new ArrayList<String>();

        var processor = new TemplateUpdateProcessor(
            trackingIndexRepository(List.of(engAtV3, engAtV4), stateUpdates),
            stubDiffProvider(),
            stubTransformer(),
            stubTemplateProvider(v5, List.of(v4, v5)),
            trackingSummaryRepository(savedSummaries)
        );

        int processed = processor.processTemplatePublished("AUDIT-CA", 5);

        assertEquals(2, processed, "engagements updated");
        assertTrue(savedSummaries.contains("AUDIT-CA:3:5"), "collapsed v3→v5");
        assertTrue(savedSummaries.contains("AUDIT-CA:3:4"), "step v3→v4");
        assertTrue(savedSummaries.contains("AUDIT-CA:4:5"), "step/collapsed v4→v5");

        assertTrue(stateUpdates.contains("ENG-1:PENDING:5:null:true"), "ENG-1 state updated to PENDING");
        assertTrue(stateUpdates.contains("ENG-2:PENDING:5:null:true"), "ENG-2 state updated to PENDING");
    }

    @Test
    void testProcessTemplatePublishedPreservesDeclinedStatus() {
        var declined = new EngagementRecord("ENG-3", "FIRM-1", "Corp C", "AUDIT-CA", 4, 5,
            UpdateStatus.DECLINED, 5, true);

        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 5, NOW);

        var savedSummaries = new ArrayList<String>();
        var stateUpdates = new ArrayList<String>();

        var processor = new TemplateUpdateProcessor(
            trackingIndexRepository(List.of(declined), stateUpdates),
            stubDiffProvider(),
            stubTransformer(),
            stubTemplateProvider(v5, List.of(v5)),
            trackingSummaryRepository(savedSummaries)
        );

        // Same version published again (re-process) — declined should stay DECLINED
        processor.processTemplatePublished("AUDIT-CA", 5);
        assertTrue(stateUpdates.contains("ENG-3:DECLINED:5:5:true"), "declined stays DECLINED");
    }

    @Test
    void testProcessTemplatePublishedSupersestsDeclineOnNewVersion() {
        var declined = new EngagementRecord("ENG-4", "FIRM-1", "Corp D", "AUDIT-CA", 4, 5,
            UpdateStatus.DECLINED, 5, true);

        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 5, EARLIER);
        var v6 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 6, NOW);

        var savedSummaries = new ArrayList<String>();
        var stateUpdates = new ArrayList<String>();

        var processor = new TemplateUpdateProcessor(
            trackingIndexRepository(List.of(declined), stateUpdates),
            stubDiffProvider(),
            stubTransformer(),
            stubTemplateProvider(v6, List.of(v5, v6)),
            trackingSummaryRepository(savedSummaries)
        );

        processor.processTemplatePublished("AUDIT-CA", 6);
        assertTrue(stateUpdates.contains("ENG-4:PENDING:6:null:true"), "decline superseded → PENDING, declinedVersion cleared");
    }

    @Test
    void testReconcileComputesMissingSummaries() {
        var engAtV4 = record("ENG-5", "FIRM-1", "Corp E", "AUDIT-CA", 4);
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 5, NOW);

        var savedSummaries = new ArrayList<String>();

        var processor = new TemplateUpdateProcessor(
            stubIndexRepository(List.of(engAtV4)),
            stubDiffProvider(),
            stubTransformer(),
            stubTemplateProvider(v5, List.of(v5)),
            trackingSummaryRepository(savedSummaries)
        );

        int reconciled = processor.reconcile("AUDIT-CA");

        assertEquals(1, reconciled, "version gaps reconciled");
        assertTrue(savedSummaries.contains("AUDIT-CA:4:5"), "collapsed v4→v5");
    }

    @Test
    void testReconcileSkipsExistingSummaries() {
        var engAtV4 = record("ENG-6", "FIRM-1", "Corp F", "AUDIT-CA", 4);
        var v5 = new TemplateVersion("AUDIT-CA", "Canadian Audit", 5, NOW);
        var existingSummary = new ChangeSummary(4, 5, NOW, List.of(), 1);

        var savedSummaries = new ArrayList<String>();

        var processor = new TemplateUpdateProcessor(
            stubIndexRepository(List.of(engAtV4)),
            stubDiffProvider(),
            stubTransformer(),
            stubTemplateProvider(v5, List.of(v5)),
            preloadedSummaryRepository(Map.of("AUDIT-CA:4:5", existingSummary), savedSummaries)
        );

        int reconciled = processor.reconcile("AUDIT-CA");
        assertEquals(0, reconciled, "nothing to reconcile");
        assertTrue(savedSummaries.isEmpty(), "no new summaries saved");
    }

    // --- Helpers ---

    private static EngagementRecord record(String id, String firmId, String name, String templateId, int version) {
        return new EngagementRecord(id, firmId, name, templateId, version, version,
            UpdateStatus.UP_TO_DATE, null, false);
    }

    private static EngagementIndexRepository stubIndexRepository(List<EngagementRecord> records) {
        return trackingIndexRepository(records, new ArrayList<>());
    }

    private static EngagementIndexRepository trackingIndexRepository(
            List<EngagementRecord> records, List<String> stateUpdates) {
        return new EngagementIndexRepository() {
            @Override
            public List<EngagementRecord> findByFirmId(String firmId) {
                return records.stream().filter(e -> e.firmId().equals(firmId)).toList();
            }
            @Override
            public Optional<EngagementRecord> findById(String engagementId) {
                return records.stream().filter(e -> e.engagementId().equals(engagementId)).findFirst();
            }
            @Override
            public List<EngagementRecord> findByTemplateWithVersionBelow(String templateId, int belowVersion) {
                return records.stream()
                    .filter(e -> e.templateId().equals(templateId) && e.templateVersion() < belowVersion).toList();
            }
            @Override
            public void updateState(String engagementId, int templateVersion, UpdateStatus status,
                                    int latestVersion, Integer declinedVersion, boolean summaryAvailable) {
                stateUpdates.add(engagementId + ":" + status + ":" + latestVersion + ":" + declinedVersion + ":" + summaryAvailable);
            }
        };
    }

    private static TemplateDiffProvider stubDiffProvider() {
        return (templateId, from, to) -> new TemplateDiff(
            templateId, from, to, NOW, List.of(
                new DiffOperation("add", "/sections/planning/questions/7", null, null, null)
            )
        );
    }

    private static DiffSummaryTransformer stubTransformer() {
        return diff -> new ChangeSummary(
            diff.fromVersion(), diff.toVersion(), diff.generatedAt(), List.of(), diff.changes().size()
        );
    }

    private static TemplateVersionProvider stubTemplateProvider(
            TemplateVersion latest, List<TemplateVersion> intermediates) {
        return new TemplateVersionProvider() {
            @Override
            public TemplateVersion getLatestVersion(String templateId) { return latest; }
            @Override
            public List<TemplateVersion> getVersionsBetween(String templateId, int fromExclusive, int toInclusive) {
                return intermediates.stream()
                    .filter(v -> v.version() > fromExclusive && v.version() <= toInclusive).toList();
            }
        };
    }

    private static UpdateSummaryRepository trackingSummaryRepository(List<String> saved) {
        var store = new HashMap<String, ChangeSummary>();
        return new UpdateSummaryRepository() {
            @Override
            public Optional<ChangeSummary> findByVersionRange(String templateId, int from, int to) {
                return Optional.ofNullable(store.get(templateId + ":" + from + ":" + to));
            }
            @Override
            public List<ChangeSummary> findStepSummaries(String templateId, int from, int to) { return List.of(); }
            @Override
            public void save(String templateId, ChangeSummary summary) {
                String key = templateId + ":" + summary.fromVersion() + ":" + summary.toVersion();
                store.put(key, summary);
                saved.add(key);
            }
        };
    }

    private static UpdateSummaryRepository preloadedSummaryRepository(
            Map<String, ChangeSummary> existing, List<String> saved) {
        var store = new HashMap<>(existing);
        return new UpdateSummaryRepository() {
            @Override
            public Optional<ChangeSummary> findByVersionRange(String templateId, int from, int to) {
                return Optional.ofNullable(store.get(templateId + ":" + from + ":" + to));
            }
            @Override
            public List<ChangeSummary> findStepSummaries(String templateId, int from, int to) { return List.of(); }
            @Override
            public void save(String templateId, ChangeSummary summary) {
                String key = templateId + ":" + summary.fromVersion() + ":" + summary.toVersion();
                store.put(key, summary);
                saved.add(key);
            }
        };
    }
}
