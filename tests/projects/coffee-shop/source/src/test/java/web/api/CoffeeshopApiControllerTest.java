package com.coffeeshop.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pure MVC slice for the web-service API controller.
 * No dependency on counter-service; no OrderService mock.
 */
@WebMvcTest(controllers = CoffeeshopApiController.class)
class CoffeeshopApiControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/order -> returns 202 Accepted")
    void postOrder_returns202() throws Exception {
        // Minimal JSON that matches your PlaceOrderCommand (no Optional fields)
        String json = """
        {
          "id": "order-123",
          "orderSource": "WEB",
          "location": "ATLANTA",
          "timestamp": "%s"
        }
        """.formatted(Instant.now().toString());

        mockMvc.perform(post("/api/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isAccepted());
    }
}
