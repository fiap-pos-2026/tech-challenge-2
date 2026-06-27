package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateServiceRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.MechanicalServiceResponse;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ServiceAvgDurationResponse;
import br.com.fiap.pos.tech_challenge.core.domain.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.mapper.MechanicalServiceMapper;
import br.com.fiap.pos.tech_challenge.core.repository.MechanicalServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author johncgo
 * @since 2026-06-25
 */
@Service
@RequiredArgsConstructor
public class MechanicalServiceService {

    private final MechanicalServiceRepository repository;
    private final MechanicalServiceMapper mapper;

    private static final List<ServiceOrderStatus> ACTIVE_STATUSES = Arrays.stream(ServiceOrderStatus.values())
            .filter(s -> !s.isTerminal())
            .toList();

    private static final List<ServiceOrderStatus> COMPLETED_STATUSES =
            List.of(ServiceOrderStatus.COMPLETED, ServiceOrderStatus.DELIVERED);

    @Transactional
    public MechanicalServiceResponse create(CreateServiceRequest request) {
        MechanicalService entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public MechanicalServiceResponse update(UUID uuid, CreateServiceRequest request) {
        MechanicalService entity = repository.findByUuid(uuid)
                .orElseThrow(MechanicalServiceNotFoundException::new);
        mapper.fullUpdate(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID uuid) {
        MechanicalService entity = repository.findByUuid(uuid)
                .orElseThrow(MechanicalServiceNotFoundException::new);

        if (repository.existsByIdAndServiceOrders_StatusIn(entity.getId(), ACTIVE_STATUSES)) {
            throw new CoreException(EApplicationError.MECHANICAL_SERVICE_IN_ACTIVE_ORDER);
        }

        repository.deleteById(entity.getId());
    }

    @Transactional(readOnly = true)
    public Page<MechanicalServiceResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public MechanicalServiceResponse findByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .map(mapper::toResponse)
                .orElseThrow(MechanicalServiceNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<ServiceAvgDurationResponse> findAvgDurations() {
        return repository.findAvgDurationByService(COMPLETED_STATUSES);
    }
}
