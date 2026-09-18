package domain.port;

import domain.model.ChangeSummary;

import java.util.List;
import java.util.Optional;

/**
 * Port for storing and retrieving pre-computed change summaries.
 *
 * <p>Summaries are keyed by {@code (templateId, fromVersion, toVersion)} and shared across
 * all firms — the same template version gap produces identical summaries regardless of
 * which firm's engagements are affected.
 *
 * <h3>Architecture role</h3>
 * <ul>
 *   <li><b>Write side ({@link domain.service.TemplateUpdateProcessor}):</b>
 *       After computing diffs and transforming them into human-readable summaries, the
 *       processor stores them here via {@link #save}. It also checks {@link #findByVersionRange}
 *       to avoid recomputing summaries that already exist (idempotency for reconciliation).</li>
 *   <li><b>Read side ({@link domain.service.UpdateStateResolver}):</b>
 *       When the client requests update details, the resolver reads the pre-computed collapsed
 *       summary via {@link #findByVersionRange} and step-by-step summaries via
 *       {@link #findStepSummaries}. It never computes or writes summaries.</li>
 * </ul>
 *
 * <p>This store is what makes the read side a pure lookup — all computation happens at
 * event time (template published), not at request time (user opens details).
 */
public interface UpdateSummaryRepository {

    /**
     * Retrieves the collapsed summary for a specific version range.
     * Used by the read side to serve detail requests and by the write side
     * to check existence before recomputing.
     *
     * @return the summary, or empty if not yet computed
     */
    Optional<ChangeSummary> findByVersionRange(String templateId, int fromVersion, int toVersion);

    /**
     * Retrieves all consecutive step-by-step summaries between two versions, ordered ascending.
     * Used by the read side to serve the per-version breakdown in the detail view.
     *
     * @return list of summaries for each consecutive version pair, empty if none computed
     */
    List<ChangeSummary> findStepSummaries(String templateId, int fromVersion, int toVersion);

    /**
     * Persists a computed summary for future retrieval.
     * Called only by the write side after transforming a diff.
     */
    void save(String templateId, ChangeSummary summary);
}
