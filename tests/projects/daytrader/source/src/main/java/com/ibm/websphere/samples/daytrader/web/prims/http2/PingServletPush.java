/**
 * (C) Copyright IBM Corporation 2019.
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
package com.ibm.websphere.samples.daytrader.web.prims.http2;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.ibm.websphere.samples.daytrader.util.Log;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.PushBuilder;

@Component
@WebServlet(name = "PingServletPush", urlPatterns = { "/PingServletPush" })
public class PingServletPush extends HttpServlet {

  private static final long serialVersionUID = -1687383294950455998L;
  private static String initTime;
  private static int hitCount;

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    try {
      PushBuilder pushBuilder = req.newPushBuilder();
      if (pushBuilder != null) {
        pushBuilder
            .path("images/graph.gif")
            .push();

      } else {
        Log.error("HTTP/2 not enabled or Push not supported");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try (PrintWriter respWriter = resp.getWriter();) {
      hitCount++;
      // System.out.println("Sending hit count: " + hitCount);
      respWriter.write("<html><head><title>Ping Servlet HTTP/2</title></head>"
          + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet HTTP/2<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
          + initTime
          + "<BR><BR></FONT>  <B>Hit Count: " + hitCount + "</B><br>" +
          "<img src='images/graph.gif'>" +
          "</body></html>");
    }
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    initTime = new java.util.Date().toString();
    hitCount = 0;
  }
}
