package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.ReplenishmentRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.StockMovementResponse;
import br.com.fiap.pos.tech_challenge.core.domain.StockMovement;
import br.com.fiap.pos.tech_challenge.core.mapper.StockMovementMapper;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.service.StockService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory - Stock")
public class InventoryController {

    private final StockService stockService;
    private final StockMovementMapper movementMapper;

    @PostMapping(path = "/products/{uuid}/replenishment",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Replenish product stock", operationId = "replenish-product")
    @Parameter(
            name = "uuid",
            description = "The product UUID.",
            required = true,
            content = @Content(schema = @Schema(implementation = UUID.class))
    )
    @PreAuthorize("hasRole('ATTENDANT')")
    public ResponseEntity<Void> replenish(@PathVariable UUID uuid,
                                          @RequestBody @Valid ReplenishmentRequest request,
                                          @AuthenticationPrincipal UserDetailsImpl principal) {
        stockService.replenish(uuid, request.quantity(), principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(path = "/movements", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all stock movements (paginated)", operationId = "list-stock-movements")
    @PreAuthorize("hasRole('ATTENDANT')")
    public ResponseEntity<Page<StockMovementResponse>> listMovements(
            @RequestParam(required = false) Long productId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<StockMovement> page = productId != null
                ? stockService.listMovementsByProduct(productId, pageable)
                : stockService.listAllMovements(pageable);
        return ResponseEntity.ok(page.map(movementMapper::toResponse));
    }
}
