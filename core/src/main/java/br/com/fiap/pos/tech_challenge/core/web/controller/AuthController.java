package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.application.dto.LoginDTO;
import br.com.fiap.pos.tech_challenge.core.application.AuthenticationService;
import br.com.fiap.pos.tech_challenge.core.util.TokenDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signin")
@RequiredArgsConstructor
@Tag(name = "Authentication")
@SecurityRequirements
public class AuthController {

    private final AuthenticationService service;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "API to sign in into the application.",
            operationId = "sign-in"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Support object to login.",
            required = true,
            content = @Content(schema = @Schema(implementation = LoginDTO.class))
    )
    public ResponseEntity<TokenDTO> singIn(@RequestBody @Valid final LoginDTO dto) {
        return ResponseEntity.ok(service.authenticate(dto));
    }
}
