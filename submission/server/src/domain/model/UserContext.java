package domain.model;

/**
 * Authenticated user context carrying tenant identity and authorization role.
 * Extracted from the authentication token by the infrastructure layer and
 * passed into domain services — the domain never inspects tokens directly.
 */
public record UserContext(
    String userId,
    String firmId,
    UserRole role
) {}
