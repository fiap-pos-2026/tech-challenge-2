package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.StockService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CurrentActorPort;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuoteApprovalService {

    private final OTPService otpService;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final CurrentActorPort currentActorPort;
    private final QuoteWorkbench quotes;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional
    public ServiceOrderResponse approveQuote(UUID osUuid, String customerDocument, String tokenRaw) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.AWAITING_APPROVAL);

        Quote quote = quotes.latestOrThrow(so);
        boolean firstApproval = quote.getApprovedAt() == null;

        if (!firstApproval) {
            quote.getProductLines().stream()
                    .filter(QuoteProductLine::isUnbudgeted)
                    .forEach(l -> l.setUnbudgeted(false));
        }

        quote.setApprovedAt(Instant.now());
        quotes.save(quote);

        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.IN_PROGRESS);

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

        return responseFactory.toResponse(saved);
    }

    @Transactional
    public ServiceOrderResponse rejectQuote(UUID osUuid, String customerDocument, String tokenRaw) {
        otpService.validate(osUuid, customerDocument, tokenRaw);

        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.AWAITING_APPROVAL);

        Quote quote = quotes.latestOrThrow(so);
        List<QuoteProductLine> unbudgeted = quote.getProductLines().stream()
                .filter(QuoteProductLine::isUnbudgeted).toList();

        if (unbudgeted.isEmpty()) {
            ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.CANCELLED);
            notificationService.publishToRole(UserRole.ATTENDANT, NotificationType.QUOTE_REJECTED,
                    "Orçamento da OS " + osUuid + " rejeitado pelo cliente.", saved);
            return responseFactory.toResponse(saved);
        }

        User actor = currentActorPort.currentUser().orElse(null);
        for (QuoteProductLine line : unbudgeted) {
            stockService.compensate(line.getProduct().getUuid(), line.getQuantity(), so, actor);
        }
        quote.getProductLines().removeAll(unbudgeted);
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);

        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.IN_PROGRESS);

        notificationService.publishToRole(UserRole.MECHANIC, NotificationType.ADDENDUM_PRODUCT_REJECTED,
                "Adendo da OS " + osUuid + " rejeitado pelo cliente.", saved);

        return responseFactory.toResponse(saved);
    }

    @Transactional
    public void resendOTP(UUID osUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        if (so.getStatus() != ServiceOrderStatus.AWAITING_APPROVAL
                && so.getStatus() != ServiceOrderStatus.COMPLETED) {
            throw new InvalidStatusTransitionException();
        }
        otpService.invalidateByServiceOrder(so);
        otpService.generateAndSend(so);
    }
}
