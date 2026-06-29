package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.service.AuthenticationService;
import br.com.fiap.pos.tech_challenge.core.util.AuthUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signout")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class SignoutController {

    private final AuthenticationService service;

    @PostMapping
    @Operation(summary = "Sign out and invalidate the current session token.", operationId = "sign-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> signout(final HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION)
                .replace("Bearer", "").trim();
        UserDetailsImpl user = AuthUtility.getLoggedUser();
        service.signout(token, user);
        return ResponseEntity.noContent().build();
    }
}
