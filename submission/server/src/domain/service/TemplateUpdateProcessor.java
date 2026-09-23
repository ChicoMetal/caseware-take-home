package domain.service;

import domain.model.*;
import domain.port.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Domain service that processes template publication events.
 *
 * <p>When a new template version is published, this service:
 * <ol>
 *   <li>Identifies all engagements using that template with a lagging version.</li>
 *   <li>Computes diffs for each distinct version gap (collapsed and step-by-step).</li>
 *   <li>Transforms diffs into human-readable summaries and stores them.</li>
 *   <li>Updates each affected engagement's materialized state in the index.</li>
 * </ol>
 *
 * <p>This is the "write side" of the CQRS pattern — it pre-computes summaries
 * and materializes engagement states so the read side is a simple lookup.
 *
 * <p><b>Security boundary:</b> This service operates across all firms (cross-tenant)
 * by design. It must only be invoked by infrastructure-authenticated callers
 * (event handlers, scheduled jobs) — never exposed as a client-facing API.
 * Authentication is enforced at the infrastructure layer (e.g., mTLS on the
 * internal event bus, service account credentials for the scheduler).
 */
public class TemplateUpdateProcessor {

    private final EngagementIndexRepository indexRepository;
    private final TemplateLookupRepository templateLookup;
    private final TemplateDiffProvider diffProvider;
    private final DiffSummaryTransformer summaryTransformer;
    private final TemplateVersionProvider templateProvider;
    private final UpdateSummaryRepository summaryRepository;

    public TemplateUpdateProcessor(EngagementIndexRepository indexRepository,
                                   TemplateLookupRepository templateLookup,
                                   TemplateDiffProvider diffProvider,
                                   DiffSummaryTransformer summaryTransformer,
                                   TemplateVersionProvider templateProvider,
                                   UpdateSummaryRepository summaryRepository) {
        this.indexRepository = Objects.requireNonNull(indexRepository, "indexRepository");
        this.templateLookup = Objects.requireNonNull(templateLookup, "templateLookup");
        this.diffProvider = Objects.requireNonNull(diffProvider, "diffProvider");
        this.summaryTransformer = Objects.requireNonNull(summaryTransformer, "summaryTransformer");
        this.templateProvider = Objects.requireNonNull(templateProvider, "templateProvider");
        this.summaryRepository = Objects.requireNonNull(summaryRepository, "summaryRepository");
    }

    /**
     * Processes a template publication event.
     * Computes and stores summaries for all distinct version gaps, then updates
     * each affected engagement's state in the index.
     *
     * @param templateId       the published template
     * @param publishedVersion the newly published version number
     * @return the number of engagements whose state was updated
     */
    public int processTemplatePublished(String templateId, int publishedVersion) {
        List<EngagementRecord> affected = templateLookup.findByTemplateWithVersionBelow(
            templateId, publishedVersion
        );

        List<Integer> distinctVersions = affected.stream()
            .map(EngagementRecord::templateVersion)
            .distinct()
            .sorted()
            .toList();

        for (int lagVersion : distinctVersions) {
            computeAndStoreSummaries(templateId, lagVersion, publishedVersion);
        }

        for (EngagementRecord engagement : affected) {
            UpdateStatus newStatus = resolveStatusAfterPublish(engagement, publishedVersion);
            boolean summaryAvailable = summaryRepository
                .findByVersionRange(templateId, engagement.templateVersion(), publishedVersion)
                .isPresent();

            Integer clearedDecline = (newStatus == UpdateStatus.PENDING) ? null : engagement.declinedVersion();

            indexRepository.updateState(
                engagement.firmId(),
                engagement.engagementId(),
                engagement.templateVersion(),
                newStatus,
                publishedVersion,
                clearedDecline,
                summaryAvailable
            );
        }

        return affected.size();
    }

    /**
     * Reconciliation entry point. Scans the full index for a template,
     * identifies engagements with missing summaries, and computes them.
     * Intended to be called by a periodic job as a safety net for missed events.
     *
     * @param templateId the template to reconcile
     * @return the number of version gaps reconciled
     */
    public int reconcile(String templateId) {
        TemplateVersion latest = templateProvider.getLatestVersion(templateId);

        List<EngagementRecord> lagging = templateLookup.findByTemplateWithVersionBelow(
            templateId, latest.version()
        );

        List<Integer> distinctVersions = lagging.stream()
            .map(EngagementRecord::templateVersion)
            .distinct()
            .sorted()
            .toList();

        int reconciled = 0;

        for (int lagVersion : distinctVersions) {
            if (summaryRepository.findByVersionRange(templateId, lagVersion, latest.version()).isEmpty()) {
                computeAndStoreSummaries(templateId, lagVersion, latest.version());
                reconciled++;
            }
        }

        // Update state for any engagement still marked as COMPUTING
        for (EngagementRecord engagement : lagging) {
            if (engagement.status() == UpdateStatus.COMPUTING) {
                boolean summaryAvailable = summaryRepository
                    .findByVersionRange(templateId, engagement.templateVersion(), latest.version())
                    .isPresent();
                if (summaryAvailable) {
                    UpdateStatus newStatus = resolveStatusAfterPublish(engagement, latest.version());
                    indexRepository.updateState(
                        engagement.firmId(),
                        engagement.engagementId(),
                        engagement.templateVersion(),
                        newStatus,
                        latest.version(),
                        engagement.declinedVersion(),
                        true
                    );
                }
            }
        }

        return reconciled;
    }

    private UpdateStatus resolveStatusAfterPublish(EngagementRecord engagement, int publishedVersion) {
        if (engagement.declinedVersion() != null && engagement.declinedVersion() >= publishedVersion) {
            return UpdateStatus.DECLINED;
        }
        return UpdateStatus.PENDING;
    }

    private void computeAndStoreSummaries(String templateId, int fromVersion, int toVersion) {
        List<TemplateVersion> intermediates = templateProvider.getVersionsBetween(
            templateId, fromVersion, toVersion
        );

        // Collapsed summary: direct diff from current → latest
        TemplateDiff collapsedDiff = diffProvider.computeDiff(templateId, fromVersion, toVersion);
        ChangeSummary rawCollapsed = summaryTransformer.transform(collapsedDiff);
        Instant collapsedPublishedAt = intermediates.isEmpty()
            ? rawCollapsed.publishedAt()
            : intermediates.get(intermediates.size() - 1).publishedAt();
        summaryRepository.save(templateId, withPublishedAt(rawCollapsed, collapsedPublishedAt));

        // Step-by-step summaries: consecutive diffs for each intermediate version
        int previousVersion = fromVersion;
        for (TemplateVersion version : intermediates) {
            if (summaryRepository.findByVersionRange(templateId, previousVersion, version.version()).isEmpty()) {
                TemplateDiff stepDiff = diffProvider.computeDiff(templateId, previousVersion, version.version());
                ChangeSummary rawStep = summaryTransformer.transform(stepDiff);
                summaryRepository.save(templateId, withPublishedAt(rawStep, version.publishedAt()));
            }
            previousVersion = version.version();
        }
    }

    private static ChangeSummary withPublishedAt(ChangeSummary summary, Instant publishedAt) {
        return new ChangeSummary(
            summary.fromVersion(), summary.toVersion(), publishedAt,
            summary.sections(), summary.totalChanges()
        );
    }
}
