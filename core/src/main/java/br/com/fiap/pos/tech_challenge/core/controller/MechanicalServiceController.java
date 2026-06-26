package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateServiceRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.MechanicalServiceResponse;
import br.com.fiap.pos.tech_challenge.core.service.MechanicalServiceService;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-25
 */
@RestController
@RequestMapping("/api/catalog/services")
@RequiredArgsConstructor
@Tag(name = "Catalog - Mechanical Services")
public class MechanicalServiceController {

    private final MechanicalServiceService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new mechanical service", operationId = "create-mechanical-service")
    public ResponseEntity<MechanicalServiceResponse> create(@RequestBody @Valid CreateServiceRequest request) {
        final var created = service.create(request);
        return ResponseEntity.created(WebUtility.getLocation(created.uuid())).body(created);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all mechanical services (paginated)", operationId = "list-mechanical-services")
    public ResponseEntity<Page<MechanicalServiceResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping(path = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find a mechanical service by UUID", operationId = "find-mechanical-service-by-uuid")
    @Parameter(
            name = "uuid",
            description = "The mechanical service UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    public ResponseEntity<MechanicalServiceResponse> findByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.findByUuid(uuid));
    }

    @PutMapping(path = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a mechanical service", operationId = "update-mechanical-service")
    @Parameter(
            name = "uuid",
            description = "The mechanical service UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    public ResponseEntity<MechanicalServiceResponse> update(@PathVariable UUID uuid,
                                                            @RequestBody @Valid CreateServiceRequest request) {
        return ResponseEntity.ok(service.update(uuid, request));
    }

    @DeleteMapping(path = "/{uuid}")
    @Operation(summary = "Delete a mechanical service", operationId = "delete-mechanical-service",
            description = "Fails with 409 if the service is linked to an active service order.")
    @Parameter(
            name = "uuid",
            description = "The mechanical service UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        service.delete(uuid);
        return ResponseEntity.noContent().build();
    }
}
