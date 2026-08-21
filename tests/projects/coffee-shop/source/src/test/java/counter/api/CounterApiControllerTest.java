package com.coffeeshop.counter.api;

import com.coffeeshop.common.commands.PlaceOrderCommand;
import com.coffeeshop.common.domain.Location;
import com.coffeeshop.common.domain.OrderSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CounterApiControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderService orderService = Mockito.mock(OrderService.class);

    @BeforeEach
    void setup() {
        var controller = new CounterApiController(orderService);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                // If you have a global exception handler, add it here:
                // .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/order -> 202")
    void placeOrder_ok() throws Exception {
        var cmd = new PlaceOrderCommand(
                "order-123", OrderSource.WEB, Location.ATLANTA,
                null, null, null, Instant.now()
        );
        Mockito.doNothing().when(orderService).onOrderIn(Mockito.any());

        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(cmd)))
                .andExpect(status().isAccepted());
    }
}
