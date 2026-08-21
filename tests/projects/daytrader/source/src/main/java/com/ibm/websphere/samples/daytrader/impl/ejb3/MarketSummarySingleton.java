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
package com.ibm.websphere.samples.daytrader.impl.ejb3;

import com.ibm.websphere.samples.daytrader.beans.MarketSummaryDataBean;
import com.ibm.websphere.samples.daytrader.entities.QuoteDataBean;
import com.ibm.websphere.samples.daytrader.events.MarketSummaryUpdateEvent;
import com.ibm.websphere.samples.daytrader.util.FinancialUtils;
import com.ibm.websphere.samples.daytrader.util.Log;
import com.ibm.websphere.samples.daytrader.util.TradeConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketSummarySingleton {

    private MarketSummaryDataBean marketSummaryDataBean;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReadLock readLock = rwLock.readLock();
    private final WriteLock writeLock = rwLock.writeLock();

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    ApplicationEventPublisher publisher;

    /* Update Market Summary every 20 seconds */
    @Scheduled(cron = "*/20 * * * * *")
    @Transactional(readOnly = true)
    private void updateMarketSummary() {
        Log.trace(
            "MarketSummarySingleton:updateMarketSummary -- updating market summary"
        );

        if (TradeConfig.getRunTimeMode() != TradeConfig.EJB3) {
            Log.trace(
                "MarketSummarySingleton:updateMarketSummary -- Not EJB3 Mode, so not updating"
            );
            return; // Only do the actual work if in EJB3 Mode
        }

        List<QuoteDataBean> quotes;

        try {
            // Find Trade Stock Index Quotes (Top 100 quotes) ordered by their change in
            // value
            CriteriaBuilder criteriaBuilder =
                entityManager.getCriteriaBuilder();
            CriteriaQuery<QuoteDataBean> criteriaQuery =
                criteriaBuilder.createQuery(QuoteDataBean.class);
            Root<QuoteDataBean> quoteRoot = criteriaQuery.from(
                QuoteDataBean.class
            );
            criteriaQuery.orderBy(
                criteriaBuilder.desc(quoteRoot.get("change1"))
            );
            criteriaQuery.select(quoteRoot);
            TypedQuery<QuoteDataBean> q = entityManager.createQuery(
                criteriaQuery
            );
            quotes = q.getResultList();
        } catch (Exception e) {
            Log.debug(
                "Warning: The database has not been configured. If this is the first time the application has been started, please create and populate the database tables. Then restart the server."
            );
            return;
        }

        QuoteDataBean[] quoteArray = quotes.toArray(QuoteDataBean[]::new);
        ArrayList<QuoteDataBean> topGainers = new ArrayList<>(5);
        ArrayList<QuoteDataBean> topLosers = new ArrayList<>(5);
        BigDecimal TSIA = FinancialUtils.ZERO;
        BigDecimal openTSIA = FinancialUtils.ZERO;
        double totalVolume = 0.0;

        if (quoteArray.length > 5) {
            for (int i = 0; i < 5; i++) {
                topGainers.add(quoteArray[i]);
            }
            for (
                int i = quoteArray.length - 1;
                i >= quoteArray.length - 5;
                i--
            ) {
                topLosers.add(quoteArray[i]);
            }

            for (QuoteDataBean quote : quoteArray) {
                BigDecimal price = quote.getPrice();
                BigDecimal open = quote.getOpen();
                double volume = quote.getVolume();
                TSIA = TSIA.add(price);
                openTSIA = openTSIA.add(open);
                totalVolume += volume;
                TSIA = TSIA.divide(
                    new BigDecimal(quoteArray.length),
                    RoundingMode.HALF_UP
                );
                openTSIA = openTSIA.divide(
                    new BigDecimal(quoteArray.length),
                    RoundingMode.HALF_UP
                );
                openTSIA = openTSIA.divide(
                    new BigDecimal(quoteArray.length),
                    RoundingMode.HALF_UP
                );
            }

            setMarketSummaryDataBean(
                new MarketSummaryDataBean(
                    TSIA,
                    openTSIA,
                    totalVolume,
                    topGainers,
                    topLosers
                )
            );
            publisher.publishEvent(
                new MarketSummaryUpdateEvent("MarketSummaryUpdate")
            );
        }
    }

    public MarketSummaryDataBean getMarketSummaryDataBean() {
        readLock.lock();
        if (marketSummaryDataBean == null) {
            updateMarketSummary();
        }

        return marketSummaryDataBean;
    }

    public void setMarketSummaryDataBean(
        MarketSummaryDataBean marketSummaryDataBean
    ) {
        writeLock.lock();
        this.marketSummaryDataBean = marketSummaryDataBean;
    }
}
