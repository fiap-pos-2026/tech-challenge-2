package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.web.dto.OpenProductItemRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.ServiceOrderStatusResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.*;
import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.QuoteNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ServiceOrderNotFoundException;
import br.com.fiap.pos.tech_challenge.core.web.mapper.ServiceOrderMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.*;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.application.event.ServiceOrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import br.com.fiap.pos.tech_challenge.core.application.port.out.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public ServiceOrderResponse openServiceOrder(UUID customerUuid, UUID vehicleUuid,
                                                  String customerComplaint) {
        return openServiceOrder(customerUuid, vehicleUuid, customerComplaint, null, null);
    }

    @Transactional
    public ServiceOrderResponse openServiceOrder(UUID customerUuid, UUID vehicleUuid,
                                                  String customerComplaint,
                                                  List<UUID> mechanicalServiceUuids,
                                                  List<OpenProductItemRequest> products) {
        Customer customer = customerService.findEntityByUuid(customerUuid);
        Vehicle vehicle = vehicleService.findEntityByUuid(vehicleUuid);

        if (!vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new CoreException(EApplicationError.VEHICLE_NOT_OWNED_BY_CUSTOMER);
        }

        List<MechanicalService> services = resolveMechanicalServices(mechanicalServiceUuids);
        List<OpeningProductItem> productItems = resolveProducts(products);

        ServiceOrder so = new ServiceOrder();
        so.setCustomerComplaint(customerComplaint);
        so.setCustomer(customer);
        so.setVehicle(vehicle);

        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.RECEIVED);
        attachOpeningItems(saved, services, productItems);
        return buildResponse(saved);
    }

    @Transactional
    public ServiceOrderResponse startDiagnosis(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.RECEIVED);
        return buildResponse(persistStatusChange(so, ServiceOrderStatus.IN_DIAGNOSIS));
    }

    @Transactional
    public ServiceOrderResponse addServiceToDiagnosis(UUID osUuid, UUID mechanicalServiceUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        MechanicalService ms = mechanicalServiceRepository.findByUuid(mechanicalServiceUuid)
                .orElseThrow(MechanicalServiceNotFoundException::new);

        Quote quote = getOrCreateProvisionalQuote(so);

        quote.getServiceLines().add(buildServiceLine(quote, ms));
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
        return buildResponse(so);
    }

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

        quote.getProductLines().add(buildProductLine(quote, product, quantity));
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
        return buildResponse(so);
    }

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

    @Transactional
    public ServiceOrderResponse completeDiagnosis(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = getOrCreateProvisionalQuote(so);
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);

        so.setApprovalExpiresAt(Instant.now().plus(APPROVAL_WINDOW_HOURS, ChronoUnit.HOURS));
        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.AWAITING_APPROVAL);

        otpService.generateAndSend(saved);
        return buildResponse(saved);
    }

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

        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.IN_PROGRESS);

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
            ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.CANCELLED);
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

        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.IN_PROGRESS);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ADDENDUM_PRODUCT_REJECTED,
                "Adendo da OS " + osUuid + " rejeitado pelo cliente.", saved);

        return buildResponse(saved);
    }

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
            so.setApprovalExpiresAt(Instant.now().plus(APPROVAL_WINDOW_HOURS, ChronoUnit.HOURS));
            so = persistStatusChange(so, ServiceOrderStatus.AWAITING_APPROVAL);
            otpService.generateAndSend(so);
        }

        return buildResponse(so);
    }

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

    @Transactional
    public ServiceOrderResponse completeExecution(UUID osUuid) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.COMPLETED);
        otpService.generateAndSend(saved);
        notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.EXECUTION_COMPLETED,
                "OS " + osUuid + " concluída pelo mecânico. Cliente notificado para vistoria de entrega.", saved);
        return buildResponse(saved);
    }

    @Transactional
    public ServiceOrderResponse acceptDelivery(UUID osUuid, String customerDocument,
                                                String tokenRaw, boolean byAttendantJwt) {
        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.COMPLETED);

        if (!byAttendantJwt) {
            otpService.validate(osUuid, customerDocument, tokenRaw);
        }

        return buildResponse(persistStatusChange(so, ServiceOrderStatus.DELIVERED));
    }

    @Transactional
    public ServiceOrderResponse rejectDelivery(UUID osUuid, String customerDocument,
                                                String tokenRaw, String reason) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = findServiceOrder(osUuid);
        requireStatus(so, ServiceOrderStatus.COMPLETED);

        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.IN_PROGRESS);

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

    @Transactional
    public ServiceOrderResponse closeDispute(UUID osUuid, String resolution, UserDetailsImpl principal) {
        ServiceOrder so = findServiceOrder(osUuid);
        if (so.getStatus() != ServiceOrderStatus.IN_PROGRESS
                && so.getStatus() != ServiceOrderStatus.COMPLETED) {
            throw new InvalidStatusTransitionException();
        }

        ServiceOrder saved = persistStatusChange(so, ServiceOrderStatus.DISPUTED);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ORDER_DISPUTED,
                "OS " + osUuid + " encerrada como DISPUTED. Resolução: " + resolution, saved);

        User actor = resolveActor(principal);
        auditLogService.register(AuditEventType.DISPUTED_CLOSURE, actor,
                principal != null ? principal.getLogin() : null, "200",
                "OS " + osUuid + " encerrada como DISPUTED");

        return buildResponse(saved);
    }

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
                .findWithFilters(status, customerUuid, from, to, withoutClientSort(pageable))
                .map(this::buildResponse);
    }

    // A ordem de prioridade da listagem é definida na query; o sort enviado pelo client é descartado
    // para não sobrescrevê-la.
    private Pageable withoutClientSort(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
    }

    private ServiceOrder findServiceOrder(UUID uuid) {
        return serviceOrderRepository.findByUuid(uuid)
                .orElseThrow(ServiceOrderNotFoundException::new);
    }

    private void requireStatus(ServiceOrder so, ServiceOrderStatus required) {
        if (so.getStatus() != required) {
            throw new InvalidStatusTransitionException();
        }
    }

    private ServiceOrder persistStatusChange(ServiceOrder so, ServiceOrderStatus newStatus) {
        ServiceOrderStatus previousStatus = so.getStatus();
        so.setStatus(newStatus);
        ServiceOrder saved = serviceOrderRepository.save(so);
        Customer customer = saved.getCustomer();
        eventPublisher.publish(new ServiceOrderStatusChangedEvent(
                saved.getUuid(), previousStatus, saved.getStatus(),
                customer != null ? customer.getEmail() : null,
                customer != null ? customer.getName() : null));
        return saved;
    }

    private List<MechanicalService> resolveMechanicalServices(List<UUID> mechanicalServiceUuids) {
        if (mechanicalServiceUuids == null) return List.of();
        return mechanicalServiceUuids.stream()
                .map(uuid -> mechanicalServiceRepository.findByUuid(uuid)
                        .orElseThrow(MechanicalServiceNotFoundException::new))
                .toList();
    }

    private List<OpeningProductItem> resolveProducts(List<OpenProductItemRequest> products) {
        if (products == null) return List.of();
        return products.stream()
                .map(item -> new OpeningProductItem(
                        productRepository.findByUuid(item.productUuid())
                                .orElseThrow(ProductNotFoundException::new),
                        item.quantity()))
                .toList();
    }

    private void attachOpeningItems(ServiceOrder so, List<MechanicalService> services,
                                     List<OpeningProductItem> productItems) {
        if (services.isEmpty() && productItems.isEmpty()) return;

        Quote quote = getOrCreateProvisionalQuote(so);
        services.forEach(ms -> quote.getServiceLines().add(buildServiceLine(quote, ms)));
        productItems.forEach(item -> quote.getProductLines()
                .add(buildProductLine(quote, item.product(), item.quantity())));
        quote.setTotalAmount(calculateTotal(quote));
        quoteRepository.save(quote);
    }

    private QuoteServiceLine buildServiceLine(Quote quote, MechanicalService ms) {
        QuoteServiceLine line = new QuoteServiceLine();
        line.setQuote(quote);
        line.setMechanicalService(ms);
        line.setNameSnapshot(ms.getName());
        line.setPriceSnapshot(ms.getBasePrice());
        line.setEstimatedDurationMinutes(ms.getEstimatedDurationMinutes());
        return line;
    }

    private QuoteProductLine buildProductLine(Quote quote, Product product, BigDecimal quantity) {
        QuoteProductLine line = new QuoteProductLine();
        line.setQuote(quote);
        line.setProduct(product);
        line.setNameSnapshot(product.getName());
        line.setUnitPriceSnapshot(product.getUnitPrice());
        line.setQuantity(quantity);
        line.setMeasurementUnit(product.getMeasurementUnit());
        line.setUnbudgeted(false);
        return line;
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

    private record OpeningProductItem(Product product, BigDecimal quantity) {
    }
}
