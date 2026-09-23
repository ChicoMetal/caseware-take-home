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
 *   <li><b>Write side ({@link domain.service.TemplateUpdateProcessor}):</b> After identifying
 *       affected engagements via {@link TemplateLookupRepository}, the processor calls
 *       {@link #updateState} to materialize status, latestVersion, and summaryAvailable.</li>
 *   <li><b>Read side ({@link domain.service.UpdateStateResolver}):</b> Client queries use
 *       {@link #findByFirmId} for the engagement list and {@link #findById(String, String)} for
 *       tenant-scoped detail/decision lookups. Decisions also call {@link #updateState} to
 *       record apply/decline outcomes.</li>
 * </ul>
 */
public interface EngagementIndexRepository {

    /**
     * Lists all indexed engagements for a firm.
     * Used by the read side to serve the engagement list endpoint.
     */
    List<EngagementRecord> findByFirmId(String firmId);

    /**
     * Looks up a single engagement's materialized state within a firm boundary.
     * Used by the read side for detail retrieval and decision processing.
     * The firmId parameter enforces tenant isolation at the port level —
     * callers cannot access engagements belonging to other firms.
     */
    Optional<EngagementRecord> findById(String firmId, String engagementId);

    /**
     * Atomically updates the full materialized state of an engagement in the index.
     * The firmId parameter enforces tenant isolation on writes — the adapter must include
     * it in the WHERE clause (e.g., {@code WHERE engagement_id = ? AND firm_id = ?}).
     * Called by the write side after computing summaries and by the read side after decisions.
     */
    void updateState(String firmId, String engagementId, int templateVersion, UpdateStatus status,
                     int latestVersion, Integer declinedVersion, boolean summaryAvailable);
}
