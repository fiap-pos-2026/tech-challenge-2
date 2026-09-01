package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.adapter;

import br.com.fiap.pos.tech_challenge.core.application.port.out.VehicleRepository;
import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa.VehicleJpaRepository;
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
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VehicleJpaRepository jpa;
    private final PersistenceMapper mapper;

    public Optional<Vehicle> findByUuid(UUID uuid) { return jpa.findByUuid(uuid).map(mapper::toDomain); }
    public Optional<Vehicle> findByLicensePlate(String plate) { return jpa.findByLicensePlate(plate).map(mapper::toDomain); }
    public boolean existsByLicensePlate(String plate) { return jpa.existsByLicensePlate(plate); }
    public boolean existsById(Long id) { return jpa.existsById(id); }
    public boolean hasActiveServiceOrders(Long vehicleId, Collection<ServiceOrderStatus> statuses) {
        return jpa.hasActiveServiceOrders(vehicleId, statuses);
    }
    public Vehicle save(Vehicle vehicle) { return mapper.toDomain(jpa.save(mapper.toEntity(vehicle))); }
    public void deleteById(Long id) { jpa.deleteById(id); }
    public void deleteAll() { jpa.deleteAll(); }
}
