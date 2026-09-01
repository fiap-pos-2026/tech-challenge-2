package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateProductRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.ProductResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.enums.MeasurementUnit;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ProductType;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.web.mapper.ProductMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository repository;
    @Mock ProductMapper mapper;

    @InjectMocks ProductService sut;

    @Test
    void create_savesAndReturnsResponse() {
        CreateProductRequest req = productRequest();
        Product entity = new Product();
        ProductResponse resp = productResponse();

        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.create(req)).isEqualTo(resp);
        verify(repository).save(entity);
    }

    @Test
    void update_appliesChangesAndReturns() {
        UUID uuid = UUID.randomUUID();
        Product entity = new Product();
        ProductResponse resp = productResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.update(uuid, productRequest())).isEqualTo(resp);
        verify(mapper).fullUpdate(any(), eq(entity));
    }

    @Test
    void update_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(uuid, productRequest()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void delete_removesWhenNotInActiveOrder() {
        UUID uuid = UUID.randomUUID();
        Product entity = new Product();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.existsByIdAndServiceOrders_StatusIn(eq(1L), any())).thenReturn(false);

        sut.delete(uuid);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenInActiveOrder() {
        UUID uuid = UUID.randomUUID();
        Product entity = new Product();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.existsByIdAndServiceOrders_StatusIn(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void delete_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findAll_returnsMappedPage() {
        Product entity = new Product();
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toResponse(entity)).thenReturn(productResponse());

        assertThat(sut.findAll(Pageable.unpaged()).getContent()).hasSize(1);
    }

    @Test
    void findByUuid_returnsResponse() {
        UUID uuid = UUID.randomUUID();
        Product entity = new Product();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(productResponse());

        assertThat(sut.findByUuid(uuid)).isNotNull();
    }

    @Test
    void findByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByUuid(uuid))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findEntityByUuid_returnsEntity() {
        UUID uuid = UUID.randomUUID();
        Product entity = new Product();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));

        assertThat(sut.findEntityByUuid(uuid)).isSameAs(entity);
    }

    @Test
    void findEntityByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findEntityByUuid(uuid))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private CreateProductRequest productRequest() {
        return new CreateProductRequest("Óleo 5W30", null, ProductType.SUPPLY, MeasurementUnit.LITER,
                new BigDecimal("45.00"), new BigDecimal("100"), true);
    }

    private ProductResponse productResponse() {
        return new ProductResponse(UUID.randomUUID(), "Óleo 5W30", ProductType.SUPPLY,
                MeasurementUnit.LITER, new BigDecimal("45.00"), new BigDecimal("100"), true);
    }
}
