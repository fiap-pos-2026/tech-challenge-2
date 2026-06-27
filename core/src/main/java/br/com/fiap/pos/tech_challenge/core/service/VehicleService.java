package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.UpdateVehicleRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.VehicleResponse;
import br.com.fiap.pos.tech_challenge.core.domain.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.Vehicle;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.exception.DuplicateLicensePlateException;
import br.com.fiap.pos.tech_challenge.core.exception.VehicleNotFoundException;
import br.com.fiap.pos.tech_challenge.core.mapper.VehicleMapper;
import br.com.fiap.pos.tech_challenge.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private static final List<ServiceOrderStatus> ACTIVE_STATUSES = Arrays.stream(ServiceOrderStatus.values())
            .filter(s -> !s.isTerminal())
            .toList();

    private final VehicleRepository repository;
    private final VehicleMapper mapper;
    private final CustomerService customerService;

    @Transactional
    public VehicleResponse register(CreateVehicleRequest request) {
        if (repository.existsByLicensePlate(request.licensePlate())) {
            throw new DuplicateLicensePlateException();
        }
        Customer customer = customerService.findEntityByUuid(request.customerUuid());
        Vehicle entity = mapper.toEntity(request);
        entity.setCustomer(customer);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public VehicleResponse findByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .map(mapper::toResponse)
                .orElseThrow(VehicleNotFoundException::new);
    }

    public void validateExistence(Long id) {
        if (!repository.existsById(id)) {
            throw new VehicleNotFoundException();
        }
    }

    @Transactional(readOnly = true)
    public VehicleResponse findByLicensePlate(String licensePlate) {
        return repository.findByLicensePlate(licensePlate)
                .map(mapper::toResponse)
                .orElseThrow(VehicleNotFoundException::new);
    }

    @Transactional
    public VehicleResponse update(UUID uuid, UpdateVehicleRequest request) {
        Vehicle entity = repository.findByUuid(uuid)
                .orElseThrow(VehicleNotFoundException::new);
        mapper.fullUpdate(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID uuid) {
        Vehicle entity = repository.findByUuid(uuid)
                .orElseThrow(VehicleNotFoundException::new);
        if (repository.hasActiveServiceOrders(entity.getId(), ACTIVE_STATUSES)) {
            throw new CoreException(EApplicationError.RESOURCE_IN_USE);
        }
        repository.deleteById(entity.getId());
    }

    @Transactional(readOnly = true)
    public Vehicle findEntityByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .orElseThrow(VehicleNotFoundException::new);
    }
}
