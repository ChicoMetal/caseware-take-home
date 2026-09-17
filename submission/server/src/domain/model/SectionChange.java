package domain.model;

import java.util.List;

/** Grouped changes within a single template section (e.g. Planning, Materiality). */
public record SectionChange(
    String sectionPath,
    String sectionDisplayName,
    List<HumanReadableChange> changes
) {}
