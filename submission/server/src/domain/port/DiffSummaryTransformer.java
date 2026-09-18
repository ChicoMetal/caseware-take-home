package domain.port;

import domain.model.ChangeSummary;
import domain.model.TemplateDiff;

/**
 * Port for transforming a raw technical diff into a human-readable change summary.
 *
 * <p>This is the core interpretation layer: it takes machine-readable diff operations
 * and produces auditor-friendly descriptions grouped by template section.
 *
 * <h3>Architecture role</h3>
 * <ul>
 *   <li><b>Used exclusively by the write side ({@link domain.service.TemplateUpdateProcessor}).</b>
 *       After the {@link TemplateDiffProvider} produces a raw diff, the processor passes it
 *       through this transformer to generate the human-readable summary, then stores the
 *       result via {@link UpdateSummaryRepository}.</li>
 *   <li><b>Swappable strategy:</b> The current implementation ({@code RuleBasedDiffSummaryTransformer})
 *       uses deterministic rules. A future LLM-enhanced implementation can be swapped in
 *       behind this same interface without changing the domain or the write pipeline.</li>
 * </ul>
 *
 * <p>Note: The transformer only sees the {@link TemplateDiff} — it has no access to template
 * metadata like {@code publishedAt}. The processor corrects the timestamp after transformation.
 */
public interface DiffSummaryTransformer {

    /**
     * Transforms a raw diff into a human-readable summary grouped by template section.
     *
     * @param diff raw diff between two template versions
     * @return summary with sections, change descriptions, and impact levels
     */
    ChangeSummary transform(TemplateDiff diff);
}
