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
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.jms.core.JmsTemplate;

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
 * servlet call to a post a message to an MDB Queue. The TradeBrokerMDB receives
 * the message This servlet makes use of the MDB EJB
 * {@link com.ibm.websphere.samples.daytrader.ejb3.DTBroker3MDB} by posting a
 * message to the MDB Queue
 */
@Component
@WebServlet(name = "ejb3.PingServlet2MDBQueue", urlPatterns = { "/ejb3/PingServlet2MDBQueue" })
public class PingServlet2MDBQueue extends HttpServlet {

    private static final long serialVersionUID = 2637271552188745216L;

    private static String initTime;

    private static int hitCount;

    @Autowired
    private ConnectionFactory queueConnectionFactory;

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
        output.append("<html><head><title>PingServlet2MDBQueue</title></head>"
                + "<body><HR><FONT size=\"+2\" color=\"#000066\">PingServlet2MDBQueue<BR></FONT>"
                + "<FONT size=\"-1\" color=\"#000066\">"
                + "Tests the basic operation of a servlet posting a message to an EJB MDB through a JMS Queue.<BR>"
                + "<FONT color=\"red\"><B>Note:</B> Not intended for performance testing.</FONT>");

        try {
            TextMessage[] lastMessage = new TextMessage[1];
            try {
                int iter = TradeConfig.getPrimIterations();
                for (int ii = 0; ii < iter; ii++) {
                    JmsTemplate t = new JmsTemplate(queueConnectionFactory);
                    t.setPubSubDomain(false);
                    t.send("TradeBrokerQueue", session -> {
                        TextMessage m = session.createTextMessage();
                        m.setStringProperty("command", "ping");
                        m.setLongProperty("publishTime", System.currentTimeMillis());
                        m.setText("Ping message for queue TradeBrokerQueue sent from PingServlet2MDBQueue at "
                                + new java.util.Date());
                        lastMessage[0] = m;
                        return m;
                    });
                }

                // write out the output
                output.append("<HR>initTime: ").append(initTime);
                output.append("<BR>Hit Count: ").append(hitCount++);
                output.append("<HR>Posted Text message to TradeBrokerQueue destination");
                if (lastMessage[0] != null) {
                    output.append("<BR>Message: ").append(lastMessage[0]);
                    output.append("<BR><BR>Message text: ").append(lastMessage[0].getText());
                }
                output.append("<BR><HR></FONT></BODY></HTML>");
                out.println(output.toString());

            } catch (Exception e) {
                Log.error("PingServlet2MDBQueue.doGet(...):exception posting message to TradeBrokerQueue destination ");
                throw e;
            }
        } // this is where I actually handle the exceptions
        catch (Exception e) {
            Log.error(e, "PingServlet2MDBQueue.doGet(...): error");
            res.sendError(500, "PingServlet2MDBQueue.doGet(...): error, " + e.toString());

        }
    }

    @Override
    public String getServletInfo() {
        return "web primitive, configured with trade runtime configs, tests Servlet to Session EJB path";

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
        hitCount = 0;
        initTime = new java.util.Date().toString();
    }

}
