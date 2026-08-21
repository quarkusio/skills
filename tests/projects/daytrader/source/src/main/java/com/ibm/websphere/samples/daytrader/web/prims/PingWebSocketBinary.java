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
import java.nio.ByteBuffer;

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

/** This class a simple websocket that echos the binary it has been sent. */
@Component
@ServerEndpoint(value = "/pingBinary", configurator = SpringEndpointConfigurator.class)
public class PingWebSocketBinary {

    private Session currentSession = null;

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        currentSession = session;
    }

    @OnMessage
    public void ping(ByteBuffer data) {
        currentSession.getAsyncRemote().sendBinary(data);
    }

    @OnError
    public void onError(Throwable t) {
        Log.error("PingWebSocketBinary:onError", t);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {

        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            Log.error("PingWebSocketBinary:onClose error", e);
        }
    }

}
