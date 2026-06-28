package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.CustomerResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers")
public class CustomerController {

    private final CustomerService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Register a new customer", operationId = "register-customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<CustomerResponse> register(@RequestBody @Valid CreateCustomerRequest request) {
        final var created = service.register(request);
        return ResponseEntity.created(WebUtility.getLocation(created.uuid())).body(created);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find a customer by document (CPF/CNPJ)", operationId = "find-customer-by-document")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<CustomerResponse> findByDocument(@RequestParam String document) {
        return ResponseEntity.ok(service.findByDocument(document));
    }

    @GetMapping(path = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find a customer by UUID", operationId = "find-customer-by-uuid")
    @Parameter(
            name = "uuid",
            description = "The customer UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<CustomerResponse> findByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.findByUuid(uuid));
    }

    @PutMapping(path = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a customer", operationId = "update-customer")
    @Parameter(name = "uuid", description = "The customer UUID.", required = true,
            content = @Content(schema = @Schema(implementation = UUID.class)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID uuid,
                                                   @RequestBody @Valid UpdateCustomerRequest request) {
        return ResponseEntity.ok(service.update(uuid, request));
    }

    @DeleteMapping(path = "/{uuid}")
    @Operation(summary = "Delete a customer", operationId = "delete-customer",
            description = "Fails with 409 if the customer has active service orders.")
    @Parameter(name = "uuid", description = "The customer UUID.", required = true,
            content = @Content(schema = @Schema(implementation = UUID.class)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        service.delete(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/{uuid}/document", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get full unmasked document for a customer (ATTENDANT only)", operationId = "find-customer-full-document")
    @Parameter(
            name = "uuid",
            description = "The customer UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<String> findFullDocument(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.findFullDocument(uuid));
    }
}
