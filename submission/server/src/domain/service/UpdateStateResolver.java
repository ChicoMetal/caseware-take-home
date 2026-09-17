package domain.service;

import domain.model.*;
import domain.port.DiffSummaryTransformer;
import domain.port.TemplateDiffProvider;
import domain.port.TemplateVersionProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain service that resolves engagement update state without loading full engagement files.
 * Compares the indexed engagement version against the latest published template version
 * and produces both list-level summaries and detailed change breakdowns.
 */
public class UpdateStateResolver {

    private final TemplateDiffProvider diffProvider;
    private final DiffSummaryTransformer summaryTransformer;
    private final TemplateVersionProvider templateProvider;

    public UpdateStateResolver(TemplateDiffProvider diffProvider,
                               DiffSummaryTransformer summaryTransformer,
                               TemplateVersionProvider templateProvider) {
        this.diffProvider = Objects.requireNonNull(diffProvider, "diffProvider");
        this.summaryTransformer = Objects.requireNonNull(summaryTransformer, "summaryTransformer");
        this.templateProvider = Objects.requireNonNull(templateProvider, "templateProvider");
    }

    /**
     * Determines the update status for an engagement by comparing its version to the latest
     * template version. Decline logic: if {@code declinedVersion >= latest}, status is DECLINED;
     * if a newer version has since been published, the decline is superseded and status is PENDING.
     *
     * @param engagement      the lightweight index entry for the engagement
     * @param declinedVersion the version the user previously declined, or {@code null} if none
     * @return list-level summary including status, pending count, and declined version tracking
     */
    public EngagementUpdateSummary resolveUpdateState(EngagementRecord engagement,
                                                      Integer declinedVersion) {
        TemplateVersion latest = templateProvider.getLatestVersion(engagement.templateId());

        if (engagement.templateVersion() >= latest.version()) {
            return new EngagementUpdateSummary(
                engagement.engagementId(),
                engagement.name(),
                engagement.templateId(),
                latest.displayName(),
                engagement.templateVersion(),
                latest.version(),
                UpdateStatus.UP_TO_DATE,
                0,
                false,
                Instant.now(),
                null
            );
        }

        int pendingCount = latest.version() - engagement.templateVersion();

        if (declinedVersion != null && declinedVersion >= latest.version()) {
            return new EngagementUpdateSummary(
                engagement.engagementId(),
                engagement.name(),
                engagement.templateId(),
                latest.displayName(),
                engagement.templateVersion(),
                latest.version(),
                UpdateStatus.DECLINED,
                pendingCount,
                true,
                Instant.now(),
                declinedVersion
            );
        }

        return new EngagementUpdateSummary(
            engagement.engagementId(),
            engagement.name(),
            engagement.templateId(),
            latest.displayName(),
            engagement.templateVersion(),
            latest.version(),
            UpdateStatus.PENDING,
            pendingCount,
            true,
            Instant.now(),
            declinedVersion
        );
    }

    /**
     * Computes detailed change information for an engagement's pending update.
     * Produces two views: a collapsed summary (single diff from current to latest) and
     * step-by-step summaries (one diff per consecutive version increment).
     *
     * @param engagement the lightweight index entry for the engagement
     * @return detail-level response with both summary views and freshness metadata
     */
    public EngagementUpdateDetails resolveUpdateDetails(EngagementRecord engagement) {
        TemplateVersion latest = templateProvider.getLatestVersion(engagement.templateId());
        int currentVersion = engagement.templateVersion();
        int latestVersion = latest.version();

        // Collapsed summary: direct diff from current → latest
        TemplateDiff collapsedDiff = diffProvider.computeDiff(
            engagement.templateId(), currentVersion, latestVersion
        );
        ChangeSummary collapsedSummary = summaryTransformer.transform(collapsedDiff);

        // Step-by-step summaries: consecutive diffs for each intermediate version
        List<ChangeSummary> stepByStepSummaries = new ArrayList<>();
        List<TemplateVersion> intermediateVersions = templateProvider.getVersionsBetween(
            engagement.templateId(), currentVersion, latestVersion
        );

        int previousVersion = currentVersion;
        for (TemplateVersion version : intermediateVersions) {
            TemplateDiff stepDiff = diffProvider.computeDiff(
                engagement.templateId(), previousVersion, version.version()
            );
            stepByStepSummaries.add(summaryTransformer.transform(stepDiff));
            previousVersion = version.version();
        }

        var freshness = new EngagementUpdateDetails.Freshness(
            Instant.now(),
            latest.publishedAt()
        );

        return new EngagementUpdateDetails(
            engagement.engagementId(),
            currentVersion,
            latestVersion,
            collapsedSummary,
            stepByStepSummaries,
            freshness
        );
    }
}
