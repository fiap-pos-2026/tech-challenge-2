package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ServiceOrderStatusResponse;
import br.com.fiap.pos.tech_challenge.core.domain.*;
import br.com.fiap.pos.tech_challenge.core.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.exception.QuoteNotFoundException;
import br.com.fiap.pos.tech_challenge.core.exception.ServiceOrderNotFoundException;
import br.com.fiap.pos.tech_challenge.core.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.repository.*;
import br.com.fiap.pos.tech_challenge.core.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class ServiceOrderService {

    private static final long APPROVAL_WINDOW_HOURS = 168L;

    @Value("${app.rework.hourly-rate:150.00}")
    private BigDecimal reworkHourlyRate;

    private final ServiceOrderRepository serviceOrderRepository;
    private final QuoteRepository quoteRepository;
    private final ReworkCycleRepository reworkCycleRepository;
    private final MechanicalServiceRepository mechanicalServiceRepository;
    private final ProductRepository productRepository;
    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final StockService stockService;
    private final OTPService otpService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final ServiceOrderMapper mapper;

    @Transactional
    public ServiceOrderResponse openServiceOrder(UUID customerUuid, UUID vehicleUuid,
                                                  String customerComplaint) {
        Customer customer = customerService.findEntityByUuid(customerUuid);
        Vehicle vehicle = vehicleService.findEntityByUuid(vehicleUuid);

        if (!vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new CoreException(EApplicationError.VEHICLE_NOT_OWNED_BY_CUSTOMER);
        }

        ServiceOrder so = new ServiceOrder();
        so.setStatus(ServiceOrderStatus.RECEIVED);
        so.setCustomerComplaint(customerComplaint);
        so.setCustomer(customer);
        so.setVehicle(vehicle);

        return buildResponse(serviceOrderRepository.save(so));
    }

    @Transactional
    public ServiceOrderResponse startDiagnosis(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.RECEIVED);
        so.setStatus(ServiceOrderStatus.IN_DIAGNOSIS);
        return buildResponse(serviceOrderRepository.save(so));
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse addServiceToDiagnosis(UUID osUuid, UUID mechanicalServiceUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        MechanicalService ms = mechanicalServiceRepository.findByUuid(mechanicalServiceUuid)
                .orElseThrow(MechanicalServiceNotFoundException::new);

        Quote quote = getOrCreateProvisionalQuote(so);

        QuoteServiceLine line = new QuoteServiceLine();
        line.setQuote(quote);
        line.setMechanicalService(ms);
        line.setNameSnapshot(ms.getName());
        line.setPriceSnapshot(ms.getBasePrice());
        line.setEstimatedDurationMinutes(ms.getEstimatedDurationMinutes());
        quote.getServiceLines().add(line);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
        return buildResponse(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse removeServiceFromDiagnosis(UUID osUuid, UUID mechanicalServiceUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = getOrCreateProvisionalQuote(so);
        boolean removed = quote.getServiceLines().removeIf(
                line -> line.getMechanicalService().getUuid().equals(mechanicalServiceUuid));

        if (!removed) {
            throw new MechanicalServiceNotFoundException();
        }

        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
        return buildResponse(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse addProductToDiagnosis(UUID osUuid, UUID productUuid, BigDecimal quantity) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        try {
            stockService.checkAvailability(productUuid, quantity);
        } catch (InsufficientStockException e) {
            notificationService.publishInsufficientStockNotification(
                    "Estoque insuficiente do produto " + productUuid + " na OS " + osUuid + ".", so);
            throw e;
        }

        Quote quote = getOrCreateProvisionalQuote(so);

        QuoteProductLine line = new QuoteProductLine();
        line.setQuote(quote);
        line.setProduct(product);
        line.setNameSnapshot(product.getName());
        line.setUnitPriceSnapshot(product.getUnitPrice());
        line.setQuantity(quantity);
        line.setMeasurementUnit(product.getMeasurementUnit());
        line.setUnbudgeted(false);
        quote.getProductLines().add(line);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
        return buildResponse(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse removeProductFromDiagnosis(UUID osUuid, UUID productUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = getOrCreateProvisionalQuote(so);
        boolean removed = quote.getProductLines().removeIf(
                line -> line.getProduct().getUuid().equals(productUuid));

        if (!removed) {
            throw new ProductNotFoundException();
        }

        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
        return buildResponse(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse completeDiagnosis(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = getOrCreateProvisionalQuote(so);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);

        so.setStatus(ServiceOrderStatus.AWAITING_APPROVAL);
        so.setApprovalExpiresAt(Instant.now().plus(APPROVAL_WINDOW_HOURS, ChronoUnit.HOURS));
        ServiceOrder saved = serviceOrderRepository.save(so);

        otpService.generateAndSend(saved);
        return buildResponse(saved);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse approveQuote(UUID osUuid, String customerDocument, String tokenRaw) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.AWAITING_APPROVAL);

        Quote quote = latestQuote(so);
        boolean firstApproval = quote.getApprovedAt() == null;

        if (!firstApproval) {
            quote.getProductLines()
                    .stream()
                    .filter(QuoteProductLine::isUnbudgeted)
                    .forEach(l -> l.setUnbudgeted(false));
        }

        quote.setApprovedAt(Instant.now());
        quoteRepository.save(quote);

        so.setStatus(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder saved = serviceOrderRepository.save(so);

        if (firstApproval) {
            for (QuoteProductLine line : quote.getProductLines()) {
                if (line.isUnbudgeted()) continue;
                try {
                    stockService.debit(line.getProduct().getUuid(), line.getQuantity(), saved, null);
                } catch (InsufficientStockException _) {
                    notificationService.publishInsufficientStockNotification(
                            "Estoque insuficiente do produto " + line.getProduct().getUuid()
                                    + " na OS " + osUuid + ".", saved);
                }
            }
        }

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ORDER_APPROVED,
                "OS " + osUuid + " aprovada pelo cliente.", saved);

        return buildResponse(saved);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse rejectQuote(UUID osUuid, String customerDocument, String tokenRaw,
                                             UserDetailsImpl principal) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.AWAITING_APPROVAL);

        Quote quote = latestQuote(so);
        List<QuoteProductLine> unbudgeted = quote.getProductLines().stream()
                .filter(QuoteProductLine::isUnbudgeted).toList();

        if (unbudgeted.isEmpty()) {
            so.setStatus(ServiceOrderStatus.CANCELLED);
            ServiceOrder saved = serviceOrderRepository.save(so);
            notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.QUOTE_REJECTED,
                    "Orçamento da OS " + osUuid + " rejeitado pelo cliente.", saved);
            return buildResponse(saved);
        }

        User actor = resolveActor(principal);
        for (QuoteProductLine line : unbudgeted) {
            stockService.compensate(line.getProduct().getUuid(), line.getQuantity(), so, actor);
        }
        quote.getProductLines().removeAll(unbudgeted);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);

        so.setStatus(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder saved = serviceOrderRepository.save(so);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ADDENDUM_PRODUCT_REJECTED,
                "Adendo da OS " + osUuid + " rejeitado pelo cliente.", saved);

        return buildResponse(saved);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public void resendOTP(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        if (so.getStatus() != ServiceOrderStatus.AWAITING_APPROVAL
                && so.getStatus() != ServiceOrderStatus.COMPLETED) {
            throw new InvalidStatusTransitionException();
        }
        otpService.invalidateByServiceOrder(so);
        otpService.generateAndSend(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse requestProduct(UUID osUuid, UUID productUuid,
                                                BigDecimal quantity, UserDetailsImpl principal) {
        ServiceOrder so = findServiceOrder(osUuid);

        boolean isAddendumPhase = so.getStatus() == ServiceOrderStatus.AWAITING_APPROVAL
                && latestQuote(so).getApprovedAt() != null;

        if (so.getStatus() != ServiceOrderStatus.IN_PROGRESS && !isAddendumPhase) {
            throw new InvalidStatusTransitionException();
        }

        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        User actor = resolveActor(principal);
        try {
            stockService.debit(productUuid, quantity, so, actor);
        } catch (InsufficientStockException e) {
            notificationService.publishInsufficientStockNotification(
                    "Estoque insuficiente do produto " + productUuid + " na OS " + osUuid + ".", so);
            throw e;
        }

        Quote quote = latestQuote(so);

        QuoteProductLine line = new QuoteProductLine();
        line.setQuote(quote);
        line.setProduct(product);
        line.setNameSnapshot(product.getName());
        line.setUnitPriceSnapshot(product.getUnitPrice());
        line.setQuantity(quantity);
        line.setMeasurementUnit(product.getMeasurementUnit());
        line.setUnbudgeted(true);
        quote.getProductLines().add(line);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);

        if (so.getStatus() == ServiceOrderStatus.IN_PROGRESS) {
            so.setStatus(ServiceOrderStatus.AWAITING_APPROVAL);
            so.setApprovalExpiresAt(Instant.now().plus(APPROVAL_WINDOW_HOURS, ChronoUnit.HOURS));
            so = serviceOrderRepository.save(so);
            otpService.generateAndSend(so);
        }

        return buildResponse(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse returnProduct(UUID osUuid, UUID productUuid,
                                               String rawPassword, UserDetailsImpl principal) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_PROGRESS);

        authenticationService.validatePassword(principal.getLogin(), rawPassword);

        Quote quote = latestQuote(so);
        QuoteProductLine line = quote.getProductLines().stream()
                .filter(l -> l.getProduct().getUuid().equals(productUuid))
                .findFirst()
                .orElseThrow(ProductNotFoundException::new);

        User actor = resolveActor(principal);
        stockService.credit(productUuid, line.getQuantity(), so, actor);

        quote.getProductLines().remove(line);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);

        auditLogService.register(AuditEventType.PRODUCT_RETURN, actor,
                principal.getLogin(), "200", "Produto " + productUuid + " devolvido da OS " + osUuid);

        return buildResponse(so);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse completeExecution(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_PROGRESS);
        so.setStatus(ServiceOrderStatus.COMPLETED);
        ServiceOrder saved = serviceOrderRepository.save(so);
        otpService.generateAndSend(saved);
        notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.EXECUTION_COMPLETED,
                "OS " + osUuid + " concluída pelo mecânico. Cliente notificado para vistoria de entrega.", saved);
        return buildResponse(saved);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse acceptDelivery(UUID osUuid, String customerDocument,
                                                String tokenRaw, boolean byAttendantJwt) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.COMPLETED);

        if (!byAttendantJwt) {
            otpService.validate(osUuid, customerDocument, tokenRaw);
        }

        so.setStatus(ServiceOrderStatus.DELIVERED);
        return buildResponse(serviceOrderRepository.save(so));
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse rejectDelivery(UUID osUuid, String customerDocument,
                                                String tokenRaw, String reason) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.COMPLETED);

        so.setStatus(ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder saved = serviceOrderRepository.save(so);

        Quote quote = latestQuote(saved);
        int totalEstimatedMinutes = quote.getServiceLines().stream()
                .mapToInt(QuoteServiceLine::getEstimatedDurationMinutes)
                .sum();
        BigDecimal internalCost = BigDecimal.valueOf(totalEstimatedMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                .multiply(reworkHourlyRate);

        ReworkCycle rework = new ReworkCycle();
        rework.setServiceOrder(saved);
        rework.setRejectionReason(reason);
        rework.setEstimatedDurationMinutes(totalEstimatedMinutes);
        rework.setEstimatedInternalCost(internalCost);
        reworkCycleRepository.save(rework);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.DELIVERY_REJECTED,
                "Entrega da OS " + osUuid + " rejeitada. Motivo: " + reason, saved);

        return buildResponse(saved);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional
    public ServiceOrderResponse closeDispute(UUID osUuid, String resolution, UserDetailsImpl principal) {
        ServiceOrder so = findServiceOrder(osUuid);
        if (so.getStatus() != ServiceOrderStatus.IN_PROGRESS
                && so.getStatus() != ServiceOrderStatus.COMPLETED) {
            throw new InvalidStatusTransitionException();
        }

        so.setStatus(ServiceOrderStatus.DISPUTED);
        ServiceOrder saved = serviceOrderRepository.save(so);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ORDER_DISPUTED,
                "OS " + osUuid + " encerrada como DISPUTED. Resolução: " + resolution, saved);

        User actor = resolveActor(principal);
        auditLogService.register(AuditEventType.DISPUTED_CLOSURE, actor,
                principal != null ? principal.getLogin() : null, "200",
                "OS " + osUuid + " encerrada como DISPUTED");

        return buildResponse(saved);
    }

    // -----------------------------------------------------------------------
    // -----------------------------------------------------------------------
    @Transactional(readOnly = true)
    public ServiceOrderStatusResponse getServiceOrderStatus(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        return new ServiceOrderStatusResponse(so.getUuid(), so.getStatus(), so.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public ServiceOrderResponse getServiceOrder(UUID osUuid) {
        return buildResponse(findServiceOrder(osUuid));
    }

    @Transactional(readOnly = true)
    public Page<ServiceOrderResponse> listServiceOrders(ServiceOrderStatus status, UUID customerUuid,
                                                         LocalDateTime from, LocalDateTime to,
                                                         Pageable pageable) {
        return serviceOrderRepository
                .findWithFilters(status, customerUuid, from, to, pageable)
                .map(this::buildResponse);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------
    private ServiceOrder findServiceOrder(UUID uuid) {
        return serviceOrderRepository.findByUuid(uuid)
                .orElseThrow(ServiceOrderNotFoundException::new);
    }

    private void requireStatus(ServiceOrder so, ServiceOrderStatus required) {
        if (so.getStatus() != required) {
            throw new InvalidStatusTransitionException();
        }
    }

    private Quote getOrCreateProvisionalQuote(ServiceOrder so) {
        return quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId())
                .orElseGet(() -> {
                    Quote q = new Quote();
                    q.setServiceOrder(so);
                    q.setTotalAmount(BigDecimal.ZERO);
                    return quoteRepository.save(q);
                });
    }

    private Quote latestQuote(ServiceOrder so) {
        return quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId())
                .orElseThrow(QuoteNotFoundException::new);
    }

    private BigDecimal calculateTotal(Quote quote) {
        BigDecimal services = quote.getServiceLines().stream()
                .map(QuoteServiceLine::getPriceSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal products = quote.getProductLines().stream()
                .map(l -> l.getUnitPriceSnapshot().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return services.add(products);
    }

    private User resolveActor(UserDetailsImpl principal) {
        if (principal == null) return null;
        return userRepository.findById(principal.getId()).orElse(null);
    }

    private ServiceOrderResponse buildResponse(ServiceOrder so) {
        ServiceOrderResponse base = mapper.toResponse(so);
        return quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId())
                .map(q -> new ServiceOrderResponse(
                        base.uuid(), base.status(), base.customerComplaint(),
                        mapper.toQuoteResponse(q), base.createdAt()))
                .orElse(base);
    }
}
