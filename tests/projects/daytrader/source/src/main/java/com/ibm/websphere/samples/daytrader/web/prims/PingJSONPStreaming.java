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
import java.io.StringReader;
import java.io.StringWriter;

import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.ibm.websphere.samples.daytrader.util.Log;

import jakarta.json.Json;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * PingJSONP tests JSON generating and parsing
 *
 */

@Component
@WebServlet(name = "PingJSONPStreaming", urlPatterns = { "/servlet/PingJSONPStreaming" })
public class PingJSONPStreaming extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -5348806619121122708L;
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
        try {
            res.setContentType("text/html");

            ServletOutputStream out = res.getOutputStream();

            hitCount++;

            // JSON generate
            StringWriter sw = new StringWriter();
            JsonGenerator generator = Json.createGenerator(sw);

            generator.writeStartObject();
            generator.write("initTime", initTime);
            generator.write("hitCount", hitCount);
            generator.writeEnd();
            generator.flush();

            String generatedJSON = sw.toString();
            StringBuffer parsedJSON = new StringBuffer();

            // JSON parse
            JsonParser parser = Json.createParser(new StringReader(generatedJSON));
            while (parser.hasNext()) {
                JsonParser.Event event = parser.next();
                switch (event) {
                    case START_ARRAY:
                    case END_ARRAY:
                    case START_OBJECT:
                    case END_OBJECT:
                    case VALUE_FALSE:
                    case VALUE_NULL:
                    case VALUE_TRUE:
                        break;
                    case KEY_NAME:
                        parsedJSON.append(parser.getString() + ":");
                        break;
                    case VALUE_STRING:
                    case VALUE_NUMBER:
                        parsedJSON.append(parser.getString() + " ");
                        break;
                }
            }

            out.println("<html><head><title>Ping JSONP</title></head>"
                    + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping JSONP</FONT><BR>Generated JSON: "
                    + generatedJSON + "<br>Parsed JSON: " + parsedJSON + "</body></html>");
        } catch (Exception e) {
            Log.error(e, "PingJSONP.doGet(...): general exception caught");
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
        return "Basic JSON generation and parsing in a servlet";
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