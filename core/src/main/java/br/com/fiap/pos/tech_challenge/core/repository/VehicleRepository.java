package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.Vehicle;
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
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByUuid(UUID uuid);

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByCustomerId(Long customerId);

    @Query("""
            SELECT CASE WHEN COUNT(so) > 0 THEN true ELSE false END
            FROM ServiceOrder so
            WHERE so.vehicle.id = :vehicleId AND so.status IN :statuses
            """)
    boolean hasActiveServiceOrders(@Param("vehicleId") Long vehicleId,
                                   @Param("statuses") Collection<ServiceOrderStatus> statuses);
}
