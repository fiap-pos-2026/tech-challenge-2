package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.application.dto.*;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import org.springframework.format.annotation.DateTimeFormat;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.QuoteApprovalService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderDeliveryService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderDiagnosisService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderDisputeService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderExecutionService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderOpeningService;
import br.com.fiap.pos.tech_challenge.core.application.serviceorder.ServiceOrderQueryService;
import br.com.fiap.pos.tech_challenge.core.util.WebUtility;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import java.time.LocalDateTime;
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
@RequestMapping("/api/service-orders")
@RequiredArgsConstructor
@Tag(name = "Service Orders")
public class ServiceOrderController {

    private final ServiceOrderOpeningService openingService;
    private final ServiceOrderDiagnosisService diagnosisService;
    private final QuoteApprovalService approvalService;
    private final ServiceOrderExecutionService executionService;
    private final ServiceOrderDeliveryService deliveryService;
    private final ServiceOrderDisputeService disputeService;
    private final ServiceOrderQueryService queryService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Open a new service order", operationId = "open-service-order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service order created"),
            @ApiResponse(responseCode = "404", description = "Customer, vehicle, service or product not found")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<ServiceOrderResponse> open(@RequestBody @Valid OpenServiceOrderRequest request) {
        ServiceOrderResponse created = openingService.openServiceOrder(
                request.customerUuid(), request.vehicleUuid(), request.customerComplaint(),
                request.mechanicalServiceUuids(), request.products());
        return ResponseEntity.created(WebUtility.getLocation(created.uuid())).body(created);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List service orders (paginated, filterable by status, customer and date)",
            operationId = "list-service-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<Page<ServiceOrderResponse>> list(
            @RequestParam(required = false) ServiceOrderStatus status,
            @RequestParam(required = false) java.util.UUID customerUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(queryService.listServiceOrders(status, customerUuid, from, to, pageable));
    }

    @GetMapping(path = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a service order by UUID", operationId = "get-service-order")
    @Parameter(name = "uuid", description = "Service order UUID", required = true,
            content = @Content(schema = @Schema(implementation = UUID.class)))
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(queryService.getServiceOrder(uuid));
    }

