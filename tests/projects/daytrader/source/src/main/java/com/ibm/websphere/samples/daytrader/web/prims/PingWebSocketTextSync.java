/**
 * (C) Copyright IBM Corporation 2015.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.websphere.samples.daytrader.web.prims;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.web.SpringEndpointConfigurator;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * This class a simple websocket that sends the number of times it has been
 * pinged.
 */
@Component
@ServerEndpoint(value = "/pingTextSync", configurator = SpringEndpointConfigurator.class)
public class PingWebSocketTextSync {

    private Session currentSession = null;
    private Integer hitCount = null;

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        currentSession = session;
        hitCount = 0;
    }

    @OnMessage
    public void ping(String text) {
        hitCount++;
        try {
            currentSession.getBasicRemote().sendText(hitCount.toString());
        } catch (IOException e) {
            Log.error("PingWebSocketTextSync:send error", e);
        }
    }

    @OnError
    public void onError(Throwable t) {
        Log.error("PingWebSocketTextSync:onError", t);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
    }
}
