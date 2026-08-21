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
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@WebServlet("/servlet/PingServletCDI")
public class PingServletCDI extends HttpServlet {

  private static final long serialVersionUID = -1803544618879689949L;
  private static String initTime;

  @Autowired
  PingCDIBean cdiBean;

  @Autowired
  PingEJBIFace ejb;

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    PrintWriter pw = response.getWriter();
    pw.write("<html><head><title>Ping Servlet CDI</title></head>"
        + "<body><HR><BR><FONT size=\"+2\" color=\"#000066\">Ping Servlet CDI<BR></FONT><FONT size=\"+1\" color=\"#000066\">Init time : "
        + initTime
        + "<BR><BR></FONT>");

    pw.write("<B>hitCount: " + cdiBean.hello() + "</B><BR>");
    pw.write("<B>hitCount: " + ejb.getMsg() + "</B><BR>");

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
    SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    initTime = new java.util.Date().toString();

  }
}
