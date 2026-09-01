package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.AuditLogService;
import br.com.fiap.pos.tech_challenge.core.application.AuthenticationService;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.StockService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.AuditEventType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderExecutionService {

    private final ProductRepository productRepository;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final OTPService otpService;
    private final AuthenticationService authenticationService;
    private final AuditLogService auditLogService;
    private final CurrentActorPort currentActorPort;
    private final QuoteWorkbench quotes;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional
    public ServiceOrderResponse requestProduct(UUID osUuid, UUID productUuid, BigDecimal quantity) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);

        boolean isAddendumPhase = so.getStatus() == ServiceOrderStatus.AWAITING_APPROVAL
                && quotes.latestOrThrow(so).getApprovedAt() != null;

        if (so.getStatus() != ServiceOrderStatus.IN_PROGRESS && !isAddendumPhase) {
            throw new InvalidStatusTransitionException();
        }

        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        User actor = currentActorPort.currentUser().orElse(null);
        try {
            stockService.debit(productUuid, quantity, so, actor);
        } catch (InsufficientStockException e) {
            notificationService.publishInsufficientStockNotification(
                    "Estoque insuficiente do produto " + productUuid + " na OS " + osUuid + ".", so);
            throw e;
        }

        Quote quote = quotes.latestOrThrow(so);
        quote.getProductLines().add(quotes.buildUnbudgetedProductLine(quote, product, quantity));
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);

        if (so.getStatus() == ServiceOrderStatus.IN_PROGRESS) {
            so.setApprovalExpiresAt(ServiceOrderPolicy.approvalDeadlineFromNow());
            so = store.persistStatusChange(so, ServiceOrderStatus.AWAITING_APPROVAL);
            otpService.generateAndSend(so);
        }

        return responseFactory.toResponse(so);
    }

    @Transactional
    public ServiceOrderResponse returnProduct(UUID osUuid, UUID productUuid, String rawPassword) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_PROGRESS);

        User actor = currentActorPort.currentUser()
                .orElseThrow(() -> new CoreException(EApplicationError.NO_AUTHENTICATION));

        authenticationService.validatePassword(actor.getLogin(), rawPassword);

        Quote quote = quotes.latestOrThrow(so);
        QuoteProductLine line = quote.getProductLines().stream()
                .filter(l -> l.getProduct().getUuid().equals(productUuid))
                .findFirst()
                .orElseThrow(ProductNotFoundException::new);

        stockService.credit(productUuid, line.getQuantity(), so, actor);

        quote.getProductLines().remove(line);
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);

        auditLogService.register(AuditEventType.PRODUCT_RETURN, actor,
                actor.getLogin(), "200", "Produto " + productUuid + " devolvido da OS " + osUuid);

        return responseFactory.toResponse(so);
    }

    @Transactional
    public ServiceOrderResponse completeExecution(UUID osUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_PROGRESS);
        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.COMPLETED);
        otpService.generateAndSend(saved);
        notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.EXECUTION_COMPLETED,
                "OS " + osUuid + " concluída pelo mecânico. Cliente notificado para vistoria de entrega.", saved);
        return responseFactory.toResponse(saved);
    }
}
