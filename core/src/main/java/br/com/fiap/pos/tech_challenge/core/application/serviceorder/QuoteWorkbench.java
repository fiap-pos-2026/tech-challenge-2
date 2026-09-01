package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.port.out.QuoteRepository;
import br.com.fiap.pos.tech_challenge.core.domain.exception.QuoteNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteProductLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.QuoteServiceLine;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class QuoteWorkbench {

    private final QuoteRepository quoteRepository;

    Quote getOrCreateProvisional(ServiceOrder so) {
        return quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId())
                .orElseGet(() -> {
                    Quote q = new Quote();
                    q.setServiceOrder(so);
                    q.setTotalAmount(BigDecimal.ZERO);
                    return quoteRepository.save(q);
                });
    }

    Optional<Quote> findLatest(ServiceOrder so) {
        return quoteRepository.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId());
    }

    Quote latestOrThrow(ServiceOrder so) {
        return findLatest(so).orElseThrow(QuoteNotFoundException::new);
    }

    Quote save(Quote quote) {
        return quoteRepository.save(quote);
    }

    BigDecimal recalcTotal(Quote quote) {
        BigDecimal services = quote.getServiceLines().stream()
                .map(QuoteServiceLine::getPriceSnapshot)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal products = quote.getProductLines().stream()
                .map(l -> l.getUnitPriceSnapshot().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return services.add(products);
    }

    QuoteServiceLine buildServiceLine(Quote quote, MechanicalService ms) {
        QuoteServiceLine line = new QuoteServiceLine();
        line.setQuote(quote);
        line.setMechanicalService(ms);
        line.setNameSnapshot(ms.getName());
        line.setPriceSnapshot(ms.getBasePrice());
        line.setEstimatedDurationMinutes(ms.getEstimatedDurationMinutes());
        return line;
    }

    QuoteProductLine buildProductLine(Quote quote, Product product, BigDecimal quantity) {
        return productLine(quote, product, quantity, false);
    }

    QuoteProductLine buildUnbudgetedProductLine(Quote quote, Product product, BigDecimal quantity) {
        return productLine(quote, product, quantity, true);
    }

    private QuoteProductLine productLine(Quote quote, Product product, BigDecimal quantity, boolean unbudgeted) {
        QuoteProductLine line = new QuoteProductLine();
        line.setQuote(quote);
        line.setProduct(product);
        line.setNameSnapshot(product.getName());
        line.setUnitPriceSnapshot(product.getUnitPrice());
        line.setQuantity(quantity);
        line.setMeasurementUnit(product.getMeasurementUnit());
        line.setUnbudgeted(unbudgeted);
        return line;
    }
}
