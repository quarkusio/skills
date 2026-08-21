
package com.coffeeshop.web.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private final Sinks.Many<String> sink;

    public DashboardController(Sinks.Many<String> webUpdatesSink) {
        this.sink = webUpdatesSink;
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream() {
        // Start with a one-off “init” event so the client knows it’s connected
        Flux<ServerSentEvent<String>> init = Flux.just(
            ServerSentEvent.builder("dashboard stream connected at " + Instant.now()).event("init").build()
        );

        Flux<ServerSentEvent<String>> updates = sink.asFlux()
            .map(msg -> ServerSentEvent.builder(msg).event("update").build());

        return init.concatWith(updates);
    }
}




/** This iteration was before we had a separate  Kafka listener->reactor sink->SSE*/
// package com.coffeeshop.web.api;

// import com.coffeeshop.web.messaging.Topics;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.http.MediaType;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.stereotype.Component;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.http.codec.ServerSentEvent;
// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Sinks;

// @RestController
// @Component
// public class DashboardController {

//     private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

//     // Multicast sink; buffer a little to survive slow clients reconnecting
//     private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

//     // Kafka -> push into sink
//     @KafkaListener(topics = Topics.WEB_UPDATES, groupId = "web-service")
//     public void onWebUpdate(@Payload String json) {
//         log.debug("WEB_UPDATES consumed: {}", json);
//         sink.tryEmitNext(json);
//     }

//     // Browser -> subscribe to SSE
//     @GetMapping(path = "/api/dashboard/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//     public Flux<ServerSentEvent<String>> stream() {
//         return sink.asFlux().map(data -> ServerSentEvent.builder(data).build());
//     }
// }




/** The following was an initial version before we introduced kafka  */

// package com.coffeeshop.web.api;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.http.MediaType;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// import java.io.IOException;
// import java.time.Instant;




// /**
//  * Minimal Spring version of the Quarkus SSE dashboard stream.
//  * Produces text/event-stream and stays connected.
//  * Messages are not published yet (we'll wire a publisher later).
//  */
// @RestController
// @RequestMapping("/api/dashboard")
// public class DashboardController {

//     private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

//     @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//     public SseEmitter dashboardStream() {
//         // Default timeout: 30 seconds; you can choose -1L for never timeout
//         SseEmitter emitter = new SseEmitter(0L); // no timeout; client controls lifecycle
//         try {
//             // Send an initial event so the client knows it's connected.
//             emitter.send(SseEmitter.event()
//                 .name("init")
//                 .data("dashboard stream connected at " + Instant.now().toString()));
//         } catch (IOException e) {
//             LOGGER.warn("Failed to send initial SSE event", e);
//             emitter.completeWithError(e);
//         }

//         // TODO (later): register this emitter with a broadcaster and push real updates
//         // broadcaster.register(emitter);

//         return emitter;
//     }
//}

