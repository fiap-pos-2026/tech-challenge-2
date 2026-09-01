package br.com.fiap.pos.tech_challenge.core.application.port.out;

import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.web.dto.ServiceAvgDurationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MechanicalServiceRepository {
    Optional<MechanicalService> findByUuid(UUID uuid);
    Page<MechanicalService> findAll(Pageable pageable);
    boolean existsByIdAndServiceOrders_StatusIn(Long id, Collection<ServiceOrderStatus> statuses);
    // TODO Fase D: substituir ServiceAvgDurationResponse por projecao neutra em application
    List<ServiceAvgDurationResponse> findAvgDurationByService(Collection<ServiceOrderStatus> completedStatuses);
    MechanicalService save(MechanicalService service);
    void deleteById(Long id);
}
