package domain.port;

import domain.model.EngagementRecord;
import domain.model.UpdateStatus;

import java.util.List;
import java.util.Optional;

/**
 * Port for the lightweight engagement-template index.
 *
 * <p>Avoids the ~1 minute per-engagement loading constraint by maintaining a
 * fast-access store of materialized engagement state. This is the central
 * read model in the CQRS architecture.
 *
 * <h3>Architecture role</h3>
 * <ul>
 *   <li><b>Initial population:</b> A one-time backfill job loads each existing engagement
 *       (~1 min each), extracts (engagementId, firmId, templateId, version), and writes
 *       the initial index entry. New engagements are indexed at creation time.</li>
 *   <li><b>Write side ({@link domain.service.TemplateUpdateProcessor}):</b> When a template
 *       is published, the processor queries {@link #findByTemplateWithVersionBelow} to find
 *       affected engagements, then calls {@link #updateState} to materialize the new status,
 *       latestVersion, and summaryAvailable flag for each one.</li>
 *   <li><b>Read side ({@link domain.service.UpdateStateResolver}):</b> Client queries use
 *       {@link #findByFirmId} for the engagement list and {@link #findById} for detail/decision
 *       lookups. Decisions also call {@link #updateState} to record apply/decline outcomes.</li>
 * </ul>
 */
public interface EngagementIndexRepository {

    /**
     * Lists all indexed engagements for a firm.
     * Used by the read side to serve the engagement list endpoint.
     */
    List<EngagementRecord> findByFirmId(String firmId);

    /**
     * Looks up a single engagement's materialized state.
     * Used by the read side for detail retrieval and decision processing.
     */
    Optional<EngagementRecord> findById(String engagementId);

    /**
     * Finds all engagements using a template whose current version is behind a threshold.
     * Used by the write side when a template is published to identify which engagements
     * need their state updated and summaries computed.
     */
    List<EngagementRecord> findByTemplateWithVersionBelow(String templateId, int belowVersion);

    /**
     * Atomically updates the full materialized state of an engagement in the index.
     * Called by the write side after computing summaries (to set PENDING/DECLINED + latestVersion)
     * and by the read side after processing a decision (to set UP_TO_DATE or DECLINED).
     */
    void updateState(String engagementId, int templateVersion, UpdateStatus status,
                     int latestVersion, Integer declinedVersion, boolean summaryAvailable);
}
