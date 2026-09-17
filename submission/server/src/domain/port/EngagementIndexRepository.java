package domain.port;

import domain.model.EngagementRecord;

import java.util.List;
import java.util.Optional;

/**
 * Port for the lightweight engagement-template index.
 * Avoids the ~1 minute per-engagement loading constraint by caching
 * (engagementId, templateId, currentVersion) in a fast-access store.
 */
public interface EngagementIndexRepository {

    /**
     * @param firmId the audit firm identifier
     * @return all engagement index entries for the firm
     */
    List<EngagementRecord> findByFirmId(String firmId);

    /**
     * @param engagementId the engagement identifier
     * @return the index entry, or empty if the engagement is not indexed
     */
    Optional<EngagementRecord> findById(String engagementId);

    /**
     * Updates the cached template version after a successful update apply.
     *
     * @param engagementId the engagement to update
     * @param newVersion   the template version the engagement is now on
     */
    void updateVersion(String engagementId, int newVersion);
}
