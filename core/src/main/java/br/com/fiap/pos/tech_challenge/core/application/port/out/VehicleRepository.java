package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {
    Optional<Vehicle> findByUuid(UUID uuid);
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    boolean existsByLicensePlate(String licensePlate);
    boolean existsById(Long id);
    boolean hasActiveServiceOrders(Long vehicleId, Collection<ServiceOrderStatus> statuses);
    Vehicle save(Vehicle vehicle);
    void deleteById(Long id);
    void deleteAll();
}
