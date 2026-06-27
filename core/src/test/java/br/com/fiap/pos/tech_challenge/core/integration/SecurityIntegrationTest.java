package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.domain.User;
import br.com.fiap.pos.tech_challenge.core.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-26
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers
class SecurityIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("tech_challenge_security_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean JavaMailSender mailSender;

    MockMvc mockMvc;

    private static final String TEST_PASSWORD = "Test@123";
    private String testLogin;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        testLogin = "test.user." + suffix;

        User user = new User();
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test." + suffix + "@tech.com");
        user.setBirthday(LocalDate.of(1990, 1, 1));
        user.setLogin(testLogin);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setPhone("11999999999");
        user.setRole(UserRole.ATTENDANT);
        user.setActive(true);
        userRepository.save(user);
    }

    @Test
    void unauthenticated_getServiceOrders_returns401() throws Exception {
        mockMvc.perform(get("/api/service-orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_getCustomers_returns401() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_getVehicles_returns401() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvalEndpoint_isPublic_returns4xx_withInvalidOtp() throws Exception {
        mockMvc.perform(post("/api/service-orders/" + UUID.randomUUID() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid\",\"customerDocument\":\"12345678901\",\"decision\":\"APPROVE\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void validLogin_returns200WithToken() throws Exception {
        String response = mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + testLogin + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response).contains("tokenValue");
    }

    @Test
    void invalidLogin_returns400() throws Exception {
        mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + testLogin + "\",\"password\":\"WrongPassword@1\"}"))
                .andExpect(status().isBadRequest()); // INVALID_USERNAME_PASSWORD → 400
    }

    @Test
    void inactiveUser_returns401() throws Exception {
        String inactiveSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User inactive = new User();
        inactive.setFirstName("Inactive");
        inactive.setLastName("User");
        inactive.setEmail("inactive." + inactiveSuffix + "@tech.com");
        inactive.setBirthday(LocalDate.of(1985, 6, 15));
        inactive.setLogin("inactive." + inactiveSuffix);
        inactive.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        inactive.setPhone("11988888888");
        inactive.setRole(UserRole.ATTENDANT);
        inactive.setActive(false);
        userRepository.save(inactive);

        mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + inactive.getLogin() + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bruteForce_locksAccountAfter5FailedAttempts() throws Exception {
        String badPayload = "{\"login\":\"" + testLogin + "\",\"password\":\"Wrong@Pass1\"}";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/signin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badPayload))
                    .andExpect(status().isBadRequest()); // INVALID_USERNAME_PASSWORD → 400
        }

        mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isLocked()); // 423 ACCOUNT_LOCKED
    }
}
