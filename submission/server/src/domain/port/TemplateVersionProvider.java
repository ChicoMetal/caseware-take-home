package domain.port;

import domain.model.TemplateVersion;

import java.util.List;

/**
 * Port for querying available template versions from the template database.
 */
public interface TemplateVersionProvider {

    TemplateVersion getLatestVersion(String templateId);

    List<TemplateVersion> getVersionsBetween(String templateId, int fromExclusive, int toInclusive);
}
