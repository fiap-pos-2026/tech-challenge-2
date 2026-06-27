package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.NotificationResponse;
import br.com.fiap.pos.tech_challenge.core.mapper.NotificationMapper;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper mapper;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List notifications for the authenticated user (paginated)", operationId = "list-notifications")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<NotificationResponse>> listNotifications(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listByUser(principal.getId(), pageable).map(mapper::toResponse));
    }

    @PatchMapping(path = "/{uuid}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mark a notification as read", operationId = "mark-notification-read")
    @Parameter(
            name = "uuid",
            description = "The notification UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MECHANIC')")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable UUID uuid,
                                                           @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(mapper.toResponse(notificationService.markAsRead(uuid, principal.getId())));
    }
}
