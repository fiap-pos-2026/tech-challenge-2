package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.CustomerResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.enums.DocumentType;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CustomerNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.DuplicateDocumentException;
import br.com.fiap.pos.tech_challenge.core.web.mapper.CustomerMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CustomerServiceTest {

    @Mock CustomerRepository repository;
    @Mock CustomerMapper mapper;

    @InjectMocks CustomerService sut;

    @Test
    void register_savesAndReturnsResponse() {
        CreateCustomerRequest req = new CreateCustomerRequest(DocumentType.CPF, "52998224725", "João", "joao@mail.com", null);
        Customer entity = new Customer();
        CustomerResponse resp = customerResponse();

        when(repository.existsByDocument("52998224725")).thenReturn(false);
        when(mapper.toEntity(req)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        CustomerResponse result = sut.register(req);

        assertThat(result).isEqualTo(resp);
        verify(repository).save(entity);
    }

    @Test
    void register_throwsWhenDocumentDuplicate() {
        CreateCustomerRequest req = new CreateCustomerRequest(DocumentType.CPF, "52998224725", "João", "joao@mail.com", null);
        when(repository.existsByDocument("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> sut.register(req))
                .isInstanceOf(DuplicateDocumentException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findByUuid_returnsResponseWhenFound() {
        UUID uuid = UUID.randomUUID();
        Customer entity = new Customer();
        CustomerResponse resp = customerResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.findByUuid(uuid)).isEqualTo(resp);
    }

    @Test
    void findByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByUuid(uuid))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void findFullDocument_returnsDocument() {
        UUID uuid = UUID.randomUUID();
        Customer entity = new Customer();
        entity.setDocument("52998224725");
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));

        assertThat(sut.findFullDocument(uuid)).isEqualTo("52998224725");
    }

    @Test
    void findFullDocument_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findFullDocument(uuid))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void findByDocument_returnsResponse() {
        Customer entity = new Customer();
        CustomerResponse resp = customerResponse();
        when(repository.findByDocument("52998224725")).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.findByDocument("52998224725")).isEqualTo(resp);
    }

    @Test
    void findByDocument_throwsWhenNotFound() {
        when(repository.findByDocument("52998224725")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findByDocument("52998224725"))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void validateExistence_throwsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> sut.validateExistence(99L))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void validateExistence_doesNothingWhenFound() {
        when(repository.existsById(1L)).thenReturn(true);

        sut.validateExistence(1L); // no exception
    }

    @Test
    void update_appliesChangesAndReturns() {
        UUID uuid = UUID.randomUUID();
        UpdateCustomerRequest req = new UpdateCustomerRequest("Novo Nome", "novo@mail.com", null);
        Customer entity = new Customer();
        CustomerResponse resp = customerResponse();

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(resp);

        assertThat(sut.update(uuid, req)).isEqualTo(resp);
        verify(mapper).fullUpdate(req, entity);
    }

    @Test
    void update_throwsWhenCustomerNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.update(uuid, new UpdateCustomerRequest("x", "x@x.com", null)))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void delete_removesCustomerWhenNoActiveOrders() {
        UUID uuid = UUID.randomUUID();
        Customer entity = new Customer();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.hasActiveServiceOrders(eq(1L), any())).thenReturn(false);

        sut.delete(uuid);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenCustomerHasActiveOrders() {
        UUID uuid = UUID.randomUUID();
        Customer entity = new Customer();
        entity.setId(1L);

        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));
        when(repository.hasActiveServiceOrders(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(CoreException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void delete_throwsWhenCustomerNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.delete(uuid))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void findEntityByUuid_returnsEntity() {
        UUID uuid = UUID.randomUUID();
        Customer entity = new Customer();
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(entity));

        assertThat(sut.findEntityByUuid(uuid)).isSameAs(entity);
    }

    @Test
    void findEntityByUuid_throwsWhenNotFound() {
        UUID uuid = UUID.randomUUID();
        when(repository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findEntityByUuid(uuid))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    private CustomerResponse customerResponse() {
        return new CustomerResponse(UUID.randomUUID(), null, "***", "João", "joao@mail.com", null, null);
    }
}
