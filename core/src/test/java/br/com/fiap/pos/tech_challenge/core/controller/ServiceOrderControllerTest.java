package br.com.fiap.pos.tech_challenge.core.controller;

import br.com.fiap.pos.tech_challenge.core.controller.dto.*;
import br.com.fiap.pos.tech_challenge.core.enums.EApplicationError;
import br.com.fiap.pos.tech_challenge.core.enums.ServiceOrderStatus;
import br.com.fiap.pos.tech_challenge.core.exception.CustomerNotFoundException;
import br.com.fiap.pos.tech_challenge.core.exception.ExceptionHandling;
import br.com.fiap.pos.tech_challenge.core.exception.ProductNotFoundException;
import br.com.fiap.pos.tech_challenge.core.exception.VehicleNotFoundException;
import br.com.fiap.pos.tech_challenge.core.service.ServiceOrderService;
import br.com.fiap.pos.tech_challenge.core.util.Translator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author pauloogsouza
 * @since 2026-06-29
 */
@ExtendWith(MockitoExtension.class)
class ServiceOrderControllerTest {

    @Mock ServiceOrderService service;

    @InjectMocks ServiceOrderController controller;

    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    private ServiceOrderResponse stubResponse() {
        return new ServiceOrderResponse(UUID.randomUUID(), ServiceOrderStatus.RECEIVED, "queixa", null, null);
    }

