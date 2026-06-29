package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateUserDTO;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UserDTO;
import br.com.fiap.pos.tech_challenge.core.service.UserService;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService service;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "API to find all users registered.",
            operationId = "find-all"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping(path = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "API to find a user by it's uuid.",
            operationId = "find-by-uuid"
    )
    @Parameter(
            name = "uuid",
            description = "The user's uuid.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> findByUuid(@PathVariable final UUID uuid) {
        return ResponseEntity.ok(service.findByUuid(uuid));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "API to create a user.",
            description = "Login and e-mail must be unique.",
            operationId = "create-user"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> create(@RequestBody @Valid final CreateUserDTO dto) {
        final var created = service.create(dto);
        return ResponseEntity.created(WebUtility.getLocation(created.uuid())).body(created);
    }

    @DeleteMapping(path = "/{uuid}")
    @Operation(
            summary = "API to delete a user by it's uuid.",
            operationId = "delete-user"
    )
    @Parameter(
            name = "uuid",
            description = "The user's uuid.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable final UUID uuid) {
        service.deleteByUuid(uuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(
            path = "/{uuid}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "API to update a user.",
            description = "Login and e-mail must be unique.",
            operationId = "update-user"
    )
    @Parameter(
            name = "uuid",
            description = "The user's uuid.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> update(@PathVariable final UUID uuid,
                                          @RequestBody @Valid final UpdateUserDTO dto) {
        return ResponseEntity.ok(service.update(uuid, dto));
    }
}
