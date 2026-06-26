package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author johncgo
 * @since 2026-06-24
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
