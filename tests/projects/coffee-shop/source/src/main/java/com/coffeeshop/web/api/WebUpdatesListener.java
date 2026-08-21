package com.coffeeshop.web.api;

import static com.coffeeshop.common.messaging.Topics.WEB_UPDATES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

@Service
public class WebUpdatesListener {

    private static final Logger log = LoggerFactory.getLogger(WebUpdatesListener.class);
    private final Sinks.Many<String> sink;

    public WebUpdatesListener(Sinks.Many<String> webUpdatesSink) {
        this.sink = webUpdatesSink;
    }

    @KafkaListener(topics = WEB_UPDATES, groupId = "web-service")
    public void onWebUpdate(String payload) {
        log.debug("WEB_UPDATES received: {}", payload);
        sink.tryEmitNext(payload); // push to all connected SSE clients
    }
}
