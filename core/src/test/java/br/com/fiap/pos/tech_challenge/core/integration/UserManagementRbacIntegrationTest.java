package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.UserRole;
import br.com.fiap.pos.tech_challenge.core.application.port.out.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class UserManagementRbacIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("tech_challenge_rbac_test")
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
    final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean JavaMailSender mailSender;

    MockMvc mockMvc;

    private static final String PASSWORD = "Test@123";

    private UUID adminUuid;
    private UUID attendantUuid;
    private String adminToken;
    private String attendantToken;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        userRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("Test");
        admin.setEmail("admin." + suffix + "@test.com");
        admin.setBirthday(LocalDate.of(1990, 1, 1));
        admin.setLogin("admin." + suffix);
        admin.setPassword(passwordEncoder.encode(PASSWORD));
        admin.setPhone("11900000001");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        adminUuid = userRepository.save(admin).getUuid();

        User attendant = new User();
        attendant.setFirstName("Attendant");
        attendant.setLastName("Test");
        attendant.setEmail("attendant." + suffix + "@test.com");
        attendant.setBirthday(LocalDate.of(1992, 3, 15));
        attendant.setLogin("attendant." + suffix);
        attendant.setPassword(passwordEncoder.encode(PASSWORD));
        attendant.setPhone("11900000002");
        attendant.setRole(UserRole.ATTENDANT);
        attendant.setActive(true);
        attendantUuid = userRepository.save(attendant).getUuid();

        adminToken = login(admin.getLogin());
        attendantToken = login(attendant.getLogin());
    }

    @Test
    void listUsers_withAdminToken_returns200() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void listUsers_withAttendantToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + attendantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_withAttendantToken_returns403() throws Exception {
        String body = """
                {"firstName":"New","lastName":"User","email":"new@test.com",
                 "birthday":"1995-05-10","login":"newuser","password":"New@1234",
                 "phone":"11900000099"}
                """;

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + attendantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteLastAdmin_returns409() throws Exception {
        mockMvc.perform(delete("/api/users/" + adminUuid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteNonAdminUser_withAdminToken_returns204() throws Exception {
        mockMvc.perform(delete("/api/users/" + attendantUuid)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminSelfRoleChange_returns422() throws Exception {
        User admin = userRepository.findByUuid(adminUuid).orElseThrow();
        String body = """
                {"firstName":"%s","lastName":"%s","email":"%s",
                 "birthday":"1990-01-01","login":"%s","password":"Test@123",
                 "phone":"11900000001","role":"ATTENDANT"}
                """.formatted(admin.getFirstName(), admin.getLastName(), admin.getEmail(), admin.getLogin());

        mockMvc.perform(put("/api/users/" + adminUuid)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent());
    }

    private String login(String login) throws Exception {
        String response = mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + UserManagementRbacIntegrationTest.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("tokenValue").asText();
    }
}
