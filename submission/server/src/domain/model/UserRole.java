package domain.model;

/** Authorization roles for template update operations. */
public enum UserRole {
    /** Can view engagements, review details, and make apply/decline decisions. */
    ADMIN,
    /** Can view engagements and review details. Cannot make decisions. */
    VIEWER
}
