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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<UserDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "API to find a user by it's id.",
            operationId = "find-by-id"
    )
    @Parameter(
            name = "id",
            description = "The user's id.",
            required = true,
            content = @Content(schema = @Schema(implementation = Long.class))
    )
    public ResponseEntity<UserDTO> findById(@PathVariable final Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "API to create a user.",
            description = "Login and e-mail must be unique.",
            operationId = "create-user"
    )
    public ResponseEntity<UserDTO> create(@RequestBody @Valid final CreateUserDTO dto) {
        final var created = service.create(dto);
        return ResponseEntity.created(WebUtility.getLocation(created.id())).body(created);
    }

    @DeleteMapping(path = "/{id}")
    @Operation(
            summary = "API to delete a user by it's id.",
            operationId = "delete-user"
    )
    @Parameter(
            name = "id",
            description = "The user's id.",
            required = true,
            content = @Content(schema = @Schema(implementation = Long.class))
    )
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(
            path = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "API to update a user.",
            description = "Login and e-mail must be unique.",
            operationId = "update-user"
    )
    @Parameter(
            name = "id",
            description = "The user's id.",
            required = true,
            content = @Content(schema = @Schema(implementation = Long.class))
    )
    public ResponseEntity<UserDTO> update(@PathVariable final Long id,
                                          @RequestBody @Valid final UpdateUserDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
}
