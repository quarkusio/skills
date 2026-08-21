package com.coffeeshop.web.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Sinks;

@Configuration
public class SseBroadcasterConfig {

    /**
     * Multicast sink for WEB_UPDATES. Buffer to handle short bursts.
     * Consumers (SSE clients) subscribe via asFlux().
     */
    @Bean
    public Sinks.Many<String> webUpdatesSink() {
        return Sinks.many().multicast().onBackpressureBuffer(1024, false);
    }
}
