package com.healthshield.controller;

import com.healthshield.exception.GlobalExceptionHandler;
import com.healthshield.repository.UserRepository;
import com.healthshield.service.NotificationService;
import com.healthshield.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void subscribe_success_startsSseStream() throws Exception {
        SseEmitter emitter = new SseEmitter(1_000L);
        given(jwtUtil.extractEmail("valid-token")).willReturn("user@example.com");
        given(notificationService.subscribe("user@example.com")).willReturn(emitter);

        mockMvc.perform(get("/api/notifications/subscribe")
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void subscribe_expiredToken_returnsUnauthorizedEventStreamMessage() throws Exception {
        given(jwtUtil.extractEmail("expired-token")).willThrow(new JwtException("expired"));

        MvcResult result = mockMvc.perform(get("/api/notifications/subscribe")
                        .param("token", "expired-token"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(containsString("event:auth-error")))
                .andExpect(content().string(containsString("data:Session expired. Please log in again.")));
    }
}
