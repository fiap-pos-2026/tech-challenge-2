package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ReturnNotAllowedException;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock ProductRepository productRepository;
    @Mock StockMovementRepository stockMovementRepository;

    @InjectMocks
    StockService sut;

    // -------------------------------------------------------------------------
    // debit
    // -------------------------------------------------------------------------
    @Test
    void debit_reducesAvailableQuantityAndRecordsMovement() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, new BigDecimal("10.00"), BigDecimal.TEN);

        when(productRepository.findByUuidForUpdate(productUuid)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(stockMovementRepository.save(any())).thenReturn(null);

        sut.debit(productUuid, new BigDecimal("3.00"), new ServiceOrder(), null);

        assertThat(product.getAvailableQuantity()).isEqualByComparingTo("7.00");
        verify(stockMovementRepository).save(any());
    }

    @Test
    void debit_throwsWhenStockInsufficient() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, new BigDecimal("2.00"), BigDecimal.TEN);

        when(productRepository.findByUuidForUpdate(productUuid)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> sut.debit(productUuid, new BigDecimal("5.00"), new ServiceOrder(), null))
                .isInstanceOf(InsufficientStockException.class);

        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void debit_throwsWhenProductNotFound() {
        UUID productUuid = UUID.randomUUID();
        when(productRepository.findByUuidForUpdate(productUuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.debit(productUuid, BigDecimal.ONE, new ServiceOrder(), null))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // credit
    // -------------------------------------------------------------------------
    @Test
    void credit_increasesAvailableQuantityAndRecordsMovement() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, new BigDecimal("5.00"), BigDecimal.TEN);
        product.setReturnable(true);

        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(stockMovementRepository.save(any())).thenReturn(null);

        sut.credit(productUuid, new BigDecimal("2.00"), new ServiceOrder(), null);

        assertThat(product.getAvailableQuantity()).isEqualByComparingTo("7.00");
        verify(stockMovementRepository).save(any());
    }

    @Test
    void credit_throwsWhenProductNotReturnable() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, BigDecimal.TEN, BigDecimal.TEN);
        product.setReturnable(false);

        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> sut.credit(productUuid, BigDecimal.ONE, new ServiceOrder(), null))
                .isInstanceOf(ReturnNotAllowedException.class);
    }

    // -------------------------------------------------------------------------
    // compensate
    // -------------------------------------------------------------------------
    @Test
    void compensate_increasesStockWithoutReturnabilityCheck() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, new BigDecimal("3.00"), BigDecimal.TEN);
        product.setReturnable(false);

        when(productRepository.findByUuidForUpdate(productUuid)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(stockMovementRepository.save(any())).thenReturn(null);

        sut.compensate(productUuid, new BigDecimal("2.00"), new ServiceOrder(), null);

        assertThat(product.getAvailableQuantity()).isEqualByComparingTo("5.00");
    }

    // -------------------------------------------------------------------------
    // replenish
    // -------------------------------------------------------------------------
    @Test
    void replenish_increasesStockAndRecordsMovement() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, new BigDecimal("10.00"), BigDecimal.TEN);

        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);
        when(stockMovementRepository.save(any())).thenReturn(null);

        sut.replenish(productUuid, new BigDecimal("5.00"), null);

        assertThat(product.getAvailableQuantity()).isEqualByComparingTo("15.00");
        verify(stockMovementRepository).save(any());
    }

    // -------------------------------------------------------------------------
    // listMovements*
    // -------------------------------------------------------------------------
    @Test
    void listMovementsByProduct_delegatesToRepository() {
        when(stockMovementRepository.findAllByProductId(eq(1L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        sut.listMovementsByProduct(1L, org.springframework.data.domain.Pageable.unpaged());

        verify(stockMovementRepository).findAllByProductId(eq(1L), any());
    }

    @Test
    void listMovementsByServiceOrder_delegatesToRepository() {
        when(stockMovementRepository.findAllByServiceOrderId(eq(2L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        sut.listMovementsByServiceOrder(2L, org.springframework.data.domain.Pageable.unpaged());

        verify(stockMovementRepository).findAllByServiceOrderId(eq(2L), any());
    }

    @Test
    void listAllMovements_delegatesToRepository() {
        when(stockMovementRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of()));

        sut.listAllMovements(org.springframework.data.domain.Pageable.unpaged());

        verify(stockMovementRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void registerManualAdjustment_savesMovementWithNotes() {
        UUID productUuid = UUID.randomUUID();
        Product product = productWithStock(productUuid, BigDecimal.TEN, BigDecimal.TEN);

        when(productRepository.findByUuid(productUuid)).thenReturn(Optional.of(product));
        when(stockMovementRepository.save(any())).thenReturn(null);

        sut.registerManualAdjustment(productUuid, new BigDecimal("5"), "ajuste manual", null);

        verify(stockMovementRepository).save(argThat(m ->
                m.getNotes().equals("ajuste manual") &&
                m.getType() == br.com.fiap.pos.tech_challenge.core.domain.enums.MovementType.MANUAL_ADJUSTMENT));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private Product productWithStock(UUID uuid, BigDecimal stock, BigDecimal unitPrice) {
        Product p = new Product();
        p.setUuid(uuid);
        p.setAvailableQuantity(stock);
        p.setUnitPrice(unitPrice);
        return p;
    }
}
