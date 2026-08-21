package com.ibm.websphere.samples.daytrader.web.websocket;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.ibm.websphere.samples.daytrader.events.MarketSummaryUpdateEvent;
import com.ibm.websphere.samples.daytrader.events.QuotePriceChangeEvent;
import com.ibm.websphere.samples.daytrader.interfaces.TradeServices;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.RecentQuotePriceChangeList;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

import jakarta.websocket.Session;

@Component
public class MarketSummaryWebSocketEvents {

    @Autowired
    private RecentQuotePriceChangeList recentQuotePriceChangeList;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    private TradeServices tradeAction;

    @jakarta.annotation.PostConstruct
    void resolveTradeServices() {
        String key = TradeConfig.getRunTimeModeNames()[TradeConfig.getRunTimeMode()];
        if (applicationContext.containsBean(key)) {
            this.tradeAction = applicationContext.getBean(key, TradeServices.class);
        }
        if (this.tradeAction == null) {
            java.util.Map<String, TradeServices> beans = applicationContext.getBeansOfType(TradeServices.class);
            java.util.Set<String> available = beans.keySet();
            throw new IllegalStateException("No TradeServices bean named '" + key + "' (available: " + available + ")");
        }
    }

    @EventListener
    @Async("ManagedExecutorService")
    public void onStockChange(QuotePriceChangeEvent quotePriceChangeEvent) {
        Log.trace("MarketSummaryWebSocketEvents:onStockChange");
        quotePriceChangeEvent.payload();
        for (Session s : MarketSummaryWebSocket.getSessions()) {
            if (s.isOpen()) {
                s.getAsyncRemote().sendObject(recentQuotePriceChangeList.recentList());
            }
        }
    }

    @EventListener
    @Async("ManagedExecutorService")
    public void onMarketSummaryUpdate(MarketSummaryUpdateEvent marketSummaryUpdateEvent) {
        Log.trace("MarketSummaryWebSocketEvents:onMarketSummaryUpdate");
        marketSummaryUpdateEvent.payload();
        try {
            jakarta.json.JsonObject mkSummary = tradeAction.getMarketSummary().toJSON();
            for (Session s : MarketSummaryWebSocket.getSessions()) {
                if (s.isOpen()) {
                    s.getAsyncRemote().sendText(mkSummary.toString());
                }
            }
        } catch (Exception e) {
            Log.error("MarketSummaryWebSocketEvents:onMarketSummaryUpdate error", e);
        }
    }
}
