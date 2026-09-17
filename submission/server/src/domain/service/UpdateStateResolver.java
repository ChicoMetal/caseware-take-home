package domain.service;

import domain.model.*;
import domain.port.DiffSummaryTransformer;
import domain.port.TemplateDiffProvider;
import domain.port.TemplateVersionProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UpdateStateResolver {

    private final TemplateDiffProvider diffProvider;
    private final DiffSummaryTransformer summaryTransformer;
    private final TemplateVersionProvider templateProvider;

    public UpdateStateResolver(TemplateDiffProvider diffProvider,
                               DiffSummaryTransformer summaryTransformer,
                               TemplateVersionProvider templateProvider) {
        this.diffProvider = diffProvider;
        this.summaryTransformer = summaryTransformer;
        this.templateProvider = templateProvider;
    }

    public EngagementUpdateSummary resolveUpdateState(EngagementRecord engagement) {
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
                Instant.now()
            );
        }

        int pendingCount = latest.version() - engagement.templateVersion();

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
            Instant.now()
        );
    }

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
