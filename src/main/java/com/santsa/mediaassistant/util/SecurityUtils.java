package com.santsa.mediaassistant.util;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    /**
     * Returns ADMIN, PATIENT or UNKNOWN. If the token carries both roles ADMIN wins, regardless of the
     * order in which Keycloak lists the authorities.
     */
    public String extractRole(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (authorities.contains("ROLE_ADMIN")) {
            return "ADMIN";
        }
        if (authorities.contains("ROLE_PATIENT")) {
            return "PATIENT";
        }
        return "UNKNOWN";
    }

}
