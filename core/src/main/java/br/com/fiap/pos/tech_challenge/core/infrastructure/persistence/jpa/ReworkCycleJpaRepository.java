package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.jpa;

import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.ReworkCycleEntity;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.ServiceOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author johncgo
 * @since 2026-06-26
 */
@Repository
public interface ReworkCycleJpaRepository extends JpaRepository<ReworkCycleEntity, Long> {

    List<ReworkCycleEntity> findByServiceOrder(ServiceOrderEntity serviceOrder);
}
