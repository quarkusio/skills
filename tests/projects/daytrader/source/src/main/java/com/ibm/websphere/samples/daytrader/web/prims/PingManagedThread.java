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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.ibm.websphere.samples.daytrader.util.Log;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@WebServlet(asyncSupported = true, name = "PingManagedThread", urlPatterns = { "/servlet/PingManagedThread" })
public class PingManagedThread extends HttpServlet {

    private static final long serialVersionUID = -4695386150928451234L;
    private static String initTime;
    private static int hitCount;

    @Autowired
    @Qualifier("ManagedExecutorService")
    private AsyncTaskExecutor AsyncTaskExecutor;

    /**
     * forwards post requests to the doGet method Creation date: (03/18/2014
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
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        final AsyncContext asyncContext = req.startAsync();
        final ServletOutputStream out = res.getOutputStream();

        try {

            res.setContentType("text/html");

            out.println("<html><head><title>Ping ManagedThread</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping ManagedThread<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
                    + initTime + "<BR/><BR/></FONT>");

            AsyncTaskExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        out.println("<b>HitCount: " + ++hitCount + "</b><br/>");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    asyncContext.complete();
                }
            });

        } catch (Exception e) {
            Log.error(e, "PingManagedThreadServlet.doGet(...): general exception caught");
            res.sendError(500, e.toString());
        }

    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Tests a ManagedThread asynchronous servlet";
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
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
        initTime = new java.util.Date().toString();
        hitCount = 0;

    }

}
