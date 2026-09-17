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

    List<EngagementRecord> findByFirmId(String firmId);

    Optional<EngagementRecord> findById(String engagementId);

    void updateVersion(String engagementId, int newVersion);
}
