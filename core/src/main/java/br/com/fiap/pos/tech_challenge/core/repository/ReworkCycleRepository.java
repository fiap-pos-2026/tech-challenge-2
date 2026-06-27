package br.com.fiap.pos.tech_challenge.core.repository;

import br.com.fiap.pos.tech_challenge.core.domain.ReworkCycle;
import br.com.fiap.pos.tech_challenge.core.domain.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Repository
public interface ReworkCycleRepository extends JpaRepository<ReworkCycle, Long> {

    List<ReworkCycle> findByServiceOrder(ServiceOrder serviceOrder);
}
