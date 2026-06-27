package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.domain.Product;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.StockMovement;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.MovementType;
import br.com.fiap.pos.tech_challenge.core.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.exception.ReturnNotAllowedException;
import br.com.fiap.pos.tech_challenge.core.repository.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public void debit(UUID productUuid, BigDecimal quantity, ServiceOrder serviceOrder, User user) {
        Product product = productRepository.findByUuidForUpdate(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        if (product.getAvailableQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException();
        }

        product.setAvailableQuantity(product.getAvailableQuantity().subtract(quantity));
        productRepository.save(product);

        recordMovement(product, serviceOrder, user, MovementType.DEBIT, quantity);
    }

    @Transactional
    public void credit(UUID productUuid, BigDecimal quantity, ServiceOrder serviceOrder, User user) {
        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        if (!product.isReturnable()) {
            throw new ReturnNotAllowedException();
        }

        product.setAvailableQuantity(product.getAvailableQuantity().add(quantity));
        productRepository.save(product);

        recordMovement(product, serviceOrder, user, MovementType.CREDIT, quantity);
    }

    @Transactional
    public void replenish(UUID productUuid, BigDecimal quantity, User user) {
        Product product = productRepository.findByUuid(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        product.setAvailableQuantity(product.getAvailableQuantity().add(quantity));
        productRepository.save(product);

        recordMovement(product, null, user, MovementType.REPLENISHMENT, quantity);
    }

    @Transactional
    public void compensate(UUID productUuid, BigDecimal quantity, ServiceOrder serviceOrder, User user) {
        Product product = productRepository.findByUuidForUpdate(productUuid)
                .orElseThrow(ProductNotFoundException::new);

        product.setAvailableQuantity(product.getAvailableQuantity().add(quantity));
        productRepository.save(product);

        recordMovement(product, serviceOrder, user, MovementType.COMPENSATION, quantity);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> listMovementsByProduct(Long productId, Pageable pageable) {
        return stockMovementRepository.findAllByProductId(productId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> listMovementsByServiceOrder(Long serviceOrderId, Pageable pageable) {
        return stockMovementRepository.findAllByServiceOrderId(serviceOrderId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<StockMovement> listAllMovements(Pageable pageable) {
        return stockMovementRepository.findAll(pageable);
    }

    private void recordMovement(Product product, ServiceOrder serviceOrder, User user,
                                MovementType type, BigDecimal quantity) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setServiceOrder(serviceOrder);
        movement.setUser(user);
        movement.setType(type);
        movement.setQuantity(quantity);
        movement.setReferenceUnitPrice(product.getUnitPrice());
        stockMovementRepository.save(movement);
    }
}
