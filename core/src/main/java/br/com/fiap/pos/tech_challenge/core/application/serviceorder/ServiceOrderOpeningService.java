package br.com.fiap.pos.tech_challenge.core.application.serviceorder;

import br.com.fiap.pos.tech_challenge.core.application.CustomerService;
import br.com.fiap.pos.tech_challenge.core.application.VehicleService;
import br.com.fiap.pos.tech_challenge.core.application.dto.OpenProductItemRequest;
import br.com.fiap.pos.tech_challenge.core.application.dto.ServiceOrderResponse;
import br.com.fiap.pos.tech_challenge.core.application.port.out.MechanicalServiceRepository;
import br.com.fiap.pos.tech_challenge.core.application.port.out.ProductRepository;
import br.com.fiap.pos.tech_challenge.core.domain.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.domain.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.domain.exception.CoreException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.MechanicalServiceNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.domain.model.Customer;
import br.com.fiap.pos.tech_challenge.core.domain.model.MechanicalService;
import br.com.fiap.pos.tech_challenge.core.domain.model.Product;
import br.com.fiap.pos.tech_challenge.core.domain.model.Quote;
import br.com.fiap.pos.tech_challenge.core.domain.model.ServiceOrder;
import br.com.fiap.pos.tech_challenge.core.domain.model.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceOrderOpeningService {

    private final CustomerService customerService;
    private final VehicleService vehicleService;
    private final MechanicalServiceRepository mechanicalServiceRepository;
    private final ProductRepository productRepository;
    private final QuoteWorkbench quotes;
    private final ServiceOrderStore store;
    private final ServiceOrderResponseFactory responseFactory;

    @Transactional
    public ServiceOrderResponse openServiceOrder(UUID customerUuid, UUID vehicleUuid, String customerComplaint) {
        return openServiceOrder(customerUuid, vehicleUuid, customerComplaint, null, null);
    }

    @Transactional
    public ServiceOrderResponse openServiceOrder(UUID customerUuid, UUID vehicleUuid, String customerComplaint,
                                                 List<UUID> mechanicalServiceUuids,
                                                 List<OpenProductItemRequest> products) {
        Customer customer = customerService.findEntityByUuid(customerUuid);
        Vehicle vehicle = vehicleService.findEntityByUuid(vehicleUuid);

        if (!vehicle.getCustomer().getId().equals(customer.getId())) {
            throw new CoreException(EApplicationError.VEHICLE_NOT_OWNED_BY_CUSTOMER);
        }

        List<MechanicalService> services = resolveMechanicalServices(mechanicalServiceUuids);
        List<OpeningProductItem> productItems = resolveProducts(products);

        ServiceOrder so = new ServiceOrder();
        so.setCustomerComplaint(customerComplaint);
        so.setCustomer(customer);
        so.setVehicle(vehicle);

        ServiceOrder saved = store.persistStatusChange(so, ServiceOrderStatus.RECEIVED);
        attachOpeningItems(saved, services, productItems);
        return responseFactory.toResponse(saved);
    }

    private List<MechanicalService> resolveMechanicalServices(List<UUID> mechanicalServiceUuids) {
        if (mechanicalServiceUuids == null) return List.of();
        return mechanicalServiceUuids.stream()
                .map(uuid -> mechanicalServiceRepository.findByUuid(uuid)
                        .orElseThrow(MechanicalServiceNotFoundException::new))
                .toList();
    }

    private List<OpeningProductItem> resolveProducts(List<OpenProductItemRequest> products) {
        if (products == null) return List.of();
        return products.stream()
                .map(item -> new OpeningProductItem(
                        productRepository.findByUuid(item.productUuid())
                                .orElseThrow(ProductNotFoundException::new),
                        item.quantity()))
                .toList();
    }

    private void attachOpeningItems(ServiceOrder so, List<MechanicalService> services,
                                    List<OpeningProductItem> productItems) {
        if (services.isEmpty() && productItems.isEmpty()) return;

        Quote quote = quotes.getOrCreateProvisional(so);
        services.forEach(ms -> quote.getServiceLines().add(quotes.buildServiceLine(quote, ms)));
        productItems.forEach(item -> quote.getProductLines()
                .add(quotes.buildProductLine(quote, item.product(), item.quantity())));
        quote.setTotalAmount(quotes.recalcTotal(quote));
        quotes.save(quote);
    }

    private record OpeningProductItem(Product product, BigDecimal quantity) {
    }
}
