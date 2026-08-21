/**
 * (C) Copyright IBM Corporation 2015, 2021.
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
package com.ibm.websphere.samples.daytrader.web.websocket;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.ibm.websphere.samples.daytrader.interfaces.TradeServices;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.RecentQuotePriceChangeList;
import com.ibm.websphere.samples.daytrader.web.SpringEndpointConfigurator;

import jakarta.json.JsonObject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * This class is a WebSocket EndPoint that sends the Market Summary in JSON form
 * and
 * encodes recent quote price changes when requested or when triggered by CDI
 * events.
 **/
@Component
@ServerEndpoint(value = "/marketsummary", encoders = { QuotePriceChangeListEncoder.class }, decoders = {
        ActionDecoder.class }, configurator = SpringEndpointConfigurator.class)
public class MarketSummaryWebSocket {

    @Autowired
    RecentQuotePriceChangeList recentQuotePriceChangeList;

    //
    private TradeServices tradeAction;

    @Autowired
    private ApplicationContext applicationContext;

    @jakarta.annotation.PostConstruct
    void resolveTradeServices() {
        String key = com.ibm.websphere.samples.daytrader.util.TradeConfig.getRunTimeModeNames()[com.ibm.websphere.samples.daytrader.util.TradeConfig.getRunTimeMode()];
        if (applicationContext != null && applicationContext.containsBean(key)) {
            this.tradeAction = applicationContext.getBean(key, TradeServices.class);
        }
        if (this.tradeAction == null) {
            java.util.Map<String, TradeServices> beans = (applicationContext != null)
                    ? applicationContext.getBeansOfType(TradeServices.class)
                    : java.util.Collections.emptyMap();
            java.util.Set<String> available = beans.keySet();
            throw new IllegalStateException("No TradeServices bean named '" + key + "' (available: " + available + ")");
        }
    }

    private static final List<Session> sessions = new CopyOnWriteArrayList<>();
    private final CountDownLatch latch = new CountDownLatch(1);

    static List<Session> getSessions() {
        return sessions;
    }

    @OnOpen
    public void onOpen(final Session session, EndpointConfig ec) {
        Log.trace(
                "MarketSummaryWebSocket:onOpen -- session -->" + session + "<--");

        sessions.add(session);
        latch.countDown();
    }

    @OnMessage
    public void sendMarketSummary(
            ActionMessage message,
            Session currentSession) {
        String action = message.getDecodedAction();

        Log.trace(
                "MarketSummaryWebSocket:sendMarketSummary -- received -->" +
                        action +
                        "<--");

        // Make sure onopen is finished
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.error("MarketSummaryWebSocket:sendMarketSummary interrupted", e);
            return;
        } catch (Throwable t) {
            Log.error("MarketSummaryWebSocket:sendMarketSummary await failed", t);
            return;
        }

        if (action != null && action.equals("updateMarketSummary")) {
            try {
                JsonObject mkSummary = tradeAction.getMarketSummary().toJSON();

                Log.trace(
                        "MarketSummaryWebSocket:sendMarketSummary -- sending -->" +
                                mkSummary +
                                "<--");

                currentSession.getAsyncRemote().sendText(mkSummary.toString());
            } catch (Exception e) {
                Log.error("MarketSummaryWebSocket:sendMarketSummary error", e);
            }
        } else if (action != null && action.equals("updateRecentQuotePriceChange")) {
            if (!recentQuotePriceChangeList.isEmpty()) {
                currentSession
                        .getAsyncRemote()
                        .sendObject(recentQuotePriceChangeList.recentList());
            }
        }
    }

    @OnError
    public void onError(Throwable t, Session currentSession) {
        Log.trace(
                "MarketSummaryWebSocket:onError -- session -->" +
                        currentSession +
                        "<--");
        Log.error("MarketSummaryWebSocket:onError", t);
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        Log.trace(
                "MarketSummaryWebSocket:onClose -- session -->" + session + "<--");
        sessions.remove(session);
    }

    // Event handling moved to MarketSummaryWebSocketEvents to avoid proxying this endpoint
}
