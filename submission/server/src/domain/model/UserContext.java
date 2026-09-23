package domain.model;

import java.util.Objects;

/**
 * Authenticated user context carrying tenant identity and authorization role.
 * Extracted from the authentication token by the infrastructure layer and
 * passed into domain services — the domain never inspects tokens directly.
 */
public record UserContext(
    String userId,
    String firmId,
    UserRole role
) {
    public UserContext {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(firmId, "firmId");
        Objects.requireNonNull(role, "role");
    }
}
