package com.ibm.websphere.samples.daytrader.web.prims;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.web.SpringEndpointConfigurator;
import com.ibm.websphere.samples.daytrader.web.websocket.JsonDecoder;
import com.ibm.websphere.samples.daytrader.web.websocket.JsonEncoder;
import com.ibm.websphere.samples.daytrader.web.websocket.JsonMessage;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@Component
@ServerEndpoint(value = "/pingWebSocketJson", encoders = JsonEncoder.class, decoders = JsonDecoder.class, configurator = SpringEndpointConfigurator.class // ensures
                                                                                                                                                          // Spring
                                                                                                                                                          // builds/autowires
                                                                                                                                                          // this
)
public class PingWebSocketJson {

    @Autowired
    @Qualifier("ManagedExecutorService")
    private TaskExecutor taskExecutor; // define a ThreadPoolTaskExecutor bean

    // Per-connection state (new instance per WS connection)
    // Keep a session reference if needed later (not used directly now)
    private Session currentSession;
    private final AtomicInteger sentHitCount = new AtomicInteger();
    private final AtomicInteger receivedHitCount = new AtomicInteger();
    private final AtomicBoolean running = new AtomicBoolean(false);

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        this.currentSession = session;
        sentHitCount.set(0);
        receivedHitCount.set(0);
        running.set(true);

        taskExecutor.execute(() -> {
            try {
                Thread.sleep(500);
                while (running.get() && session.isOpen()) {
                    int count = sentHitCount.incrementAndGet();

                    JsonMessage response = new JsonMessage();
                    response.setKey("sentHitCount");
                    response.setValue(Integer.toString(count));

                    session.getAsyncRemote().sendObject(response);
                    Thread.sleep(100);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                Log.error("PingWebSocketJson: background loop error", t);
            }
        });
    }

    @OnMessage
    public void onMessage(JsonMessage message, Session session) {
        int count = receivedHitCount.incrementAndGet();

        JsonMessage response = new JsonMessage();
        response.setKey("receivedHitCount");
        response.setValue(Integer.toString(count));

        // use the session passed by JSR-356 (or currentSession; both are same
        // connection)
        session.getAsyncRemote().sendObject(response);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        running.set(false);
    }

    @OnError
    public void onError(Session session, Throwable t) {
        running.set(false);
        Log.error("PingWebSocketJson:onError", t);
    }
}