    @Nested
    @DisplayName("POST /api/service-orders")
    class Open {
        @Test
        void open_returns201OnSuccess() throws Exception {
            OpenServiceOrderRequest request = new OpenServiceOrderRequest(
                    UUID.randomUUID(), UUID.randomUUID(), "Barulho no motor", null, null);
            when(service.openServiceOrder(any(), any(), any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        void open_forwardsOptionalItemsToService() throws Exception {
            UUID customerUuid = UUID.randomUUID();
            UUID vehicleUuid = UUID.randomUUID();
            UUID msUuid = UUID.randomUUID();
            UUID productUuid = UUID.randomUUID();
            OpenServiceOrderRequest request = new OpenServiceOrderRequest(
                    customerUuid, vehicleUuid, "Revisão",
                    java.util.List.of(msUuid),
                    java.util.List.of(new OpenProductItemRequest(productUuid, java.math.BigDecimal.ONE)));
            when(service.openServiceOrder(any(), any(), any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(service).openServiceOrder(customerUuid, vehicleUuid, "Revisão",
                    java.util.List.of(msUuid),
                    java.util.List.of(new OpenProductItemRequest(productUuid, java.math.BigDecimal.ONE)));
        }

        @Test
        void open_returns404WhenProductDoesNotExist() throws Exception {
            MockMvc mockMvcWithAdvice = MockMvcBuilders.standaloneSetup(controller)
                    .setControllerAdvice(new ExceptionHandling(mock(Translator.class)))
                    .build();
            OpenServiceOrderRequest request = new OpenServiceOrderRequest(
                    UUID.randomUUID(), UUID.randomUUID(), "Revisão", null,
                    java.util.List.of(new OpenProductItemRequest(UUID.randomUUID(), java.math.BigDecimal.ONE)));
            when(service.openServiceOrder(any(), any(), any(), any(), any()))
                    .thenThrow(new ProductNotFoundException());

            mockMvcWithAdvice.perform(post("/api/service-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void open_returns404WhenCustomerDoesNotExist() throws Exception {
            MockMvc mockMvcWithAdvice = MockMvcBuilders.standaloneSetup(controller)
                    .setControllerAdvice(new ExceptionHandling(mock(Translator.class)))
                    .build();
            OpenServiceOrderRequest request = new OpenServiceOrderRequest(
                    UUID.randomUUID(), UUID.randomUUID(), "Barulho no motor", null, null);
            when(service.openServiceOrder(any(), any(), any(), any(), any()))
                    .thenThrow(new CustomerNotFoundException());

            mockMvcWithAdvice.perform(post("/api/service-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode")
                            .value(EApplicationError.CUSTOMER_NOT_FOUND.getErrorCode()));
        }

        @Test
        void open_returns404WhenVehicleDoesNotExist() throws Exception {
            MockMvc mockMvcWithAdvice = MockMvcBuilders.standaloneSetup(controller)
                    .setControllerAdvice(new ExceptionHandling(mock(Translator.class)))
                    .build();
            OpenServiceOrderRequest request = new OpenServiceOrderRequest(
                    UUID.randomUUID(), UUID.randomUUID(), "Barulho no motor", null, null);
            when(service.openServiceOrder(any(), any(), any(), any(), any()))
                    .thenThrow(new VehicleNotFoundException());

            mockMvcWithAdvice.perform(post("/api/service-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode")
                            .value(EApplicationError.VEHICLE_NOT_FOUND.getErrorCode()));
        }
    }

    @Nested
    @DisplayName("GET /api/service-orders")
    class List {
        @Test
        void list_returns200() throws Exception {
            when(service.listServiceOrders(isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(java.util.List.of(stubResponse()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/service-orders"))
                    .andExpect(status().isOk());
        }

        @Test
        void list_filtersByStatus() throws Exception {
            when(service.listServiceOrders(any(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(new PageImpl<>(java.util.List.of(stubResponse()), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/api/service-orders").param("status", "RECEIVED"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/service-orders/{uuid}")
    class Get {
        @Test
        void get_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.getServiceOrder(uuid)).thenReturn(stubResponse());

            mockMvc.perform(get("/api/service-orders/{uuid}", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/service-orders/{uuid}/status")
    class GetStatus {
        @Test
        void getStatus_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.getServiceOrderStatus(uuid)).thenReturn(
                    new ServiceOrderStatusResponse(uuid, ServiceOrderStatus.RECEIVED, null));

            mockMvc.perform(get("/api/service-orders/{uuid}/status", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/diagnosis/start")
    class StartDiagnosis {
        @Test
        void startDiagnosis_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.startDiagnosis(uuid)).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/diagnosis/start", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/diagnosis/services")
    class AddService {
        @Test
        void addService_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            AddServiceRequest request = new AddServiceRequest(UUID.randomUUID());
            when(service.addServiceToDiagnosis(any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/diagnosis/services", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/service-orders/{uuid}/diagnosis/services/{msUuid}")
    class RemoveService {
        @Test
        void removeService_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            UUID msUuid = UUID.randomUUID();
            when(service.removeServiceFromDiagnosis(uuid, msUuid)).thenReturn(stubResponse());

            mockMvc.perform(delete("/api/service-orders/{uuid}/diagnosis/services/{msUuid}", uuid, msUuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/diagnosis/products")
    class AddProduct {
        @Test
        void addProduct_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            AddProductRequest request = new AddProductRequest(UUID.randomUUID(), java.math.BigDecimal.ONE);
            when(service.addProductToDiagnosis(any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/diagnosis/products", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/service-orders/{uuid}/diagnosis/products/{productUuid}")
    class RemoveProductFromDiagnosis {
        @Test
        void removeProduct_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            UUID productUuid = UUID.randomUUID();
            when(service.removeProductFromDiagnosis(uuid, productUuid)).thenReturn(stubResponse());

            mockMvc.perform(delete("/api/service-orders/{uuid}/diagnosis/products/{productUuid}", uuid, productUuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/diagnosis/complete")
    class CompleteDiagnosis {
        @Test
        void completeDiagnosis_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.completeDiagnosis(uuid)).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/diagnosis/complete", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/approval")
    class QuoteDecision {
        @Test
        void quoteDecision_approve_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            QuoteDecisionRequest request = new QuoteDecisionRequest("token123", "52998224725", QuoteDecisionType.APPROVE);
            when(service.approveQuote(any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/approval", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).approveQuote(uuid, "52998224725", "token123");
        }

        @Test
        void quoteDecision_reject_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            QuoteDecisionRequest request = new QuoteDecisionRequest("token123", "52998224725", QuoteDecisionType.REJECT);
            when(service.rejectQuote(any(), any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/approval", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).rejectQuote(uuid, "52998224725", "token123", null);
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/otp/resend")
    class ResendOtp {
        @Test
        void resendOtp_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();

            mockMvc.perform(post("/api/service-orders/{uuid}/otp/resend", uuid))
                    .andExpect(status().isOk());

            verify(service).resendOTP(uuid);
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/execution/complete")
    class CompleteExecution {
        @Test
        void completeExecution_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.completeExecution(uuid)).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/execution/complete", uuid))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/delivery/accept")
    class AcceptDelivery {
        @Test
        void acceptDelivery_byOtp_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            AcceptDeliveryRequest request = new AcceptDeliveryRequest("token123", "52998224725");
            when(service.acceptDelivery(any(), any(), any(), any(Boolean.class))).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/delivery/accept", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).acceptDelivery(uuid, "52998224725", "token123", false);
        }

        @Test
        void acceptDelivery_withoutBody_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.acceptDelivery(any(), isNull(), isNull(), any(Boolean.class))).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/delivery/accept", uuid))
                    .andExpect(status().isOk());

            verify(service).acceptDelivery(uuid, null, null, false);
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/delivery/reject")
    class RejectDelivery {
        @Test
        void rejectDelivery_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            RejectDeliveryRequest request = new RejectDeliveryRequest("token123", "52998224725", "pecas com defeito");
            when(service.rejectDelivery(any(), any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/delivery/reject", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/execution/products")
    class RequestProduct {
        @Test
        void requestProduct_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            RequestProductRequest request = new RequestProductRequest(UUID.randomUUID(), java.math.BigDecimal.ONE);
            when(service.requestProduct(any(), any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/execution/products", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/service-orders/{uuid}/products/{productUuid}")
    class ReturnProduct {
        @Test
        void returnProduct_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            UUID productUuid = UUID.randomUUID();
            ReturnProductRequest request = new ReturnProductRequest("senha123", UUID.randomUUID());
            when(service.returnProduct(any(), any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(delete("/api/service-orders/{uuid}/products/{productUuid}", uuid, productUuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/service-orders/{uuid}/close-dispute")
    class CloseDispute {
        @Test
        void closeDispute_withBody_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            CloseDisputeRequest request = new CloseDisputeRequest("resolucao");
            when(service.closeDispute(any(), any(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/close-dispute", uuid)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(service).closeDispute(uuid, "resolucao", null);
        }

        @Test
        void closeDispute_withoutBody_returns200() throws Exception {
            UUID uuid = UUID.randomUUID();
            when(service.closeDispute(any(), isNull(), any())).thenReturn(stubResponse());

            mockMvc.perform(post("/api/service-orders/{uuid}/close-dispute", uuid))
                    .andExpect(status().isOk());

            verify(service).closeDispute(uuid, null, null);
        }
    }
}