package domain.model;

import java.util.List;

public record SectionChange(
    String sectionPath,
    String sectionDisplayName,
    List<HumanReadableChange> changes
) {}
