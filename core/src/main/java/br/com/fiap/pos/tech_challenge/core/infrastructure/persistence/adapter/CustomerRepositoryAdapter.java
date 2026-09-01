package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.CustomerRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.CustomerJpaRepository;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Transactional
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final CustomerJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<Customer> findByUuid(UUID uuid) { return jpa.findByUuid(uuid).map(mapper::toDomain); }
    public Optional<Customer> findByDocument(String document) { return jpa.findByDocument(document).map(mapper::toDomain); }
    public boolean existsByDocument(String document) { return jpa.existsByDocument(document); }
    public boolean existsById(Long id) { return jpa.existsById(id); }
    public boolean hasActiveServiceOrders(Long customerId, Collection<ServiceOrderStatus> statuses) {
        return jpa.hasActiveServiceOrders(customerId, statuses);
    }
    public Customer save(Customer customer) { return mapper.toDomain(jpa.save(mapper.toEntity(customer))); }
    public void deleteById(Long id) { jpa.deleteById(id); }
    public void deleteAll() { jpa.deleteAll(); }
}
