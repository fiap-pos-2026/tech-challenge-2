package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import br.com.fiap.pos.tech_challenge.core.application.OTPService;
import br.com.fiap.pos.tech_challenge.core.application.StockService;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderDiagnosisService {

    private final MechanicalServiceRepository mechanicalServiceRepository;
    private final ProductRepository productRepository;
    private final StockService stockService;
    private final NotificationService notificationService;
    private final OTPService otpService;
    private final QuoteWorkbench quotes;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional
    public ServiceOrderResponse startDiagnosis(UUID osUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.RECEIVED);
        return responseFactory.toResponse(store.persistStatusChange(so, ServiceOrderStatus.IN_DIAGNOSIS));
    }

    @Transactional
    public ServiceOrderResponse addServiceToDiagnosis(UUID osUuid, UUID mechanicalServiceUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        MechanicalService ms = mechanicalServiceRepository.findByUuid(mechanicalServiceUuid)
                .orElseThrow(MechanicalServiceNotFoundException::new);

        Quote quote = quotes.getOrCreateProvisional(so);
        quote.getServiceLines().add(quotes.buildServiceLine(quote, ms));
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);
        return responseFactory.toResponse(so);
    }

    @Transactional
    public ServiceOrderResponse removeServiceFromDiagnosis(UUID osUuid, UUID mechanicalServiceUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = quotes.getOrCreateProvisional(so);
        boolean removed = quote.getServiceLines().removeIf(
                line -> line.getMechanicalService().getUuid().equals(mechanicalServiceUuid));
        if (!removed) {
            throw new MechanicalServiceNotFoundException();
        }
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);
        return responseFactory.toResponse(so);
    }

    @Transactional
    public ServiceOrderResponse addProductToDiagnosis(UUID osUuid, UUID productUuid, BigDecimal quantity) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        try {
            stockService.checkAvailability(productUuid, quantity);
        } catch (InsufficientStockException e) {
            notificationService.publishInsufficientStockNotification(
                    "Estoque insuficiente do produto " + productUuid + " na OS " + osUuid + ".", so);
            throw e;
        }

        Quote quote = quotes.getOrCreateProvisional(so);
        quote.getProductLines().add(quotes.buildProductLine(quote, product, quantity));
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);
        return responseFactory.toResponse(so);
    }

    @Transactional
    public ServiceOrderResponse removeProductFromDiagnosis(UUID osUuid, UUID productUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = quotes.getOrCreateProvisional(so);
        boolean removed = quote.getProductLines().removeIf(
                line -> line.getProduct().getUuid().equals(productUuid));
        if (!removed) {
            throw new ProductNotFoundException();
        }
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);
        return responseFactory.toResponse(so);
    }

    @Transactional
    public ServiceOrderResponse completeDiagnosis(UUID osUuid) {
        ServiceOrder so = store.findByUuidOrThrow(osUuid);
        store.requireStatus(so, ServiceOrderStatus.IN_DIAGNOSIS);

        Quote quote = quotes.getOrCreateProvisional(so);
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);

        so.setApprovalExpiresAt(ServiceOrderPolicy.approvalDeadlineFromNow());
        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.AWAITING_APPROVAL);

        otpService.generateAndSend(saved);
        return responseFactory.toResponse(saved);
    }
}
