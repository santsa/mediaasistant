package com.santsa.mediaassistant.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.santsa.mediaassistant.agent.AppointmentChainService;
import com.santsa.mediaassistant.agent.RoutingWorkflowService;
import com.santsa.mediaassistant.agent.orchestrator.OrchestratorService;
import com.santsa.mediaassistant.dto.ChatRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
public class AgentController {

        private final AppointmentChainService appointmentChainService;
        private final RoutingWorkflowService routingWorkflowService;
        private final OrchestratorService orchestratorService;

        @PostMapping("/chain/appointment")
        public ResponseEntity<String> bookAppointmentChain(
                        @Valid @RequestBody ChatRequest request,
                        @AuthenticationPrincipal Jwt jwt,
                        Authentication authentication) {

                return ResponseEntity.ok(appointmentChainService
                                .bookAppointmentChain(request.prompt(), request.model(), jwt.getClaim("userId")));
        }

        @PostMapping("/routing")
        public ResponseEntity<String> routing(
                        @Valid @RequestBody ChatRequest request,
                        @AuthenticationPrincipal Jwt jwt,
                        Authentication authentication) {

                return ResponseEntity.ok(routingWorkflowService
                                .routeQuery(request.prompt(), request.model(), jwt.getClaim("userId")));
        }

        @PostMapping("/orchestrator")
        public ResponseEntity<String> orchestrator(
                        @Valid @RequestBody ChatRequest request,
                        @AuthenticationPrincipal Jwt jwt,
                        Authentication authentication) {

                return ResponseEntity.ok(orchestratorService
                                .orchestrate(request.prompt(), request.model(), jwt.getClaim("userId")));
        }

        @PostMapping("/chain/appointment/confirm")
        public ResponseEntity<String> confirmBooking(
                        @AuthenticationPrincipal Jwt jwt) {

                return ResponseEntity.ok(appointmentChainService.confirmBooking(jwt.getClaim("userId")));
        }

        @PostMapping("/chain/appointment/cancel")
        public ResponseEntity<String> cancelBooking(
                        @AuthenticationPrincipal Jwt jwt) {

                return ResponseEntity.ok(appointmentChainService.cancelBooking(jwt.getClaim("userId")));
        }

        @PostMapping(value = "/orchestrator/progress", produces = "text/event-stream; charset=UTF-8")
        public Flux<String> orchestratorWithProgress(
                        @Valid @RequestBody ChatRequest request,
                        @AuthenticationPrincipal Jwt jwt,
                        Authentication authentication) {

                return orchestratorService
                                .orchestrateWithProgress(request.prompt(), request.model(), jwt.getClaim("userId"));
        }

}
