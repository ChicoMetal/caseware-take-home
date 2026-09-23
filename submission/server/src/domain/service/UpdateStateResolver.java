package domain.service;

import domain.model.*;
import domain.port.DecisionAuditLog;
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
    private final DecisionAuditLog auditLog;

    public UpdateStateResolver(EngagementIndexRepository indexRepository,
                               TemplateVersionProvider templateProvider,
                               UpdateSummaryRepository summaryRepository,
                               DecisionAuditLog auditLog) {
        this.indexRepository = Objects.requireNonNull(indexRepository, "indexRepository");
        this.templateProvider = Objects.requireNonNull(templateProvider, "templateProvider");
        this.summaryRepository = Objects.requireNonNull(summaryRepository, "summaryRepository");
        this.auditLog = Objects.requireNonNull(auditLog, "auditLog");
    }

    /**
     * Lists all engagements for a firm with their pre-materialized update status.
     * Maps index entries to API response objects.
     * Requires any authenticated role (ADMIN or VIEWER).
     *
     * @param user the authenticated user context (provides firmId for tenant isolation)
     * @return list of engagement update summaries ready for the client
     */
    public List<EngagementUpdateSummary> listEngagementUpdates(UserContext user) {
        List<EngagementRecord> engagements = indexRepository.findByFirmId(user.firmId());

        return engagements.stream()
            .map(this::toSummary)
            .toList();
    }

    /**
     * Retrieves pre-computed change details for an engagement's pending update.
     * Requires any authenticated role (ADMIN or VIEWER).
     *
     * @param user         the authenticated user context (provides firmId for tenant isolation)
     * @param engagementId the engagement to query
     * @return detail-level response with both summary views
     * @throws IllegalStateException if the engagement is not found or summaries not yet computed
     */
    public EngagementUpdateDetails getUpdateDetails(UserContext user, String engagementId) {
        EngagementRecord engagement = indexRepository.findById(user.firmId(), engagementId)
            .orElseThrow(() -> new IllegalStateException("Engagement not found"));

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
     * Requires ADMIN role — viewers cannot make decisions.
     *
     * @param user         the authenticated user context (provides firmId + role check)
     * @param engagementId the engagement being acted on
     * @param decision     the user's decision
     * @return response confirming the outcome
     * @throws SecurityException if the user lacks ADMIN role
     * @throws IllegalStateException if engagement not found or targetVersion is stale
     */
    public UpdateDecisionResponse processDecision(UserContext user, String engagementId, UpdateDecision decision) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(engagementId, "engagementId");
        Objects.requireNonNull(decision, "decision");

        if (user.role() != UserRole.ADMIN) {
            throw new SecurityException("Insufficient permissions");
        }

        EngagementRecord engagement = indexRepository.findById(user.firmId(), engagementId)
            .orElseThrow(() -> new IllegalStateException("Engagement not found"));

        if (decision.targetVersion() != engagement.latestVersion()) {
            throw new IllegalStateException(
                "Stale decision: target v%d but latest is v%d".formatted(
                    decision.targetVersion(), engagement.latestVersion())
            );
        }

        int previousVersion = engagement.templateVersion();

        if (decision.decision() == DecisionType.APPLY) {
            indexRepository.updateState(
                user.firmId(),
                engagementId,
                decision.targetVersion(),
                UpdateStatus.UP_TO_DATE,
                decision.targetVersion(),
                null,
                false
            );
            var response = new UpdateDecisionResponse(
                engagementId, user.userId(), DecisionType.APPLY, previousVersion,
                decision.targetVersion(), DecisionStatus.PROCESSING
            );
            auditLog.record(response);
            return response;
        } else {
            indexRepository.updateState(
                user.firmId(),
                engagementId,
                engagement.templateVersion(),
                UpdateStatus.DECLINED,
                engagement.latestVersion(),
                decision.targetVersion(),
                engagement.summaryAvailable()
            );
            var response = new UpdateDecisionResponse(
                engagementId, user.userId(), DecisionType.DECLINE, previousVersion,
                decision.targetVersion(), DecisionStatus.ACCEPTED
            );
            auditLog.record(response);
            return response;
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
