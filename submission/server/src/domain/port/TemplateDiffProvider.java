package domain.port;

import domain.model.TemplateDiff;

/**
 * Port for the existing template comparison tool.
 * Compares any two versions of the same template and returns a structured diff.
 */
public interface TemplateDiffProvider {

    /**
     * @param templateId  the template to compare
     * @param fromVersion the baseline version (exclusive)
     * @param toVersion   the target version (inclusive)
     * @return structured diff containing all changes between the two versions
     */
    TemplateDiff computeDiff(String templateId, int fromVersion, int toVersion);
}
