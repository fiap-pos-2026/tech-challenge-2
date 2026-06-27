package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateProductRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ProductResponse;
import br.com.fiap.pos.tech_challenge.core.service.ProductService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
@Tag(name = "Inventory - Products")
public class ProductController {

    private final ProductService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new product", operationId = "create-product")
    @PreAuthorize("hasRole('ATTENDANT')")
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid CreateProductRequest request) {
        final var created = service.create(request);
        return ResponseEntity.created(WebUtility.getLocation(created.uuid())).body(created);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all products (paginated)", operationId = "list-products")
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<ProductResponse>> findAll(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping(path = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Find a product by UUID", operationId = "find-product-by-uuid")
    @Parameter(
            name = "uuid",
            description = "The product UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasAnyRole('ATTENDANT', 'MECHANIC')")
    public ResponseEntity<ProductResponse> findByUuid(@PathVariable UUID uuid) {
        return ResponseEntity.ok(service.findByUuid(uuid));
    }

    @PutMapping(path = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a product", operationId = "update-product")
    @Parameter(
            name = "uuid",
            description = "The product UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasRole('ATTENDANT')")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID uuid,
                                                  @RequestBody @Valid CreateProductRequest request) {
        return ResponseEntity.ok(service.update(uuid, request));
    }

    @DeleteMapping(path = "/{uuid}")
    @Operation(summary = "Delete a product", operationId = "delete-product",
            description = "Fails with 409 if the product is linked to an active service order.")
    @Parameter(
            name = "uuid",
            description = "The product UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasRole('ATTENDANT')")
    public ResponseEntity<Void> delete(@PathVariable UUID uuid) {
        service.delete(uuid);
        return ResponseEntity.noContent().build();
    }
}
