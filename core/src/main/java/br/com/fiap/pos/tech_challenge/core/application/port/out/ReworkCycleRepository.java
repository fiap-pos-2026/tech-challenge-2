package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.ReworkCycle;

public interface ReworkCycleRepository {
    ReworkCycle save(ReworkCycle reworkCycle);
}
