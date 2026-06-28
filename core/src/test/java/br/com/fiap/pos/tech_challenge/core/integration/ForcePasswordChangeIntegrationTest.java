package br.com.fiap.pos.tech_challenge.core.integration;

import br.com.fiap.pos.tech_challenge.core.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author johncgo
 * @since 2026-06-27
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class ForcePasswordChangeIntegrationTest {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("tech_challenge_force_pwd_test")
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
    final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean JavaMailSender mailSender;

    MockMvc mockMvc;

    private String seedAdminToken;

    @BeforeEach
    void setup() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);
        userRepository.findByLogin("admin").ifPresent(admin -> {
            admin.setPassword(encoder.encode("admin"));
            admin.setForceChangePassword(true);
            userRepository.save(admin);
        });

        seedAdminToken = login("admin", "admin");
    }

    @Test
    void seedAdmin_existsWithPasswordTemporariaTrue() {
        var admin = userRepository.findByLogin("admin");
        assertThat(admin).isPresent();
        assertThat(admin.get().isForceChangePassword()).isTrue();
    }

    @Test
    void login_withDefaultCredentials_returns200() throws Exception {
        mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getUsers_withSeedAdminToken_returns403_passwordChangeRequired() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + seedAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_withWrongCurrentPassword_returns400() throws Exception {
        String body = """
                {"senhaAtual":"wrong","novaSenha":"Admin@2025"}
                """;

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + seedAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_withWeakNewPassword_returns400() throws Exception {
        String body = """
                {"senhaAtual":"admin","novaSenha":"fraca"}
                """;

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + seedAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_withValidCredentials_returns204_andClearsFlag() throws Exception {
        String body = """
                {"senhaAtual":"admin","novaSenha":"Admin@2025"}
                """;

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + seedAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        var admin = userRepository.findByLogin("admin");
        assertThat(admin).isPresent();
        assertThat(admin.get().isForceChangePassword()).isFalse();
    }

    @Test
    void afterPasswordChange_sameToken_canAccessProtectedEndpoint() throws Exception {
        String body = """
                {"senhaAtual":"admin","novaSenha":"Admin@2025"}
                """;

        mockMvc.perform(put("/api/profile/password")
                        .header("Authorization", "Bearer " + seedAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + seedAdminToken))
                .andExpect(status().isOk());
    }

    private String login(String login, String password) throws Exception {
        String response = mockMvc.perform(post("/api/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(response);
        return node.get("tokenValue").asText();
    }
}
