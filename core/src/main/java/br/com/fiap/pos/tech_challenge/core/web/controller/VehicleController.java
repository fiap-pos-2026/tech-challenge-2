package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.VehicleResponse;
import br.com.fiap.pos.tech_challenge.core.application.VehicleService;
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

import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles")
public class VehicleController {

    private final VehicleService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a new vehicle", operationId = "register-vehicle")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<VehicleResponse> register(@RequestBody @Valid CreateVehicleRequest request) {
        final var created = service.register(request);
        return ResponseEntity.created(WebUtility.getLocation(created.uuid())).body(created);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find a vehicle by license plate", operationId = "find-vehicle-by-license-plate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<VehicleResponse> findByLicensePlate(@RequestParam String licensePlate) {
        return ResponseEntity.ok(service.findByLicensePlate(licensePlate));
    }

    @PutMapping(path = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a vehicle", operationId = "update-vehicle")
    @Parameter(name = "uuid", description = "The vehicle UUID.", required = true,
            content = @Content(schema = @Schema(implementation = UUID.class)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<VehicleResponse> update(@PathVariable UUID uuid,
                                                   @RequestBody @Valid UpdateVehicleRequest request) {
        return ResponseEntity.ok(service.update(uuid, request));
    }

    @DeleteMapping(path = "/{uuid}")
    @Operation(summary = "Delete a vehicle", operationId = "delete-vehicle",
            description = "Fails with 409 if the vehicle has active service orders.")
    @Parameter(name = "uuid", description = "The vehicle UUID.", required = true,
            content = @Content(schema = @Schema(implementation = UUID.class)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        service.delete(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find a vehicle by UUID", operationId = "find-vehicle-by-uuid")
    @Parameter(
            name = "uuid",
            description = "The vehicle UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<VehicleResponse> findByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.findByUuid(uuid));
    }
}
