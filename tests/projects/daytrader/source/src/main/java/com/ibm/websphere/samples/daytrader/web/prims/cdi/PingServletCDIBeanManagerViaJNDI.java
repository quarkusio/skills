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
package com.ibm.websphere.samples.daytrader.web.prims.cdi;

import java.io.IOException;
import java.io.PrintWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@WebServlet("/servlet/PingServletCDIBeanManagerViaJNDI")
public class PingServletCDIBeanManagerViaJNDI extends HttpServlet {

  private static final long serialVersionUID = -1803544618879689949L;
  private static String initTime;

  @Autowired
  PingCDIBean cdiBean;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    PrintWriter pw = response.getWriter();
    pw.write("<html><head><title>Ping Servlet CDI Bean Manager</title></head>"
        + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet CDI Bean Manager<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
        + initTime
        + "<BR><BR></FONT>");

    try {
      pw.write("<B>hitCount: " + cdiBean.getBeanMangerViaJNDI() + "</B></body></html>");
    } catch (Exception e) {
      e.printStackTrace();
    }

    pw.flush();
    pw.close();

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
    org.springframework.web.context.support.SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this,
        config.getServletContext());
    initTime = new java.util.Date().toString();

  }
}
