package com.ibm.websphere.samples.daytrader.impl.ejb3;

import java.time.Instant;
import java.util.concurrent.Future;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class AsyncScheduledOrderSubmitter {

    // --- REMOVE ---
    // @Resource
    // private ManagedScheduledExecutorService mes;

    // +++ ADD +++
    @Autowired
    private TaskScheduler mes;

    // --- REMOVE ---
    // @Inject

    // +++ ADD +++
    @Autowired
    private AsyncScheduledOrder asyncOrder;

    public Future<?> submitOrder(Integer orderID, boolean twoPhase) {
        asyncOrder.setProperties(orderID, twoPhase);
        return mes.schedule(asyncOrder, Instant.now().plusMillis(500));
    }
}
