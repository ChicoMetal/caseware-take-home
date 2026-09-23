package domain.service;

import domain.model.*;
import domain.port.EngagementIndexRepository;
import domain.port.TemplateVersionProvider;
import domain.port.UpdateSummaryRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Read-side query service for engagement update state.
 *
 * <p>All state is pre-materialized in the index by {@link TemplateUpdateProcessor}.
 * This service reads from the index and summary store — it never computes diffs
 * or resolves status. It also handles user decisions (apply/decline), which update
 * the index as a side effect.
 */
public class UpdateStateResolver {

    private final EngagementIndexRepository indexRepository;
    private final TemplateVersionProvider templateProvider;
    private final UpdateSummaryRepository summaryRepository;

    public UpdateStateResolver(EngagementIndexRepository indexRepository,
                               TemplateVersionProvider templateProvider,
                               UpdateSummaryRepository summaryRepository) {
        this.indexRepository = Objects.requireNonNull(indexRepository, "indexRepository");
        this.templateProvider = Objects.requireNonNull(templateProvider, "templateProvider");
        this.summaryRepository = Objects.requireNonNull(summaryRepository, "summaryRepository");
    }

    /**
     * Lists all engagements for a firm with their pre-materialized update status.
     * Maps index entries to API response objects.
     *
     * @param firmId the audit firm identifier
     * @return list of engagement update summaries ready for the client
     */
    public List<EngagementUpdateSummary> listEngagementUpdates(String firmId) {
        List<EngagementRecord> engagements = indexRepository.findByFirmId(firmId);

        return engagements.stream()
            .map(this::toSummary)
            .toList();
    }

    /**
     * Retrieves pre-computed change details for an engagement's pending update.
     *
     * @param firmId       the firm requesting access (tenant isolation)
     * @param engagementId the engagement to query
     * @return detail-level response with both summary views
     * @throws IllegalStateException if the engagement is not found or summaries not yet computed
     */
    public EngagementUpdateDetails getUpdateDetails(String firmId, String engagementId) {
        EngagementRecord engagement = indexRepository.findById(firmId, engagementId)
            .orElseThrow(() -> new IllegalStateException("Engagement not found: " + engagementId));

        TemplateVersion latest = templateProvider.getLatestVersion(engagement.templateId());

        ChangeSummary collapsedSummary = summaryRepository
            .findByVersionRange(engagement.templateId(), engagement.templateVersion(), engagement.latestVersion())
            .orElseThrow(() -> new IllegalStateException(
                "Summary not yet computed for %s v%d→v%d".formatted(
                    engagement.templateId(), engagement.templateVersion(), engagement.latestVersion())
            ));

        List<ChangeSummary> stepByStepSummaries = summaryRepository
            .findStepSummaries(engagement.templateId(), engagement.templateVersion(), engagement.latestVersion());

        var freshness = new EngagementUpdateDetails.Freshness(
            Instant.now(),
            latest.publishedAt()
        );

        return new EngagementUpdateDetails(
            engagement.engagementId(),
            engagement.templateVersion(),
            engagement.latestVersion(),
            collapsedSummary,
            stepByStepSummaries,
            freshness
        );
    }

    /**
     * Processes a user's decision to apply or decline a template update.
     *
     * @param firmId       the firm requesting access (tenant isolation)
     * @param engagementId the engagement being acted on
     * @param decision     the user's decision
     * @return response confirming the outcome
     * @throws IllegalStateException if engagement not found or targetVersion is stale
     */
    public UpdateDecisionResponse processDecision(String firmId, String engagementId, UpdateDecision decision) {
        EngagementRecord engagement = indexRepository.findById(firmId, engagementId)
            .orElseThrow(() -> new IllegalStateException("Engagement not found: " + engagementId));

        if (decision.targetVersion() != engagement.latestVersion()) {
            throw new IllegalStateException(
                "Stale decision: target v%d but latest is v%d".formatted(
                    decision.targetVersion(), engagement.latestVersion())
            );
        }

        int previousVersion = engagement.templateVersion();

        if (decision.decision() == DecisionType.APPLY) {
            indexRepository.updateState(
                engagementId,
                decision.targetVersion(),
                UpdateStatus.UP_TO_DATE,
                decision.targetVersion(),
                null,
                false
            );
            return new UpdateDecisionResponse(
                engagementId, DecisionType.APPLY, previousVersion,
                decision.targetVersion(), DecisionStatus.PROCESSING
            );
        } else {
            indexRepository.updateState(
                engagementId,
                engagement.templateVersion(),
                UpdateStatus.DECLINED,
                engagement.latestVersion(),
                decision.targetVersion(),
                engagement.summaryAvailable()
            );
            return new UpdateDecisionResponse(
                engagementId, DecisionType.DECLINE, previousVersion,
                decision.targetVersion(), DecisionStatus.ACCEPTED
            );
        }
    }

    private EngagementUpdateSummary toSummary(EngagementRecord engagement) {
        TemplateVersion latest = templateProvider.getLatestVersion(engagement.templateId());
        int pendingCount = engagement.latestVersion() - engagement.templateVersion();

        return new EngagementUpdateSummary(
            engagement.engagementId(),
            engagement.firmId(),
            engagement.name(),
            engagement.templateId(),
            latest.displayName(),
            engagement.templateVersion(),
            engagement.latestVersion(),
            engagement.status(),
            Math.max(0, pendingCount),
            engagement.summaryAvailable(),
            Instant.now(),
            engagement.declinedVersion()
        );
    }
}
