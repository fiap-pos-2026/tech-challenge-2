package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.application.dto.CreateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.UpdateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.VehicleResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.DuplicateLicensePlateException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.VehicleNotFoundException;
import br.com.fiap.pos.tech_challenge.core.application.mapper.VehicleMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class VehicleServiceTest {

    @Mock VehicleRepository repository;
    @Mock VehicleMapper mapper;
    @Mock CustomerService customerService;

    @InjectMocks VehicleService sut;

    @Test
    void register_savesAndReturnsResponse() {
        UUID customerUuid = UUID.randomUUID();
        CreateVehicleRequest req = new CreateVehicleRequest("ABC1234", "Toyota", "Corolla", 2022, customerUuid);
        Customer customer = new Customer();
        Vehicle entity = new Vehicle();
        VehicleResponse resp = vehicleResponse();

        when(repository.existsByLicensePlate("ABC1234")).thenReturn(false);
        when(customerService.findEntityByUuid(customerUuid)).thenReturn(customer);
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.register(req)).isEqualTo(resp);
        assertThat(entity.getCustomer()).isSameAs(customer);
    }

    @Test
    void register_throwsWhenLicensePlateDuplicate() {
        UUID customerUuid = UUID.randomUUID();
        CreateVehicleRequest req = new CreateVehicleRequest("ABC1234", "Toyota", "Corolla", 2022, customerUuid);
        when(repository.existsByLicensePlate("ABC1234")).thenReturn(true);

        assertThatThrownBy(() -> sut.register(req))
                .isInstanceOf(DuplicateLicensePlateException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findByUuid_returnsResponseWhenFound() {
        UUID uuid = UUID.randomUUID();
        Vehicle entity = new Vehicle();
        VehicleResponse resp = vehicleResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.findByUuid(uuid)).isEqualTo(resp);
    }

    @Test
    void findByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByUuid(uuid))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void findByLicensePlate_returnsResponse() {
        Vehicle entity = new Vehicle();
        VehicleResponse resp = vehicleResponse();
        when(repository.findByLicensePlate("ABC1234")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.findByLicensePlate("ABC1234")).isEqualTo(resp);
    }

    @Test
    void findByLicensePlate_throwsWhenNotFound() {
        when(repository.findByLicensePlate("XYZ9999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByLicensePlate("XYZ9999"))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void validateExistence_throwsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sut.validateExistence(99L))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void validateExistence_doesNothingWhenFound() {
        when(repository.existsById(1L)).thenReturn(true);

        sut.validateExistence(1L); // no exception expected
    }

    @Test
    void update_appliesChangesAndReturns() {
        UUID uuid = UUID.randomUUID();
        UpdateVehicleRequest req = new UpdateVehicleRequest("Honda", "Civic", 2023);
        Vehicle entity = new Vehicle();
        VehicleResponse resp = vehicleResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.update(uuid, req)).isEqualTo(resp);
        verify(mapper).fullUpdate(req, entity);
    }

    @Test
    void update_throwsWhenVehicleNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(uuid, new UpdateVehicleRequest("x", "y", 2020)))
                .isInstanceOf(VehicleNotFoundException.class);
    }

    @Test
    void delete_removesWhenNoActiveOrders() {
        UUID uuid = UUID.randomUUID();
        Vehicle entity = new Vehicle();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.hasActiveServiceOrders(eq(1L), any())).thenReturn(false);

        sut.delete(uuid);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenVehicleHasActiveOrders() {
        UUID uuid = UUID.randomUUID();
        Vehicle entity = new Vehicle();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.hasActiveServiceOrders(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(CoreException.class);
    }

    @Test
    void findEntityByUuid_returnsEntity() {
        UUID uuid = UUID.randomUUID();
        Vehicle entity = new Vehicle();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));

        assertThat(sut.findEntityByUuid(uuid)).isSameAs(entity);
    }

    private VehicleResponse vehicleResponse() {
        return new VehicleResponse(UUID.randomUUID(), "ABC1234", "Toyota", "Corolla", 2022, UUID.randomUUID());
    }
}
