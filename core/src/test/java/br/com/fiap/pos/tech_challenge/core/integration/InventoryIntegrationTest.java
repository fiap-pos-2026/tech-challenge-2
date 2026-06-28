package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.domain.Product;
import br.com.fiap.pos.tech_challenge.core.domain.StockMovement;
import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.enums.MovementType;
import br.com.fiap.pos.tech_challenge.core.enums.ProductType;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.exception.InsufficientStockException;
import br.com.fiap.pos.tech_challenge.core.repository.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.repository.StockMovementRepository;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import br.com.fiap.pos.tech_challenge.core.service.StockService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author johncgo
 * @since 2026-06-27
 */
@Transactional
class InventoryIntegrationTest extends BaseIntegrationTest {

    @Autowired StockService stockService;
    @Autowired ProductRepository productRepository;
    @Autowired StockMovementRepository stockMovementRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;

    @MockitoBean JavaMailSender mailSender;

    private Product product;
    private Product nonReturnableProduct;
    private User operator;

    @BeforeEach
    void setUp() {
        stockMovementRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        operator = new User();
        operator.setFirstName("Operador");
        operator.setLastName("Teste");
        operator.setEmail("operador@garage.com");
        operator.setBirthday(LocalDate.of(1990, 1, 1));
        operator.setLogin("operador_inv");
        operator.setPassword("senha-hash");
        operator.setPhone("11999999999");
        operator.setRole(UserRole.ATTENDANT);
        operator = userRepository.save(operator);

        product = new Product();
        product.setName("Óleo Motor 5W30");
        product.setType(ProductType.SUPPLY);
        product.setMeasurementUnit(MeasurementUnit.LITER);
        product.setUnitPrice(new BigDecimal("45.00"));
        product.setAvailableQuantity(new BigDecimal("10.0000"));
        product.setReturnable(true);
        product = productRepository.save(product);

        nonReturnableProduct = new Product();
        nonReturnableProduct.setName("Filtro de Ar");
        nonReturnableProduct.setType(ProductType.PART);
        nonReturnableProduct.setMeasurementUnit(MeasurementUnit.UNIT);
        nonReturnableProduct.setUnitPrice(new BigDecimal("30.00"));
        nonReturnableProduct.setAvailableQuantity(new BigDecimal("5.0000"));
        nonReturnableProduct.setReturnable(false);
        nonReturnableProduct = productRepository.save(nonReturnableProduct);
    }

    @Test
    void debit_reducesAvailableQuantityAndCreatesMovement() {
        BigDecimal quantity = new BigDecimal("3.0000");

        stockService.debit(product.getUuid(), quantity, null, operator);

        Product updated = productRepository.findByUuid(product.getUuid()).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("7.0000"));

        var movements = stockMovementRepository.findAllByProductId(product.getId(), PageRequest.of(0, 10));
        assertThat(movements.getTotalElements()).isEqualTo(1);
        assertThat(movements.getContent().get(0).getType()).isEqualTo(MovementType.DEBIT);
        assertThat(movements.getContent().get(0).getQuantity()).isEqualByComparingTo(quantity);
    }

    @Test
    void debit_throwsInsufficientStockException_whenQuantityExceedsAvailable() {
        BigDecimal excessQuantity = new BigDecimal("20.0000");

        assertThatThrownBy(() -> stockService.debit(product.getUuid(), excessQuantity, null, operator))
                .isInstanceOf(InsufficientStockException.class);

        Product unchanged = productRepository.findByUuid(product.getUuid()).orElseThrow();
        assertThat(unchanged.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("10.0000"));
    }

    @Test
    void debit_throwsInsufficientStockException_whenStockIsZero() {
        stockService.debit(product.getUuid(), new BigDecimal("10.0000"), null, operator);

        assertThatThrownBy(() -> stockService.debit(product.getUuid(), new BigDecimal("0.0001"), null, operator))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void replenishment_increasesAvailableQuantityAndCreatesMovement() {
        BigDecimal quantity = new BigDecimal("5.0000");

        stockService.replenish(product.getUuid(), quantity, operator);

        Product updated = productRepository.findByUuid(product.getUuid()).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("15.0000"));

        var movements = stockMovementRepository.findAllByProductId(product.getId(), PageRequest.of(0, 10));
        assertThat(movements.getTotalElements()).isEqualTo(1);
        StockMovement movement = movements.getContent().get(0);
        assertThat(movement.getType()).isEqualTo(MovementType.REPLENISHMENT);
        assertThat(movement.getQuantity()).isEqualByComparingTo(quantity);
        assertThat(movement.getServiceOrder()).isNull();
    }

    @Test
    void compensation_increasesQuantityAndCreatesCompensationMovement_withoutCheckingReturnable() {
        BigDecimal quantity = new BigDecimal("2.0000");

        // nonReturnableProduct.returnable == false, but compensate() must NOT check returnable
        stockService.compensate(nonReturnableProduct.getUuid(), quantity, null, operator);

        Product updated = productRepository.findByUuid(nonReturnableProduct.getUuid()).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualByComparingTo(new BigDecimal("7.0000"));

        var movements = stockMovementRepository.findAllByProductId(nonReturnableProduct.getId(), PageRequest.of(0, 10));
        assertThat(movements.getTotalElements()).isEqualTo(1);
        assertThat(movements.getContent().get(0).getType()).isEqualTo(MovementType.COMPENSATION);
    }

    @Test
    void stockMovement_isImmutable_updatesAreIgnoredByJpa() {
        stockService.debit(product.getUuid(), new BigDecimal("1.0000"), null, operator);

        StockMovement movement = stockMovementRepository
                .findAllByProductId(product.getId(), PageRequest.of(0, 10))
                .getContent().get(0);

        BigDecimal originalQuantity = movement.getQuantity();
        MovementType originalType = movement.getType();

        // Attempt to mutate and persist — JPA must ignore updatable=false columns
        movement.setQuantity(new BigDecimal("999.0000"));
        movement.setType(MovementType.CREDIT);
        stockMovementRepository.saveAndFlush(movement);

        // Clear L1 cache so the next load hits the database, not the in-memory entity
        entityManager.clear();

        StockMovement reloaded = stockMovementRepository.findById(movement.getId()).orElseThrow();
        assertThat(reloaded.getQuantity()).isEqualByComparingTo(originalQuantity);
        assertThat(reloaded.getType()).isEqualTo(originalType);
    }
}
