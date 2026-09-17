package domain.port;

import domain.model.ChangeSummary;
import domain.model.TemplateDiff;

/**
 * Port for transforming a raw technical diff into a human-readable change summary.
 * Allows different strategies: rule-based (deterministic) or LLM-enhanced.
 */
public interface DiffSummaryTransformer {

    /**
     * @param diff raw diff between two template versions
     * @return human-readable summary grouped by template section
     */
    ChangeSummary transform(TemplateDiff diff);
}
