package domain.port;

import domain.model.TemplateDiff;

/**
 * Port for the existing template comparison tool.
 * Compares any two versions of the same template and returns a structured diff.
 */
public interface TemplateDiffProvider {

    TemplateDiff computeDiff(String templateId, int fromVersion, int toVersion);
}
