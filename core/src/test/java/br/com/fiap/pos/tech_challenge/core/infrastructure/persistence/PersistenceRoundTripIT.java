package br.com.fiap.pos.tech_challenge.core.infrastructure.persistence;

import br.com.fiap.pos.tech_challenge.core.application.port.out.*;
import br.com.fiap.pos.tech_challenge.core.domain.enums.*;
import br.com.fiap.pos.tech_challenge.core.domain.model.*;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.ReworkCycleEntity;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.entity.SecurityAuditLogEntity;
import br.com.fiap.pos.tech_challenge.core.infrastructure.persistence.mapper.PersistenceMapper;
import br.com.fiap.pos.tech_challenge.core.integration.BaseIntegrationTest;
import br.com.fiap.pos.tech_challenge.core.web.dto.ServiceAvgDurationResponse;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PersistenceRoundTripIT extends BaseIntegrationTest {

    @MockitoBean JavaMailSender mailSender;

    @Autowired CustomerRepository customers;
    @Autowired VehicleRepository vehicles;
    @Autowired ProductRepository products;
    @Autowired MechanicalServiceRepository services;
    @Autowired UserRepository users;
    @Autowired SecurityAuditLogRepository auditLogs;
    @Autowired NotificationRepository notifications;
    @Autowired OTPTokenRepository otpTokens;
    @Autowired StockMovementRepository stockMovements;
    @Autowired ReworkCycleRepository reworkCycles;
    @Autowired ServiceOrderRepository serviceOrders;
    @Autowired QuoteRepository quotes;
    @Autowired PersistenceMapper mapper;
    @Autowired EntityManager em;

    private static final List<ServiceOrderStatus> ACTIVE =
            Arrays.stream(ServiceOrderStatus.values()).filter(s -> !s.isTerminal()).toList();
    private static final List<ServiceOrderStatus> COMPLETED_STATUSES =
            List.of(ServiceOrderStatus.COMPLETED, ServiceOrderStatus.DELIVERED);
    private static final LocalDateTime TS = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    private static final Instant INSTANT = Instant.now().truncatedTo(ChronoUnit.MICROS);

    private RecursiveComparisonConfiguration cfg(String... ignoredFields) {
        var c = new RecursiveComparisonConfiguration();
        c.ignoreFieldsMatchingRegexes(".*version");                 // @Version incrementa na escrita
        c.registerEqualsForType((a, b) -> a.compareTo(b) == 0, BigDecimal.class);
        c.ignoreCollectionOrder(true);
        for (String f : ignoredFields) c.ignoreFields(f);
        return c;
    }

    private void flushClear() {
        em.flush();
        em.clear();
    }

    @Test
    void customer_roundTripsAllFields() {
        Customer saved = customers.save(newCustomer());
        flushClear();
        Customer reloaded = customers.findByUuid(saved.getUuid()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void vehicle_roundTripsAllFieldsIncludingCustomerFk() {
        Customer customer = customers.save(newCustomer());
        Vehicle saved = vehicles.save(newVehicle(customer));
        flushClear();
        Vehicle reloaded = vehicles.findByUuid(saved.getUuid()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
        assertThat(reloaded.getCustomer().getId()).isEqualTo(customer.getId());
    }

    @Test
    void product_roundTripsAllFields() {
        Product saved = products.save(newProduct());
        flushClear();
        Product reloaded = products.findByUuid(saved.getUuid()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void mechanicalService_roundTripsAllFields() {
        MechanicalService saved = services.save(newService());
        flushClear();
        MechanicalService reloaded = services.findByUuid(saved.getUuid()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void user_roundTripsAllFields() {
        User saved = users.save(newUser("rt_user"));
        flushClear();
        User reloaded = users.findByUuid(saved.getUuid()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void securityAuditLog_roundTripsAllFields() {
        User actor = users.save(newUser("rt_audit"));
        SecurityAuditLog log = newAuditLog(actor);
        SecurityAuditLog saved = auditLogs.save(log);
        flushClear();
        SecurityAuditLog reloaded = mapper.toDomain(em.find(SecurityAuditLogEntity.class, saved.getId()));
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void notification_roundTripsAllFields() {
        User target = users.save(newUser("rt_notif"));
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.RECEIVED);
        Notification saved = notifications.save(newNotification(target, so));
        flushClear();
        Notification reloaded = notifications.findByUuidAndUserId(saved.getUuid(), target.getId()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void otpToken_roundTripsAllFields() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        OTPToken saved = otpTokens.save(newOtpToken(so));
        flushClear();
        OTPToken reloaded = otpTokens
                .findFirstByServiceOrderIdAndUsedFalseAndInvalidatedAtIsNullAndExpiresAtAfter(
                        so.getId(), Instant.now())
                .orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void stockMovement_roundTripsAllFields() {
        Product product = products.save(newProduct());
        User actor = users.save(newUser("rt_stock"));
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.IN_PROGRESS);
        StockMovement saved = stockMovements.save(newStockMovement(product, so, actor));
        flushClear();
        StockMovement reloaded = stockMovements.findById(saved.getId()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void reworkCycle_roundTripsAllFields() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.IN_PROGRESS);
        ReworkCycle saved = reworkCycles.save(newReworkCycle(so));
        flushClear();
        ReworkCycle reloaded = mapper.toDomain(em.find(ReworkCycleEntity.class, saved.getId()));
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void serviceOrder_roundTripsAllFieldsIncludingRelations() {
        ServiceOrder saved = seedServiceOrder(ServiceOrderStatus.IN_DIAGNOSIS);
        flushClear();
        ServiceOrder reloaded = serviceOrders.findByUuid(saved.getUuid()).orElseThrow();
        assertThat(reloaded).usingRecursiveComparison(cfg()).isEqualTo(saved);
    }

    @Test
    void quote_roundTripsGraphWithLines() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        Product product = products.save(newProduct());
        MechanicalService service = services.save(newService());

        Quote quote = new Quote();
        quote.setUuid(UUID.randomUUID());
        quote.setCreatedAt(TS);
        quote.setServiceOrder(so);
        quote.setTotalAmount(new BigDecimal("150.00"));
        quote.setApprovedAt(INSTANT);
        quote.getProductLines().add(productLine(quote, product));
        quote.getServiceLines().add(serviceLine(quote, service));

        Quote saved = quotes.save(quote);
        flushClear();
        Quote reloaded = quotes.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId()).orElseThrow();

        assertThat(reloaded).usingRecursiveComparison(
                        cfg("productLines.quote", "serviceLines.quote"))
                .isEqualTo(saved);
        assertThat(reloaded.getProductLines()).hasSize(1);
        assertThat(reloaded.getServiceLines()).hasSize(1);
    }

    @Test
    void notification_listAndCleanupQueries_run() {
        User target = users.save(newUser("rt_notif_q"));
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.RECEIVED);
        notifications.save(newNotification(target, so));
        flushClear();

        assertThat(notifications.findByUserIdOrderByCreatedAtDesc(target.getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .hasSize(1);
        notifications.deleteByReadTrueAndReadAtBefore(Instant.now().plusSeconds(60));
        flushClear();
        assertThat(notifications.findByUserIdOrderByCreatedAtDesc(target.getId(), org.springframework.data.domain.PageRequest.of(0, 10)))
                .isEmpty();
    }

    @Test
    void adapter_deleteById_removesRow() {
        Customer saved = customers.save(newCustomer());
        flushClear();
        customers.deleteById(saved.getId());
        flushClear();
        assertThat(customers.findByUuid(saved.getUuid())).isEmpty();
    }

    @Test
    void adapters_secondaryFindersAndDeletes_areExercised() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        Customer c = customers.save(newCustomer());
        Vehicle v = vehicles.save(newVehicle(c));
        Product p = products.save(newProduct());
        MechanicalService s = services.save(newService());
        User u = users.save(newUser("rt_finders"));
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.RECEIVED);
        flushClear();

        assertThat(customers.findByDocument(c.getDocument())).isPresent();
        assertThat(customers.existsByDocument(c.getDocument())).isTrue();
        assertThat(customers.existsById(c.getId())).isTrue();

        assertThat(vehicles.findByLicensePlate(v.getLicensePlate())).isPresent();
        assertThat(vehicles.existsByLicensePlate(v.getLicensePlate())).isTrue();
        assertThat(vehicles.existsById(v.getId())).isTrue();

        assertThat(products.findByUuidForUpdate(p.getUuid())).isPresent();
        assertThat(products.findAll(pageable).getTotalElements()).isPositive();
        assertThat(services.findAll(pageable).getTotalElements()).isPositive();

        assertThat(stockMovements.findAll(pageable)).isNotNull();
        assertThat(stockMovements.findAllByProductId(p.getId(), pageable)).isNotNull();
        assertThat(stockMovements.findAllByServiceOrderId(so.getId(), pageable)).isNotNull();
        assertThat(quotes.findByServiceOrderId(so.getId())).isEmpty();

        assertThat(users.findById(u.getId())).isPresent();
        assertThat(users.findAll()).isNotEmpty();
        assertThat(users.findByRole(UserRole.ATTENDANT)).isNotEmpty();
        assertThat(users.existsByLoginIgnoreCase(u.getLogin())).isTrue();
        assertThat(users.existsByEmailIgnoreCase(u.getEmail())).isTrue();
        assertThat(users.countByRole(UserRole.ATTENDANT)).isPositive();
        users.updateLastLogin(u.getId());

        vehicles.deleteById(v.getId());
        products.deleteById(p.getId());
        services.deleteById(s.getId());
        flushClear();
        assertThat(vehicles.findByUuid(v.getUuid())).isEmpty();
    }

    @Test
    void customer_hasActiveServiceOrders_runsRewrittenJpql() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.RECEIVED);
        flushClear();

        assertThat(customers.hasActiveServiceOrders(so.getCustomer().getId(), ACTIVE)).isTrue();
        Customer other = customers.save(newCustomer());
        assertThat(customers.hasActiveServiceOrders(other.getId(), ACTIVE)).isFalse();
    }

    @Test
    void vehicle_hasActiveServiceOrders_runsRewrittenJpql() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.IN_PROGRESS);
        flushClear();

        assertThat(vehicles.hasActiveServiceOrders(so.getVehicle().getId(), ACTIVE)).isTrue();
    }

    @Test
    void product_existsByIdAndServiceOrders_StatusIn_runsRewrittenJpql() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        Product product = products.save(newProduct());

        Quote quote = new Quote();
        quote.setUuid(UUID.randomUUID());
        quote.setCreatedAt(TS);
        quote.setServiceOrder(so);
        quote.setTotalAmount(new BigDecimal("10.00"));
        quote.getProductLines().add(productLine(quote, product));
        quotes.save(quote);
        flushClear();

        assertThat(products.existsByIdAndServiceOrders_StatusIn(product.getId(), ACTIVE)).isTrue();
    }

    @Test
    void mechanicalService_avgDurationAndExistsQueries_runRewrittenJpql() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.COMPLETED);
        MechanicalService service = services.save(newService());

        Quote quote = new Quote();
        quote.setUuid(UUID.randomUUID());
        quote.setCreatedAt(TS);
        quote.setServiceOrder(so);
        quote.setTotalAmount(new BigDecimal("80.00"));
        QuoteServiceLine line = serviceLine(quote, service);
        line.setEstimatedDurationMinutes(90);
        quote.getServiceLines().add(line);
        quotes.save(quote);
        flushClear();

        assertThat(services.existsByIdAndServiceOrders_StatusIn(service.getId(), COMPLETED_STATUSES)).isTrue();

        List<ServiceAvgDurationResponse> avg = services.findAvgDurationByService(COMPLETED_STATUSES);
        assertThat(avg).hasSize(1);
        assertThat(avg.get(0).mechanicalServiceUuid()).isEqualTo(service.getUuid());
        assertThat(avg.get(0).avgEstimatedMinutes()).isEqualTo(90.0);
        assertThat(avg.get(0).executionCount()).isEqualTo(1L);
    }

    @Test
    void quote_save_removesOrphanLinesOnMerge() {
        ServiceOrder so = seedServiceOrder(ServiceOrderStatus.AWAITING_APPROVAL);
        Product p1 = products.save(newProduct());
        Product p2 = products.save(newProduct());

        Quote quote = new Quote();
        quote.setUuid(UUID.randomUUID());
        quote.setCreatedAt(TS);
        quote.setServiceOrder(so);
        quote.setTotalAmount(new BigDecimal("20.00"));
        quote.getProductLines().add(productLine(quote, p1));
        quote.getProductLines().add(productLine(quote, p2));
        Quote saved = quotes.save(quote);
        flushClear();

        Quote loaded = quotes.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId()).orElseThrow();
        assertThat(loaded.getProductLines()).hasSize(2);

        loaded.getProductLines().removeIf(l -> l.getProduct().getId().equals(p2.getId()));
        quotes.save(loaded);
        flushClear();

        Quote after = quotes.findFirstByServiceOrderIdOrderByCreatedAtDesc(so.getId()).orElseThrow();
        assertThat(after.getProductLines()).hasSize(1);
        Long rows = em.createQuery("select count(l) from QuoteProductLineEntity l", Long.class)
                .getSingleResult();
        assertThat(rows).isEqualTo(1L);
    }

    private static final String[] CPFS = {"52998224725", "11144477735", "39053344705"};
    private int customerSeq;

    private Customer newCustomer() {
        int idx = customerSeq++ % CPFS.length;
        Customer c = new Customer();
        c.setUuid(UUID.randomUUID());
        c.setDocumentType(DocumentType.CPF);
        c.setDocument(CPFS[idx]);
        c.setName("Cliente Round-Trip " + idx);
        c.setEmail("rt.cliente" + idx + "@tech.local");
        c.setPhone("11987654321");
        c.setCreatedAt(TS);
        return c;
    }

    private static final String[] PLATES = {"SMT1P23", "ABC1D23", "XYZ9K88"};
    private int vehicleSeq;

    private Vehicle newVehicle(Customer owner) {
        Vehicle v = new Vehicle();
        v.setUuid(UUID.randomUUID());
        v.setLicensePlate(PLATES[vehicleSeq++ % PLATES.length]);
        v.setMake("Toyota");
        v.setModel("Corolla");
        v.setYear(2022);
        v.setCustomer(owner);
        v.setCreatedAt(TS);
        return v;
    }

    private Product newProduct() {
        Product p = new Product();
        p.setUuid(UUID.randomUUID());
        p.setName("Óleo 5W30");
        p.setDescription("Sintético");
        p.setType(ProductType.SUPPLY);
        p.setMeasurementUnit(MeasurementUnit.LITER);
        p.setUnitPrice(new BigDecimal("45.00"));
        p.setAvailableQuantity(new BigDecimal("10.0000"));
        p.setCreatedAt(TS);
        p.setUpdatedAt(TS);
        p.setReturnable(true);
        return p;
    }

    private MechanicalService newService() {
        MechanicalService s = new MechanicalService();
        s.setUuid(UUID.randomUUID());
        s.setName("Troca de óleo");
        s.setDescription("Inclui filtro");
        s.setBasePrice(new BigDecimal("120.00"));
        s.setEstimatedDurationMinutes(45);
        s.setCreatedAt(TS);
        s.setUpdatedAt(TS);
        return s;
    }

    private User newUser(String login) {
        User u = new User();
        u.setUuid(UUID.randomUUID());
        u.setFirstName("Ana");
        u.setLastName("Souza");
        u.setEmail(login + "@tech.local");
        u.setBirthday(LocalDate.of(1990, 5, 20));
        u.setLogin(login);
        u.setPassword("$2a$hash");
        u.setPhone("11911112222");
        u.setCreatedAt(TS);
        u.setLastLogin(TS);
        u.setHash(UUID.randomUUID().toString());
        u.setRole(UserRole.ATTENDANT);
        u.setActive(true);
        u.setLoginFailedAttempts(2);
        u.setLockedUntil(TS);
        u.setForceChangePassword(true);
        return u;
    }

    private SecurityAuditLog newAuditLog(User actor) {
        SecurityAuditLog l = new SecurityAuditLog();
        l.setUuid(UUID.randomUUID());
        l.setEventTimestamp(INSTANT);
        l.setEventType(AuditEventType.LOGIN_SUCCESS);
        l.setUser(actor);
        l.setAttemptIdentifier("rt_audit");
        l.setResult("SUCCESS");
        l.setDetails("round-trip");
        l.setCreatedAt(TS);
        return l;
    }

    private Notification newNotification(User target, ServiceOrder ref) {
        Notification n = new Notification();
        n.setUuid(UUID.randomUUID());
        n.setUser(target);
        n.setType(NotificationType.ORDER_APPROVED);
        n.setMessage("OS aprovada");
        n.setServiceOrderRef(ref);
        n.setRead(true);
        n.setReadAt(INSTANT);
        n.setCreatedAt(TS);
        return n;
    }

    private OTPToken newOtpToken(ServiceOrder so) {
        OTPToken t = new OTPToken();
        t.setUuid(UUID.randomUUID());
        t.setServiceOrder(so);
        t.setTokenHash("a".repeat(64));
        t.setExpiresAt(INSTANT.plus(1, ChronoUnit.HOURS));
        t.setUsed(false);
        t.setInvalidAttempts(1);
        t.setInvalidatedAt(null);
        t.setCreatedAt(TS);
        return t;
    }

    private StockMovement newStockMovement(Product product, ServiceOrder so, User actor) {
        StockMovement m = new StockMovement();
        m.setUuid(UUID.randomUUID());
        m.setProduct(product);
        m.setServiceOrder(so);
        m.setType(MovementType.DEBIT);
        m.setQuantity(new BigDecimal("3.0000"));
        m.setReferenceUnitPrice(new BigDecimal("45.00"));
        m.setUser(actor);
        m.setNotes("baixa round-trip");
        m.setCreatedAt(TS);
        return m;
    }

    private ReworkCycle newReworkCycle(ServiceOrder so) {
        ReworkCycle r = new ReworkCycle();
        r.setUuid(UUID.randomUUID());
        r.setServiceOrder(so);
        r.setRejectionReason("barulho persiste");
        r.setEstimatedDurationMinutes(60);
        r.setEstimatedInternalCost(new BigDecimal("150.00"));
        r.setCreatedAt(TS);
        return r;
    }

    private QuoteProductLine productLine(Quote quote, Product product) {
        QuoteProductLine l = new QuoteProductLine();
        l.setQuote(quote);
        l.setProduct(product);
        l.setNameSnapshot(product.getName());
        l.setUnitPriceSnapshot(new BigDecimal("45.00"));
        l.setQuantity(new BigDecimal("2.0000"));
        l.setMeasurementUnit(MeasurementUnit.LITER);
        l.setUnbudgeted(false);
        return l;
    }

    private QuoteServiceLine serviceLine(Quote quote, MechanicalService service) {
        QuoteServiceLine l = new QuoteServiceLine();
        l.setQuote(quote);
        l.setMechanicalService(service);
        l.setNameSnapshot(service.getName());
        l.setPriceSnapshot(new BigDecimal("120.00"));
        l.setEstimatedDurationMinutes(45);
        return l;
    }

    private ServiceOrder seedServiceOrder(ServiceOrderStatus status) {
        Customer customer = customers.save(newCustomer());
        Vehicle vehicle = vehicles.save(newVehicle(customer));
        ServiceOrder so = new ServiceOrder();
        so.setUuid(UUID.randomUUID());
        so.setStatus(status);
        so.setCustomerComplaint("barulho no motor");
        so.setCustomer(customer);
        so.setVehicle(vehicle);
        so.setApprovalExpiresAt(INSTANT.plus(7, ChronoUnit.DAYS));
        so.setCreatedAt(TS);
        so.setUpdatedAt(TS);
        return serviceOrders.save(so);
    }
}
