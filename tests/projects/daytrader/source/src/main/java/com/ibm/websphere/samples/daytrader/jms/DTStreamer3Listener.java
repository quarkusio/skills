package com.ibm.websphere.samples.daytrader.jms;

import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.MDBStats;
import com.ibm.websphere.samples.daytrader.util.TimerStat;

@Service
public class DTStreamer3Listener {

    private final MDBStats mdbStats = MDBStats.getInstance();
    private int statInterval = 10000;

    @JmsListener(destination = "TradeStreamerTopic", containerFactory = "topicFactory")
    public void onStreamerTopicEvent(Message message) {
        try {
            String body = (message instanceof TextMessage) ? ((TextMessage) message).getText() : "<non-text>";
            String command = message.getStringProperty("command");

            Log.trace("DTStreamer3MDB:onMessage -- received message -->" + body + " command-->" + command + "<--");

            if (command == null) {
                Log.debug("DTStreamer3MDB:onMessage -- received message with null command. Message-->" + message);
                return;
            }

            if (command.equalsIgnoreCase("updateQuote")) {
                Log.trace("DTStreamer3MDB:onMessage -- updateQuote --> " + body
                        + "\n\t symbol = " + message.getStringProperty("symbol")
                        + "\n\t current price = " + message.getStringProperty("price")
                        + "\n\t old price = " + message.getStringProperty("oldPrice"));

                long publishTime = message.getLongProperty("publishTime");
                long receiveTime = System.currentTimeMillis();
                TimerStat s = mdbStats.addTiming("DTStreamer3MDB:updateQuote", publishTime, receiveTime);

                if ((s.getCount() % statInterval) == 0) {
                    Log.log(" DTStreamer3MDB: " + statInterval + " prices updated:"
                            + " Total message count = " + s.getCount()
                            + " Time (s): min=" + s.getMinSecs()
                            + " max=" + s.getMaxSecs()
                            + " avg=" + s.getAvgSecs());
                }

            } else if (command.equalsIgnoreCase("ping")) {
                Log.trace("DTStreamer3MDB:onMessage received ping -- " + body);

                long publishTime = message.getLongProperty("publishTime");
                long receiveTime = System.currentTimeMillis();
                TimerStat s = mdbStats.addTiming("DTStreamer3MDB:ping", publishTime, receiveTime);

                if ((s.getCount() % statInterval) == 0) {
                    Log.log(" DTStreamer3MDB: received " + statInterval + " ping messages."
                            + " Total message count = " + s.getCount()
                            + " Time (s): min=" + s.getMinSecs()
                            + " max=" + s.getMaxSecs()
                            + " avg=" + s.getAvgSecs());
                }

            } else {
                Log.error("DTStreamer3MDB:onMessage - unknown message command=" + command + " body=" + body);
            }

        } catch (Throwable t) {
            Log.error("DTStreamer3MDB: Exception", t);
            // For redelivery behavior on topic, wire error handlers if desired.
        }
    }
}