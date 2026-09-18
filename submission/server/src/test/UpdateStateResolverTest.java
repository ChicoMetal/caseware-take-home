package test;

import domain.model.*;
import domain.port.EngagementIndexRepository;
import domain.port.TemplateVersionProvider;
import domain.port.UpdateSummaryRepository;
import domain.service.UpdateStateResolver;

import java.time.Instant;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Tests for UpdateStateResolver — the read-side query service.
 * Reads pre-materialized state from the index and pre-computed summaries from the store.
 */
public class UpdateStateResolverTest {

    private static final Instant NOW = Instant.parse("2026-08-18T13:00:00Z");

    // --- Listing tests ---

    @Test
    void testListEngagementsReturnsPreMaterializedState() {
        var eng1 = record("ENG-1", "FIRM-1", "Corp A", "AUDIT-CA", 5, 5, UpdateStatus.UP_TO_DATE, null, false);
        var eng2 = record("ENG-2", "FIRM-1", "Corp B", "AUDIT-CA", 4, 5, UpdateStatus.PENDING, null, true);
        var eng3 = record("ENG-3", "FIRM-1", "Corp C", "AUDIT-CA", 4, 5, UpdateStatus.DECLINED, 5, true);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        var resolver = new UpdateStateResolver(
            stubIndexRepository(List.of(eng1, eng2, eng3)),
            stubTemplateProvider(latest),
            emptySummaryRepository()
        );

        List<EngagementUpdateSummary> results = resolver.listEngagementUpdates("FIRM-1");

        assertEquals(3, results.size(), "count");

        assertEquals(UpdateStatus.UP_TO_DATE, results.get(0).status());
        assertEquals(0, results.get(0).pendingUpdateCount());
        assertFalse(results.get(0).summaryAvailable());

        assertEquals(UpdateStatus.PENDING, results.get(1).status());
        assertEquals(1, results.get(1).pendingUpdateCount());
        assertTrue(results.get(1).summaryAvailable());

        assertEquals(UpdateStatus.DECLINED, results.get(2).status());
        assertEquals(5, results.get(2).declinedVersion());
    }

    @Test
    void testListEngagementsShowsComputingState() {
        var eng = record("ENG-4", "FIRM-1", "Corp D", "AUDIT-CA", 4, 5, UpdateStatus.COMPUTING, null, false);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        var resolver = new UpdateStateResolver(
            stubIndexRepository(List.of(eng)),
            stubTemplateProvider(latest),
            emptySummaryRepository()
        );

        List<EngagementUpdateSummary> results = resolver.listEngagementUpdates("FIRM-1");
        assertEquals(UpdateStatus.COMPUTING, results.get(0).status());
        assertFalse(results.get(0).summaryAvailable());
    }

    // --- Detail tests ---

    @Test
    void testGetUpdateDetailsReadsFromStore() {
        var eng = record("ENG-5", "FIRM-1", "Corp E", "AUDIT-CA", 3, 5, UpdateStatus.PENDING, null, true);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var collapsed = new ChangeSummary(3, 5, NOW, List.of(), 2);
        var step1 = new ChangeSummary(3, 4, NOW, List.of(), 1);
        var step2 = new ChangeSummary(4, 5, NOW, List.of(), 1);

        var resolver = new UpdateStateResolver(
            stubIndexRepository(List.of(eng)),
            stubTemplateProvider(latest),
            stubSummaryRepository(
                Map.of("AUDIT-CA:3:5", collapsed),
                Map.of("AUDIT-CA:3:5", List.of(step1, step2))
            )
        );

        EngagementUpdateDetails details = resolver.getUpdateDetails("ENG-5");
        assertEquals(3, details.currentVersion());
        assertEquals(5, details.latestVersion());
        assertEquals(3, details.collapsedSummary().fromVersion());
        assertEquals(5, details.collapsedSummary().toVersion());
        assertEquals(2, details.stepByStepSummaries().size());
        assertNotNull(details.freshness());
    }

    @Test
    void testGetUpdateDetailsThrowsWhenSummaryMissing() {
        var eng = record("ENG-6", "FIRM-1", "Corp F", "AUDIT-CA", 4, 5, UpdateStatus.COMPUTING, null, false);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);

        var resolver = new UpdateStateResolver(
            stubIndexRepository(List.of(eng)),
            stubTemplateProvider(latest),
            emptySummaryRepository()
        );

