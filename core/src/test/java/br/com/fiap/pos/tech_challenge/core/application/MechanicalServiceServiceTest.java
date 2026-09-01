package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.application.dto.CreateServiceRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.MechanicalServiceResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.application.mapper.MechanicalServiceMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
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
import static org.mockito.Mockito.*;

/**
 * @author pauloogsouza
 * @since 2026-06-27
 */
@ExtendWith(MockitoExtension.class)
class MechanicalServiceServiceTest {

    @Mock MechanicalServiceRepository repository;
    @Mock MechanicalServiceMapper mapper;

    @InjectMocks MechanicalServiceService sut;

    @Test
    void create_savesAndReturnsResponse() {
        CreateServiceRequest req = new CreateServiceRequest("Troca de óleo", null, new BigDecimal("150.00"), 60);
        MechanicalService entity = new MechanicalService();
        MechanicalServiceResponse resp = serviceResponse();

        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.create(req)).isEqualTo(resp);
        verify(repository).save(entity);
    }

    @Test
    void update_appliesChangesAndReturns() {
        UUID uuid = UUID.randomUUID();
        CreateServiceRequest req = new CreateServiceRequest("Alinhamento", null, new BigDecimal("200.00"), 90);
        MechanicalService entity = new MechanicalService();
        MechanicalServiceResponse resp = serviceResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.update(uuid, req)).isEqualTo(resp);
        verify(mapper).fullUpdate(req, entity);
    }

    @Test
    void update_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(uuid, new CreateServiceRequest("x", null, BigDecimal.TEN, 30)))
                .isInstanceOf(MechanicalServiceNotFoundException.class);
    }

    @Test
    void delete_removesWhenNotInActiveOrder() {
        UUID uuid = UUID.randomUUID();
        MechanicalService entity = new MechanicalService();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.existsByIdAndServiceOrders_StatusIn(eq(1L), any())).thenReturn(false);

        sut.delete(uuid);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenInActiveOrder() {
        UUID uuid = UUID.randomUUID();
        MechanicalService entity = new MechanicalService();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.existsByIdAndServiceOrders_StatusIn(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(CoreException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(MechanicalServiceNotFoundException.class);
    }

    @Test
    void findAll_returnsMappedPage() {
        MechanicalService entity = new MechanicalService();
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(mapper.toResponse(entity)).thenReturn(serviceResponse());

        var page = sut.findAll(Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void findByUuid_returnsResponse() {
        UUID uuid = UUID.randomUUID();
        MechanicalService entity = new MechanicalService();
        MechanicalServiceResponse resp = serviceResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.findByUuid(uuid)).isEqualTo(resp);
    }

    @Test
    void findByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByUuid(uuid))
                .isInstanceOf(MechanicalServiceNotFoundException.class);
    }

    @Test
    void findAvgDurations_delegatesToRepository() {
        when(repository.findAvgDurationByService(any())).thenReturn(List.of());

        var result = sut.findAvgDurations();

        assertThat(result).isEmpty();
        verify(repository).findAvgDurationByService(any());
    }

    private MechanicalServiceResponse serviceResponse() {
        return new MechanicalServiceResponse(UUID.randomUUID(), "Troca de óleo", null, new BigDecimal("150.00"), 60);
    }
}
