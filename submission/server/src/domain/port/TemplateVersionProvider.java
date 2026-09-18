package domain.port;

import domain.model.TemplateVersion;

import java.util.List;

/**
 * Port for querying available template versions from the template database.
 *
 * <p>Provides version metadata needed by both sides of the CQRS architecture.
 *
 * <h3>Architecture role</h3>
 * <ul>
 *   <li><b>Write side ({@link domain.service.TemplateUpdateProcessor}):</b>
 *       Uses {@link #getVersionsBetween} to discover intermediate versions for step-by-step
 *       diff computation, and {@link #getLatestVersion} during reconciliation to find the
 *       current target version.</li>
 *   <li><b>Read side ({@link domain.service.UpdateStateResolver}):</b>
 *       Uses {@link #getLatestVersion} to resolve the template display name for the
 *       engagement list response and to provide freshness metadata in detail responses.</li>
 * </ul>
 *
 * <p>The adapter implementation queries the Template DB where version history is stored.
 */
public interface TemplateVersionProvider {

    /**
     * Returns the most recently published version of a template.
     * Used by both sides: write (reconciliation target) and read (display name, freshness).
     */
    TemplateVersion getLatestVersion(String templateId);

    /**
     * Returns all versions between two bounds, in ascending order.
     * Used by the write side to enumerate intermediate versions for step-by-step diffs.
     *
     * @param fromExclusive lower version bound (exclusive)
     * @param toInclusive   upper version bound (inclusive)
     * @return versions in ascending order; empty if no intermediates exist
     */
    List<TemplateVersion> getVersionsBetween(String templateId, int fromExclusive, int toInclusive);
}
