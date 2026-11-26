package com.santsa.mediaassistant.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

/** Shared JWT helpers for the {@code @WebMvcTest} controller tests. */
final class ControllerTestSupport {

    static final long USER_ID = 1L;

    private ControllerTestSupport() {
    }

    static JwtRequestPostProcessor patient() {
        return userWithRole("ROLE_PATIENT");
    }

    static JwtRequestPostProcessor admin() {
        return userWithRole("ROLE_ADMIN");
    }

    /** Authenticated user whose token has no PATIENT/ADMIN role. */
    static JwtRequestPostProcessor userWithoutRole() {
        return userWithRole("ROLE_USER");
    }

    private static JwtRequestPostProcessor userWithRole(String role) {
        return jwt().jwt(j -> j.claim("userId", USER_ID)).authorities(new SimpleGrantedAuthority(role));
    }

    static String body(String prompt) {
        return "{\"prompt\":\"" + prompt + "\",\"model\":\"gemini\"}";
    }
}
