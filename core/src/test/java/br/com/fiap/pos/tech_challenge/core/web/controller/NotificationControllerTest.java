package br.com.fiap.pos.tech_challenge.core.web.controller;

import br.com.fiap.pos.tech_challenge.core.web.dto.NotificationResponse;
import br.com.fiap.pos.tech_challenge.core.domain.model.User;
import br.com.fiap.pos.tech_challenge.core.domain.enums.NotificationType;
import br.com.fiap.pos.tech_challenge.core.web.mapper.NotificationMapper;
import br.com.fiap.pos.tech_challenge.core.infrastructure.security.UserDetailsImpl;
import br.com.fiap.pos.tech_challenge.core.application.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock NotificationService notificationService;
    @Mock NotificationMapper mapper;

    @InjectMocks NotificationController controller;

    MockMvc mockMvc;

    UserDetailsImpl principal;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setLogin("user");
        principal = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private NotificationResponse stubResponse() {
        return new NotificationResponse(UUID.randomUUID(), NotificationType.INSUFFICIENT_STOCK, "msg", null, false, null);
    }

    @Nested
    @DisplayName("GET /api/notifications")
    class ListNotifications {
        @Test
        void listNotifications_returns200() throws Exception {
            when(notificationService.listByUser(eq(1L), any()))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{uuid}/read")
    class MarkAsRead {
        @Test
        void markAsRead_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            var notification = mock(br.com.fiap.pos.tech_challenge.core.domain.model.Notification.class);
            when(notificationService.markAsRead(eq(uuid), eq(1L))).thenReturn(notification);
            when(mapper.toResponse(notification)).thenReturn(stubResponse());

            mockMvc.perform(patch("/api/notifications/{uuid}/read", uuid))
                    .andExpect(status().isOk());
        }
    }
}