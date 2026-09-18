package domain.port;

import domain.model.ChangeSummary;

import java.util.List;
import java.util.Optional;

/**
 * Port for storing and retrieving pre-computed change summaries.
 * Summaries are keyed by (templateId, fromVersion, toVersion) and shared across all firms.
 */
public interface UpdateSummaryRepository {

    /**
     * Retrieves a previously computed summary for the given version range.
     *
     * @return the summary, or empty if not yet computed
     */
    Optional<ChangeSummary> findByVersionRange(String templateId, int fromVersion, int toVersion);

    /**
     * Retrieves all consecutive step-by-step summaries between two versions, ordered ascending.
     *
     * @return list of summaries for each consecutive version pair, empty if none computed
     */
    List<ChangeSummary> findStepSummaries(String templateId, int fromVersion, int toVersion);

    /**
     * Persists a computed summary for future retrieval.
     */
    void save(String templateId, ChangeSummary summary);
}