        assertThrows(IllegalStateException.class, () -> resolver.getUpdateDetails("ENG-6"));
    }

    // --- Decision tests ---

    @Test
    void testProcessApplyDecision() {
        var eng = record("ENG-7", "FIRM-1", "Corp G", "AUDIT-CA", 4, 5, UpdateStatus.PENDING, null, true);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var stateUpdates = new ArrayList<String>();

        var resolver = new UpdateStateResolver(
            trackingIndexRepository(List.of(eng), stateUpdates),
            stubTemplateProvider(latest),
            emptySummaryRepository()
        );

        var decision = new UpdateDecision(DecisionType.APPLY, 5);
        UpdateDecisionResponse response = resolver.processDecision("ENG-7", decision);

        assertEquals(DecisionType.APPLY, response.decision());
        assertEquals(4, response.previousVersion());
        assertEquals(5, response.targetVersion());
        assertEquals(DecisionStatus.PROCESSING, response.status());
        assertTrue(stateUpdates.contains("ENG-7:UP_TO_DATE:5:null:false"), "state updated");
    }

    @Test
    void testProcessDeclineDecision() {
        var eng = record("ENG-8", "FIRM-1", "Corp H", "AUDIT-CA", 4, 5, UpdateStatus.PENDING, null, true);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 5, NOW);
        var stateUpdates = new ArrayList<String>();

        var resolver = new UpdateStateResolver(
            trackingIndexRepository(List.of(eng), stateUpdates),
            stubTemplateProvider(latest),
            emptySummaryRepository()
        );

        var decision = new UpdateDecision(DecisionType.DECLINE, 5);
        UpdateDecisionResponse response = resolver.processDecision("ENG-8", decision);

        assertEquals(DecisionType.DECLINE, response.decision());
        assertEquals(DecisionStatus.ACCEPTED, response.status());
        assertTrue(stateUpdates.contains("ENG-8:DECLINED:5:5:true"), "state updated with declinedVersion");
    }

    @Test
    void testProcessDecisionRejectsStaleTarget() {
        var eng = record("ENG-9", "FIRM-1", "Corp I", "AUDIT-CA", 4, 6, UpdateStatus.PENDING, null, true);
        var latest = new TemplateVersion("AUDIT-CA", "Canadian Audit Engagement", 6, NOW);

        var resolver = new UpdateStateResolver(
            stubIndexRepository(List.of(eng)),
            stubTemplateProvider(latest),
            emptySummaryRepository()
        );

        var staleDecision = new UpdateDecision(DecisionType.APPLY, 5);
        assertThrows(IllegalStateException.class, () -> resolver.processDecision("ENG-9", staleDecision));
    }

    // --- Helpers ---

    private static EngagementRecord record(String id, String firmId, String name, String templateId,
                                           int version, int latestVersion, UpdateStatus status,
                                           Integer declinedVersion, boolean summaryAvailable) {
        return new EngagementRecord(id, firmId, name, templateId, version, latestVersion,
            status, declinedVersion, summaryAvailable);
    }

    private static EngagementIndexRepository stubIndexRepository(List<EngagementRecord> records) {
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
                                    int latestVersion, Integer declinedVersion, boolean summaryAvailable) {}
        };
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

    private static TemplateVersionProvider stubTemplateProvider(TemplateVersion latest) {
        return new TemplateVersionProvider() {
            @Override
            public TemplateVersion getLatestVersion(String templateId) { return latest; }
            @Override
            public List<TemplateVersion> getVersionsBetween(String templateId, int from, int to) { return emptyList(); }
        };
    }

    private static UpdateSummaryRepository emptySummaryRepository() {
        return stubSummaryRepository(Map.of(), Map.of());
    }

    private static UpdateSummaryRepository stubSummaryRepository(
            Map<String, ChangeSummary> collapsed, Map<String, List<ChangeSummary>> steps) {
        return new UpdateSummaryRepository() {
            @Override
            public Optional<ChangeSummary> findByVersionRange(String templateId, int from, int to) {
                return Optional.ofNullable(collapsed.get(templateId + ":" + from + ":" + to));
            }
            @Override
            public List<ChangeSummary> findStepSummaries(String templateId, int from, int to) {
                return steps.getOrDefault(templateId + ":" + from + ":" + to, emptyList());
            }
            @Override
            public void save(String templateId, ChangeSummary summary) {
                throw new AssertionError("Resolver should never write summaries");
            }
        };
    }
}
