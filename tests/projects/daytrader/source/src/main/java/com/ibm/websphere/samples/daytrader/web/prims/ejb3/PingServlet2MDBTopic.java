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
package com.ibm.websphere.samples.daytrader.web.prims.ejb3;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.TextMessage;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This primitive is designed to run inside the TradeApplication and relies upon
 * the {@link com.ibm.websphere.samples.daytrader.util.TradeConfig} class to set
 * configuration parameters. PingServlet2MDBQueue tests key functionality of a
 * servlet call to a post a message to an MDB Topic. The TradeStreamerMDB (and
 * any other subscribers) receives the message This servlet makes use of the MDB
 * EJB {@link com.ibm.websphere.samples.daytrader.ejb3.DTStreamer3MDB} by
 * posting a message to the MDB Topic
 */
@Component
@WebServlet(name = "ejb3.PingServlet2MDBTopic", urlPatterns = { "/ejb3/PingServlet2MDBTopic" })
public class PingServlet2MDBTopic extends HttpServlet {

    private static final long serialVersionUID = 5925470158886928225L;

    private static String initTime;

    private static int hitCount;

    @Autowired
    private ConnectionFactory topicConnectionFactory;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();
        // use a stringbuffer to avoid concatenation of Strings
        StringBuffer output = new StringBuffer(100);
        output.append("<html><head><title>PingServlet2MDBTopic</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2MDBTopic<BR></FONT>"
                + "<FONT size=\"-1\" color=\"#000066\">"
                + "Tests the basic operation of a servlet posting a message to an EJB MDB (and other subscribers) through a JMS Topic.<BR>"
                + "<FONT color=\"red\"><B>Note:</B> Not intended for performance testing.</FONT>");

        // we only want to look up the JMS resources once
        try {

            TextMessage[] lastMessage = new TextMessage[1];
            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    JmsTemplate t = new JmsTemplate(topicConnectionFactory);
                    t.setPubSubDomain(true);
                    t.send("TradeStreamerTopic", session -> {
                        TextMessage m = session.createTextMessage();
                        m.setStringProperty("command", "ping");
                        m.setLongProperty("publishTime", System.currentTimeMillis());
                        m.setText("Ping message for topic TradeStreamerTopic sent from PingServlet2MDBTopic at "
                                + new java.util.Date());
                        lastMessage[0] = m;
                        return m;
                    });
                }

                // write out the output
                output.append("<HR>initTime: ").append(initTime);
                output.append("<BR>Hit Count: ").append(hitCount++);
                output.append("<HR>Posted Text message to TradeStreamerTopic topic");
                if (lastMessage[0] != null) {
                    output.append("<BR>Message: ").append(lastMessage[0]);
                    output.append("<BR><BR>Message text: ").append(lastMessage[0].getText());
                }
                output.append("<BR><HR></FONT></BODY></HTML>");
                out.println(output.toString());

            } catch (Exception e) {
                Log.error("PingServlet2MDBTopic.doGet(...):exception posting message to TradeStreamerTopic topic");
                throw e;
            }
        } // this is where I actually handle the exceptions
        catch (Exception e) {
            Log.error(e, "PingServlet2MDBTopic.doGet(...): error");
            res.sendError(500, "PingServlet2MDBTopic.doGet(...): error, " + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, configured with trade runtime configs, tests Servlet to Session EJB path";
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }

}
