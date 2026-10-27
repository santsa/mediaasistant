package com.santsa.mediaassistant.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.santsa.mediaassistant.agent.AppointmentChainService;
import com.santsa.mediaassistant.dto.ChatRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
public class agentController {

    private final AppointmentChainService appointmentChainService;

    @PostMapping("/chain/appointment")
    public ResponseEntity<String> bookAppointmentChain(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication) {

        return ResponseEntity.ok(appointmentChainService
                .bookAppointmentChain(request.prompt(), request.model(), jwt.getClaim("userId")));
    }

}
