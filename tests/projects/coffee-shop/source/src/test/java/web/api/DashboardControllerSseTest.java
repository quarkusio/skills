// web-service/src/test/java/com/coffeeshop/web/api/DashboardControllerSseTest.java
package com.coffeeshop.web.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import jakarta.persistence.criteria.CriteriaBuilder.In;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@WebFluxTest(controllers = DashboardController.class)
class DashboardControllerSseTest {

    /** 
    Why this is needed
    In WebFlux, your SSE endpoint typically subscribes to a Flux and you publish events into that Flux via a sink. 
    The sink (Sinks.Many<String>) must be constructed somewhere and managed as a singleton so all subscribers get the same stream. 
    Spring injects it — but only if you declare it as a bean.
    Without a bean, Spring can’t satisfy the controller’s constructor parameter → UnsatisfiedDependencyException.
    */
    @TestConfiguration
    static class TestSseConfig {
        @Bean
        public Sinks.Many<String> webUpdatesSink() {
            return Sinks.many().multicast().onBackpressureBuffer(16, false);
        }
    }

  @Autowired
  WebTestClient webTestClient;

  @Test
  void stream_connects_and_returns_event_stream() {
    webTestClient.get()
        .uri("/api/dashboard/stream")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
  }
}
