package domain.port;

import domain.model.TemplateDiff;

/**
 * Port for the existing template comparison tool.
 *
 * <p>Wraps the pre-existing diff infrastructure that can compare any two versions
 * of a template and produce a structured list of JSON Patch-style operations.
 *
 * <h3>Architecture role</h3>
 * <ul>
 *   <li><b>Used exclusively by the write side ({@link domain.service.TemplateUpdateProcessor}).</b>
 *       When a template is published, the processor calls this port once per distinct version gap
 *       to compute both the collapsed diff (currentVersion → latestVersion) and step-by-step
 *       diffs (each consecutive version pair).</li>
 *   <li><b>Never called at query time.</b> The read side retrieves pre-computed summaries
 *       from the {@link UpdateSummaryRepository} instead.</li>
 * </ul>
 *
 * <p>The adapter implementation delegates to whatever comparison tool the platform
 * provides (e.g., a JSON diff library operating on template JSON documents).
 */
public interface TemplateDiffProvider {

    /**
     * Computes a structured diff between two versions of a template.
     *
     * @param templateId  the template to compare
     * @param fromVersion the baseline version
     * @param toVersion   the target version
     * @return structured diff containing all JSON Patch operations between the two versions
     */
    TemplateDiff computeDiff(String templateId, int fromVersion, int toVersion);
}
