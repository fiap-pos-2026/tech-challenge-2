package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {
    Optional<Customer> findByUuid(UUID uuid);
    Optional<Customer> findByDocument(String document);
    boolean existsByDocument(String document);
    boolean existsById(Long id);
    boolean hasActiveServiceOrders(Long customerId, Collection<ServiceOrderStatus> statuses);
    Customer save(Customer customer);
    void deleteById(Long id);
    void deleteAll();
}
