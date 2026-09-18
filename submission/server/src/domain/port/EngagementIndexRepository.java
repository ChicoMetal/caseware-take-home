package domain.port;

import domain.model.EngagementRecord;
import domain.model.UpdateStatus;

import java.util.List;
import java.util.Optional;

/**
 * Port for the lightweight engagement-template index.
 * Avoids the ~1 minute per-engagement loading constraint by caching
 * (engagementId, templateId, currentVersion, status) in a fast-access store.
 */
public interface EngagementIndexRepository {

    /** @return all engagement index entries for the given firm */
    List<EngagementRecord> findByFirmId(String firmId);

    /** @return the index entry, or empty if the engagement is not indexed */
    Optional<EngagementRecord> findById(String engagementId);

    /** @return all engagements using this template with a version below the given threshold */
    List<EngagementRecord> findByTemplateWithVersionBelow(String templateId, int belowVersion);

    /**
     * Updates the full materialized state of an engagement in the index.
     * Single write covering version, status, and tracking fields.
     */
    void updateState(String engagementId, int templateVersion, UpdateStatus status,
                     int latestVersion, Integer declinedVersion, boolean summaryAvailable);
}
