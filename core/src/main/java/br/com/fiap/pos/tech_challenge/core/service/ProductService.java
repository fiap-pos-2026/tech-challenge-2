package br.com.fiap.pos.tech_challenge.core.service;

import br.com.fiap.pos.tech_challenge.core.controller.dto.CreateProductRequest;
import br.com.fiap.pos.tech_challenge.core.controller.dto.ProductResponse;
import br.com.fiap.pos.tech_challenge.core.domain.Product;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.mapper.ProductMapper;
import br.com.fiap.pos.tech_challenge.core.repository.ProductRepository;
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
 * @since 2026-06-26
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    private static final List<ServiceOrderStatus> ACTIVE_STATUSES = Arrays.stream(ServiceOrderStatus.values())
            .filter(s -> !s.isTerminal())
            .toList();

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Product entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public ProductResponse update(UUID uuid, CreateProductRequest request) {
        Product entity = repository.findByUuid(uuid)
                .orElseThrow(ProductNotFoundException::new);
        mapper.fullUpdate(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public void delete(UUID uuid) {
        Product entity = repository.findByUuid(uuid)
                .orElseThrow(ProductNotFoundException::new);

        if (repository.existsByIdAndServiceOrders_StatusIn(entity.getId(), ACTIVE_STATUSES)) {
            throw new CoreException(EApplicationError.RESOURCE_IN_USE);
        }

        repository.deleteById(entity.getId());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .map(mapper::toResponse)
                .orElseThrow(ProductNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public Product findEntityByUuid(UUID uuid) {
        return repository.findByUuid(uuid)
                .orElseThrow(ProductNotFoundException::new);
    }
}
