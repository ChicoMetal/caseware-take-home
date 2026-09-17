package domain.port;

import domain.model.TemplateVersion;

import java.util.List;

/**
 * Port for querying available template versions from the template database.
 */
public interface TemplateVersionProvider {

    /**
     * @param templateId the template to query
     * @return the most recently published version
     */
    TemplateVersion getLatestVersion(String templateId);

    /**
     * @param templateId    the template to query
     * @param fromExclusive lower version bound (exclusive)
     * @param toInclusive   upper version bound (inclusive)
     * @return versions in ascending order, used for step-by-step diff computation
     */
    List<TemplateVersion> getVersionsBetween(String templateId, int fromExclusive, int toInclusive);
}
