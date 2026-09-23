package domain.port;

import domain.model.EngagementRecord;

import java.util.List;

/**
 * Write-side-only port for cross-tenant engagement lookups by template.
 *
 * <p>This port exists separately from {@link EngagementIndexRepository} to enforce
 * at compile time that the read side (which handles client queries) cannot access
 * cross-tenant data. Only the write side ({@link domain.service.TemplateUpdateProcessor})
 * should inject this interface.
 *
 * <p><b>Security note:</b> This port returns engagements across ALL firms for a given
 * template. It must only be exposed behind an infrastructure-authenticated boundary
 * (e.g., internal event bus, scheduler) — never as a client-facing API.
 */
public interface TemplateLookupRepository {

    /**
     * Finds all engagements using a template whose current version is behind a threshold.
     * Returns records from all firms — this is intentionally cross-tenant for
     * event-driven processing when a template version is published.
     */
    List<EngagementRecord> findByTemplateWithVersionBelow(String templateId, int belowVersion);
}
