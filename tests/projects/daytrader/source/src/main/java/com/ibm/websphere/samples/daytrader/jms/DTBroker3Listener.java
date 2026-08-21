package com.ibm.websphere.samples.daytrader.jms;

import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import com.ibm.websphere.samples.daytrader.interfaces.TradeServices;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.MDBStats;
import com.ibm.websphere.samples.daytrader.util.TimerStat;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

@Service
public class DTBroker3Listener {

    private final MDBStats mdbStats = MDBStats.getInstance();
    private int statInterval = 10000;

    private final ApplicationContext ctx;
    private final Map<String, TradeServices> services;
    private TradeServices trade;

    @Autowired
    public DTBroker3Listener(ApplicationContext ctx, Map<String, TradeServices> services) {
        this.ctx = ctx;
        this.services = services;
    }

    @PostConstruct
    void bootstrapTradeServices() {
        String[] modes = TradeConfig.getRunTimeModeNames();
        String modeName = modes[TradeConfig.getRunTimeMode()];
        TradeServices candidate = services.get(modeName);
        if (candidate == null) {
            try { candidate = ctx.getBean(modeName, TradeServices.class); } catch (Exception ignored) {}
        }
        if (candidate == null && services.size() == 1) {
            candidate = services.values().iterator().next();
        }
        this.trade = candidate;
        Log.log("DTBroker3Listener: selected TradeServices = " +
                (trade != null ? trade.getClass().getName() : "<none>") +
                " (mode=" + modeName + ")");
    }

    @JmsListener(destination = "TradeBrokerQueue", containerFactory = "queueFactory")
    public void onBrokerQueueMessage(Message message) {
        try {
            String body = (message instanceof TextMessage) ? ((TextMessage) message).getText() : "<non-text>";
            String command = message.getStringProperty("command");

            Log.trace("DTBroker3MDB:onMessage -- received --> " + body + " command--> " + command + " <--");

            if (message.getJMSRedelivered()) {
                Log.log("DTBroker3MDB: redelivered due to rollback:\n" + body);
                // Parity with EJB: ignore redeliveries if needed
                return;
            }
            if (command == null) {
                Log.debug("DTBroker3MDB:onMessage -- null command. Message-->" + message);
                return;
            }

            if (command.equalsIgnoreCase("neworder")) {
                Integer orderID = message.propertyExists("orderID") ? message.getIntProperty("orderID") : null;
                boolean twoPhase = message.propertyExists("twoPhase") && message.getBooleanProperty("twoPhase");
                boolean direct = message.propertyExists("direct") && message.getBooleanProperty("direct");
                long publishTime = message.propertyExists("publishTime") ? message.getLongProperty("publishTime") : -1L;
                long receiveTime = System.currentTimeMillis();

                try {
                    Log.trace("DTBroker3MDB:onMessage - completing order " + orderID
                            + " twoPhase=" + twoPhase + " direct=" + direct);

                    if (trade == null) throw new IllegalStateException("No TradeServices bean selected");
                    trade.completeOrder(orderID, twoPhase);

                    TimerStat s = mdbStats.addTiming("DTBroker3MDB:neworder", publishTime, receiveTime);
                    if ((s.getCount() % statInterval) == 0) {
                        Log.log(" DTBroker3MDB: processed " + statInterval + " stock trading orders."
                                + " Total = " + s.getCount()
                                + " Time (s): min=" + s.getMinSecs()
                                + " max=" + s.getMaxSecs()
                                + " avg=" + s.getAvgSecs());
                    }
                } catch (Exception e) {
                    Log.error("DTBroker3MDB:onMessage Exception completing order: " + orderID + "\n", e);
                    // Signal rollback → redelivery (needs transacted listener)
                    throw new RuntimeException("Rollback to redeliver", e);
                }

            } else if (command.equalsIgnoreCase("ping")) {
                Log.trace("DTBroker3MDB:onMessage test command -- " + body);

                long publishTime = message.propertyExists("publishTime") ? message.getLongProperty("publishTime") : -1L;
                long receiveTime = System.currentTimeMillis();

                TimerStat s = mdbStats.addTiming("DTBroker3MDB:ping", publishTime, receiveTime);
                if ((s.getCount() % statInterval) == 0) {
                    Log.log(" DTBroker3MDB: received " + statInterval + " ping messages."
                            + " Total = " + s.getCount()
                            + " Time (s): min=" + s.getMinSecs()
                            + " max=" + s.getMaxSecs()
                            + " avg=" + s.getAvgSecs());
                }
            } else {
                Log.error("DTBroker3MDB:onMessage - unknown command --> " + command + " <-- body=" + body);
            }

        } catch (RuntimeException rte) {
            throw rte; // ensures rollback when transacted
        } catch (Throwable t) {
            Log.error("DTBroker3MDB: Error in listener", t);
            throw new RuntimeException("Rollback to redeliver", t);
        }
    }
}