package br.com.fiap.pos.tech_challenge.core.application;

import br.com.fiap.pos.tech_challenge.core.web.dto.CreateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.web.dto.CustomerResponse;
import br.com.fiap.pos.tech_challenge.core.web.dto.UpdateCustomerRequest;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CustomerNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.DuplicateDocumentException;
import br.com.fiap.pos.tech_challenge.core.web.mapper.CustomerMapper;
import br.com.fiap.pos.tech_challenge.core.application.port.out.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final List<ServiceOrderStatus> ACTIVE_STATUSES = Arrays.stream(ServiceOrderStatus.values())
            .filter(s -> !s.isTerminal())
            .toList();

    private final CustomerRepository repository;
    private final CustomerMapper mapper;

    @Transactional
    public CustomerResponse register(CreateCustomerRequest request) {
        if (repository.existsByDocument(request.document())) {
            throw new DuplicateDocumentException();
        }
        Customer entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public CustomerResponse findByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .map(mapper::toResponse)
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public String findFullDocument(UUID uuid) {
        return repository.findByUuid(uuid)
                .map(Customer::getDocument)
                .orElseThrow(CustomerNotFoundException::new);
    }

    public void validateExistence(Long id) {
        if (!repository.existsById(id)) {
            throw new CustomerNotFoundException();
        }
    }

    @Transactional(readOnly = true)
    public CustomerResponse findByDocument(String document) {
        return repository.findByDocument(document)
                .map(mapper::toResponse)
                .orElseThrow(CustomerNotFoundException::new);
    }

    @Transactional
    public CustomerResponse update(UUID uuid, UpdateCustomerRequest request) {
        Customer entity = repository.findByUuid(uuid)
                .orElseThrow(CustomerNotFoundException::new);
        mapper.fullUpdate(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID uuid) {
        Customer entity = repository.findByUuid(uuid)
                .orElseThrow(CustomerNotFoundException::new);
        if (repository.hasActiveServiceOrders(entity.getId(), ACTIVE_STATUSES)) {
            throw new CoreException(EApplicationError.RESOURCE_IN_USE);
        }
        repository.deleteById(entity.getId());
    }

    @Transactional(readOnly = true)
    public Customer findEntityByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .orElseThrow(CustomerNotFoundException::new);
    }
}
