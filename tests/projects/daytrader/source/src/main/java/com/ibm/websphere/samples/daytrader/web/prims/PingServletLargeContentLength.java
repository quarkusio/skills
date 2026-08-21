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

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * PingServletSetContentLength tests fundamental dynamic HTML creation
 * functionality through server side servlet processing.
 *
 */

@Component
@WebServlet(name = "PingServletLargeContentLength", urlPatterns = { "/servlet/PingServletLargeContentLength" })
public class PingServletLargeContentLength extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -7979576220528252408L;

    /**
     * forwards post requests to the doGet method Creation date: (02/07/2013
     * 10:52:39 AM)
     *
     * @param res
     *             jakarta.servlet.http.HttpServletRequest
     * @param res2
     *             jakarta.servlet.http.HttpServletResponse
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        System.out.println("Length: " + req.getContentLengthLong());

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
        doPost(req, res);
    }

    /**
     * returns a string of information about the servlet
     *
     * @return info String: contains info about the servlet
     **/
    @Override
    public String getServletInfo() {
        return "Basic dynamic HTML generation through a servlet, with "
                + "contentLength set by contentLength parameter.";
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
    }
}