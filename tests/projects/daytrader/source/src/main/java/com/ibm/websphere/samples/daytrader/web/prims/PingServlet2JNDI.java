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
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.ibm.websphere.samples.daytrader.util.Log;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * PingServlet2JNDI performs a basic JNDI lookup of a JDBC DataSource
 *
 */
@Component
@WebServlet(name = "PingServlet2JNDI", urlPatterns = { "/servlet/PingServlet2JNDI" })
public class PingServlet2JNDI extends HttpServlet {

    private static final long serialVersionUID = -8236271998141415347L;
    private static String initTime;
    private static int hitCount;

    /**
     * forwards post requests to the doGet method Creation date: (11/6/2000
     * 10:52:39 AM)
     *
     * @param res
     *             jakarta.servlet.http.HttpServletRequest
     * @param res2
     *             jakarta.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        doGet(req, res);
    }

    /**
     * this is the main method of the servlet that will service all get
     * requests.
     *
     * @param request
     *                 HttpServletRequest
     * @param responce
     *                 HttpServletResponce
     **/
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/html");
        java.io.PrintWriter out = res.getWriter();

        StringBuffer output = new StringBuffer(100);

        try {
            output.append("<html><head><title>Ping JNDI -- lookup of JDBC DataSource</title></head>"
                    + "<body><HR><FONT size=\"+2\" color=\"#000066\">Ping JNDI -- lookup of JDBC DataSource</FONT><HR><FONT size=\"-1\" color=\"#000066\">Init time : "
                    + initTime);
            hitCount++;
            output.append("</FONT><BR>Hit Count: " + hitCount);
            output.append("<HR></body></html>");
            out.println(output.toString());
        } catch (Exception e) {
            Log.error(e, "PingServlet2JNDI -- error look up of a JDBC DataSource");
            res.sendError(500, "PingServlet2JNDI Exception caught: " + e.toString());
        }

    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic JNDI look up of a JDBC DataSource";
    }

    /**
     * called when the class is loaded to initialize the servlet
     *
     * @param config
     *               ServletConfig:
     **/
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        hitCount = 0;
        initTime = new java.util.Date().toString();
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    }
}