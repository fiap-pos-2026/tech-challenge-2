package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.application.dto.ChangePasswordDTO;
import br.com.fiap.pos.tech_challenge.core.application.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.application.UserService;
import br.com.fiap.pos.tech_challenge.core.util.AuthUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author johncgo
 * @since 2026-06-27
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile")
public class ProfileController {

    private final UserService userService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get authenticated user profile", operationId = "get-profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getProfile() {
        return ResponseEntity.ok(userService.getLoggedUser());
    }

    @PutMapping(path = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change authenticated user password", operationId = "change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid final ChangePasswordDTO dto) {
        UserDetailsImpl loggedUser = AuthUtility.getLoggedUser();
        userService.changePassword(loggedUser.getId(), dto);
        return ResponseEntity.noContent().build();
    }
}
