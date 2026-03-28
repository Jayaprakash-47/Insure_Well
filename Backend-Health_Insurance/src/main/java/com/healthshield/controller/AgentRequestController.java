package com.healthshield.controller;



import com.healthshield.dto.request.AgentRequestCreateDTO;
import com.healthshield.dto.response.AgentRequestResponse;
import com.healthshield.entity.User;
import com.healthshield.service.AgentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/agent-requests")
@RequiredArgsConstructor
public class AgentRequestController {

    private final AgentRequestService agentRequestService;

    /**
     * POST /api/agent-requests
     * Customer creates a new "Request Agent Help" ticket.
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AgentRequestResponse> createRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AgentRequestCreateDTO dto) {
        return new ResponseEntity<>(
                agentRequestService.createRequest(user.getUserId(), dto),
                HttpStatus.CREATED);
    }

    /**
     * GET /api/agent-requests/my-requests
     * Customer views their own requests.
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<AgentRequestResponse>> getMyRequests(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                agentRequestService.getMyRequests(user.getUserId()));
    }

    /**
     * PUT /api/agent-requests/{id}/cancel
     * Customer cancels a pending request.
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AgentRequestResponse> cancelRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                agentRequestService.cancelRequest(user.getUserId(), id));
    }
}