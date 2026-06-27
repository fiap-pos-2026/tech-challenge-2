package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.Customer;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByUuid(UUID uuid);

    Optional<Customer> findByDocument(String document);

    boolean existsByDocument(String document);

    @Query("""
            SELECT CASE WHEN COUNT(so) > 0 THEN true ELSE false END
            FROM ServiceOrder so
            WHERE so.customer.id = :customerId AND so.status IN :statuses
            """)
    boolean hasActiveServiceOrders(@Param("customerId") Long customerId,
                                   @Param("statuses") Collection<ServiceOrderStatus> statuses);
}
