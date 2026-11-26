package com.santsa.mediaassistant.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Unit test of the Keycloak role mapping (realm_access + resource_access.medassistant-client). */
class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();

    private List<String> authoritiesOf(Consumer<Map<String, Object>> claims) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("user").claims(claims).build();
        AbstractAuthenticationToken auth = config.jwtAuthenticationConverter().convert(jwt);
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                // Spring Security 7 always adds FACTOR_BEARER (MFA factor), unrelated to the role mapping.
                .filter(a -> !a.startsWith("FACTOR_"))
                .toList();
    }

    @Test
    void realmRoles_arePrefixedWithRole() {
        List<String> authorities = authoritiesOf(c -> c.put("realm_access", Map.of("roles", List.of("PATIENT"))));

        assertThat(authorities).containsExactly("ROLE_PATIENT");
    }

    @Test
    void clientRoles_ofMedassistantClient_arePrefixedWithRole() {
        List<String> authorities = authoritiesOf(c -> c.put("resource_access",
                Map.of("medassistant-client", Map.of("roles", List.of("ADMIN")))));

        assertThat(authorities).containsExactly("ROLE_ADMIN");
    }

    @Test
    void clientAndRealmRoles_areCombined() {
        List<String> authorities = authoritiesOf(c -> {
            c.put("resource_access", Map.of("medassistant-client", Map.of("roles", List.of("ADMIN"))));
            c.put("realm_access", Map.of("roles", List.of("PATIENT", "offline_access")));
        });

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_PATIENT", "ROLE_offline_access");
    }

    @Test
    void rolesOfOtherClients_areIgnored() {
        List<String> authorities = authoritiesOf(c -> c.put("resource_access",
                Map.of("account", Map.of("roles", List.of("ADMIN")))));

        assertThat(authorities).isEmpty();
    }

    @Test
    void scopes_areKeptAsScopeAuthorities() {
        List<String> authorities = authoritiesOf(c -> {
            c.put("scope", "openid profile");
            c.put("realm_access", Map.of("roles", List.of("PATIENT")));
        });

        assertThat(authorities).containsExactlyInAnyOrder("SCOPE_openid", "SCOPE_profile", "ROLE_PATIENT");
    }

    @Test
    void nonStringRolesAndMissingRolesList_areIgnored() {
        List<String> authorities = authoritiesOf(c -> {
            c.put("realm_access", Map.of("roles", List.of("PATIENT", 42)));
            c.put("resource_access", Map.of("medassistant-client", Map.of("other", "x")));
        });

        assertThat(authorities).containsExactly("ROLE_PATIENT");
    }

    @Test
    void tokenWithoutRoleClaims_hasNoAuthorities() {
        assertThat(authoritiesOf(c -> c.put("userId", 1L))).isEmpty();
    }
}
