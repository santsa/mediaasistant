package com.santsa.mediaassistant.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

class SecurityUtilsTest {

    private final SecurityUtils securityUtils = new SecurityUtils();

    private static Authentication withAuthorities(String... authorities) {
        return new TestingAuthenticationToken("user", null, authorities);
    }

    @Test
    void extractRole_whenAdmin_returnsAdmin() {
        assertThat(securityUtils.extractRole(withAuthorities("ROLE_ADMIN"))).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_whenPatient_returnsPatient() {
        assertThat(securityUtils.extractRole(withAuthorities("ROLE_PATIENT"))).isEqualTo("PATIENT");
    }

    @Test
    void extractRole_whenOtherAuthoritiesPresent_ignoresThemAndReturnsKnownRole() {
        Authentication auth = withAuthorities("ROLE_offline_access", "SCOPE_openid", "ROLE_PATIENT");

        assertThat(securityUtils.extractRole(auth)).isEqualTo("PATIENT");
    }

    @Test
    @DisplayName("With both roles ADMIN wins, whatever the authority order")
    void extractRole_whenAdminAndPatient_returnsAdminRegardlessOfOrder() {
        assertThat(securityUtils.extractRole(withAuthorities("ROLE_ADMIN", "ROLE_PATIENT"))).isEqualTo("ADMIN");
        assertThat(securityUtils.extractRole(withAuthorities("ROLE_PATIENT", "ROLE_ADMIN"))).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_whenNoKnownRole_returnsUnknown() {
        assertThat(securityUtils.extractRole(withAuthorities("ROLE_USER", "ADMIN"))).isEqualTo("UNKNOWN");
    }

    @Test
    void extractRole_whenNoAuthorities_returnsUnknown() {
        assertThat(securityUtils.extractRole(withAuthorities())).isEqualTo("UNKNOWN");
    }
}