    @GetMapping(path = "/{uuid}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get simplified status of a service order (public — for customer tracking)",
            operationId = "get-service-order-status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "404", description = "Service order not found")
    })
    public ResponseEntity<ServiceOrderStatusResponse> getStatus(@PathVariable UUID uuid) {
        return ResponseEntity.ok(queryService.getServiceOrderStatus(uuid));
    }

    @PostMapping(path = "/{uuid}/diagnosis/start", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Start diagnosis for a service order", operationId = "start-diagnosis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diagnosis started"),
            @ApiResponse(responseCode = "409", description = "Service order is not in RECEIVED status")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> startDiagnosis(@PathVariable UUID uuid) {
        return ResponseEntity.ok(diagnosisService.startDiagnosis(uuid));
    }

    @PostMapping(path = "/{uuid}/diagnosis/services",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a mechanical service to the diagnosis", operationId = "add-service-to-diagnosis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> addService(@PathVariable UUID uuid,
                                                            @RequestBody @Valid AddServiceRequest request) {
        return ResponseEntity.ok(diagnosisService.addServiceToDiagnosis(uuid, request.mechanicalServiceUuid()));
    }

    @DeleteMapping(path = "/{uuid}/diagnosis/services/{mechanicalServiceUuid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Remove a mechanical service from the diagnosis", operationId = "remove-service-from-diagnosis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service removed, quote recalculated"),
            @ApiResponse(responseCode = "404", description = "Service not found in diagnosis"),
            @ApiResponse(responseCode = "409", description = "Service order is not in IN_DIAGNOSIS status")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> removeService(@PathVariable UUID uuid,
                                                               @PathVariable UUID mechanicalServiceUuid) {
        return ResponseEntity.ok(diagnosisService.removeServiceFromDiagnosis(uuid, mechanicalServiceUuid));
    }

    @PostMapping(path = "/{uuid}/diagnosis/products",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a product to the diagnosis", operationId = "add-product-to-diagnosis")
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> addProduct(@PathVariable UUID uuid,
                                                            @RequestBody @Valid AddProductRequest request) {
        return ResponseEntity.ok(diagnosisService.addProductToDiagnosis(uuid, request.productUuid(), request.quantity()));
    }

    @DeleteMapping(path = "/{uuid}/diagnosis/products/{productUuid}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Remove a product from the diagnosis", operationId = "remove-product-from-diagnosis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product removed, quote recalculated"),
            @ApiResponse(responseCode = "404", description = "Product not found in diagnosis"),
            @ApiResponse(responseCode = "409", description = "Service order is not in IN_DIAGNOSIS status")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> removeProductFromDiagnosis(@PathVariable UUID uuid,
                                                                            @PathVariable UUID productUuid) {
        return ResponseEntity.ok(diagnosisService.removeProductFromDiagnosis(uuid, productUuid));
    }

    @PostMapping(path = "/{uuid}/diagnosis/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Complete the diagnosis and generate a quote", operationId = "complete-diagnosis")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Diagnosis completed, OTP sent to customer"),
            @ApiResponse(responseCode = "409", description = "Service order is not in IN_DIAGNOSIS status")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> completeDiagnosis(@PathVariable UUID uuid) {
        return ResponseEntity.ok(diagnosisService.completeDiagnosis(uuid));
    }

    @PostMapping(path = "/{uuid}/approval",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Customer approves or rejects the quote via OTP", operationId = "quote-approval")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision processed"),
            @ApiResponse(responseCode = "422", description = "INVALID_OTP_SUBMISSION"),
            @ApiResponse(responseCode = "429", description = "OTP_LIMIT_REACHED"),
            @ApiResponse(responseCode = "409", description = "Service order is not in AWAITING_APPROVAL")
    })
    @PreAuthorize("permitAll()")
    public ResponseEntity<ServiceOrderResponse> quoteDecision(@PathVariable UUID uuid,
                                                               @RequestBody @Valid QuoteDecisionRequest request) {
        ServiceOrderResponse response = switch (request.decision()) {
            case APPROVE -> approvalService.approveQuote(uuid, request.customerDocument(), request.token());
            case REJECT -> approvalService.rejectQuote(uuid, request.customerDocument(), request.token());
        };
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/{uuid}/otp/resend", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Resend OTP token to customer", operationId = "resend-otp")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP resent"),
            @ApiResponse(responseCode = "409", description = "Service order is not in AWAITING_APPROVAL")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<Void> resendOtp(@PathVariable UUID uuid) {
        approvalService.resendOTP(uuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/{uuid}/execution/products",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Request a product during execution (debits stock)", operationId = "request-product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product requested; unbudgeted items trigger new approval flow"),
            @ApiResponse(responseCode = "422", description = "Insufficient stock"),
            @ApiResponse(responseCode = "409", description = "Service order is not in IN_PROGRESS or AWAITING_APPROVAL")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> requestProduct(@PathVariable UUID uuid,
                                                                @RequestBody @Valid RequestProductRequest request) {
        return ResponseEntity.ok(executionService.requestProduct(uuid, request.productUuid(), request.quantity()));
    }

    @DeleteMapping(path = "/{uuid}/products/{productUuid}",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Return a product to stock (requires re-authentication)", operationId = "return-product")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product returned, quote recalculated"),
            @ApiResponse(responseCode = "401", description = "Incorrect password"),
            @ApiResponse(responseCode = "404", description = "Product not found in service order"),
            @ApiResponse(responseCode = "422", description = "Product is not returnable"),
            @ApiResponse(responseCode = "409", description = "Service order is not in IN_PROGRESS")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<ServiceOrderResponse> returnProduct(@PathVariable UUID uuid,
                                                               @PathVariable UUID productUuid,
                                                               @RequestBody @Valid ReturnProductRequest request) {
        return ResponseEntity.ok(executionService.returnProduct(uuid, productUuid, request.password()));
    }

    @PostMapping(path = "/{uuid}/execution/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Complete execution and mark service order as COMPLETED", operationId = "complete-execution")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Execution completed"),
            @ApiResponse(responseCode = "409", description = "Service order is not in IN_PROGRESS")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'MECHANIC')")
    public ResponseEntity<ServiceOrderResponse> completeExecution(@PathVariable UUID uuid) {
        return ResponseEntity.ok(executionService.completeExecution(uuid));
    }

    @PostMapping(path = "/{uuid}/delivery/accept",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Accept delivery (Attendant via JWT or Customer via OTP)", operationId = "accept-delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery accepted — service order DELIVERED"),
            @ApiResponse(responseCode = "422", description = "INVALID_OTP_SUBMISSION (OTP path)"),
            @ApiResponse(responseCode = "409", description = "Service order is not in COMPLETED")
    })
    @PreAuthorize("permitAll()")
    public ResponseEntity<ServiceOrderResponse> acceptDelivery(@PathVariable UUID uuid,
                                                                @RequestBody(required = false) AcceptDeliveryRequest request) {
        String token = (request != null) ? request.token() : null;
        String doc = (request != null) ? request.customerDocument() : null;
        return ResponseEntity.ok(deliveryService.acceptDelivery(uuid, doc, token));
    }

    @PostMapping(path = "/{uuid}/delivery/reject",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reject delivery and return service order for rework", operationId = "reject-delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery rejected — service order back to IN_PROGRESS"),
            @ApiResponse(responseCode = "422", description = "INVALID_OTP_SUBMISSION"),
            @ApiResponse(responseCode = "409", description = "Service order is not in COMPLETED")
    })
    @PreAuthorize("permitAll()")
    public ResponseEntity<ServiceOrderResponse> rejectDelivery(@PathVariable UUID uuid,
                                                                @RequestBody @Valid RejectDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.rejectDelivery(uuid, request.customerDocument(),
                request.token(), request.reason()));
    }

    @PostMapping(path = "/{uuid}/close-dispute",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Close service order as DISPUTED (Attendant only)", operationId = "close-dispute")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service order closed as DISPUTED"),
            @ApiResponse(responseCode = "409", description = "Service order is in a terminal state")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'ATTENDANT')")
    public ResponseEntity<ServiceOrderResponse> closeDispute(@PathVariable UUID uuid,
                                                              @RequestBody(required = false) CloseDisputeRequest request) {
        String resolution = (request != null) ? request.resolution() : null;
        return ResponseEntity.ok(disputeService.closeDispute(uuid, resolution));
    }
}
