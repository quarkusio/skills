/**
 * (C) Copyright IBM Corporation 2015, 2022.
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
package com.ibm.websphere.samples.daytrader.web.servlet;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.ibm.websphere.samples.daytrader.interfaces.Trace;
import com.ibm.websphere.samples.daytrader.interfaces.TradeServices;
import com.ibm.websphere.samples.daytrader.util.Diagnostics;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

@WebFilter(filterName = "OrdersAlertFilter", urlPatterns = "/app")
@Trace
public class OrdersAlertFilter implements Filter {

    private TradeServices tradeAction;
    
    @Autowired
    private ApplicationContext applicationContext;

    public OrdersAlertFilter() {
        // Default constructor for servlet container
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    private FilterConfig filterConfig = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, filterConfig.getServletContext());
        // Resolve TradeServices implementation by configured runtime mode after autowiring
        String key = TradeConfig.getRunTimeModeNames()[TradeConfig.getRunTimeMode()];
        if (applicationContext != null && applicationContext.containsBean(key)) {
            this.tradeAction = applicationContext.getBean(key, TradeServices.class);
        }
        if (this.tradeAction == null) {
            Map<String, TradeServices> beans = (applicationContext != null)
                    ? applicationContext.getBeansOfType(TradeServices.class)
                    : java.util.Collections.emptyMap();
            Set<String> available = beans.keySet();
            throw new IllegalStateException("No TradeServices bean named '" + key + "' (available: " + available + ")");
        }
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {
        if (filterConfig == null) {
            return;
        }

        if (TradeConfig.getDisplayOrderAlerts() == true) {
            try {
                String action = req.getParameter("action");
                if (action != null) {
                    action = action.trim();
                    if ((action.length() > 0) && (!action.equals("logout"))) {
                        String userID;
                        if (action.equals("login")) {
                            userID = req.getParameter("uid");
                        } else {
                            userID = (String) ((HttpServletRequest) req).getSession().getAttribute(
                                    "uidBean");
                        }

                        if ((userID != null) && (userID.trim().length() > 0)) {
                            Collection<?> closedOrders = tradeAction.getClosedOrders(userID);
                            if ((closedOrders != null) &&
                                    (closedOrders.size() > 0)) {
                                req.setAttribute("closedOrders", closedOrders);
                            }
                            if (Log.doTrace()) {
                                Log.printCollection(
                                        "OrderAlertFilter: userID=" +
                                                userID +
                                                " closedOrders=",
                                        closedOrders);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.error(
                        e,
                        "OrdersAlertFilter - Error checking for closedOrders");
            }
        }

        Diagnostics.checkDiagnostics();

        chain.doFilter(req, resp /* wrapper */);
    }

    /**
     * @see Filter#destroy()
     */
    @Override
    public void destroy() {
        this.filterConfig = null;
    }
}
