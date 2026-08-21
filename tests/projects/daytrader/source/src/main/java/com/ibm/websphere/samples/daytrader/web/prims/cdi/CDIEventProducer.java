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
package com.ibm.websphere.samples.daytrader.web.prims.cdi;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import com.ibm.websphere.samples.daytrader.events.HitAsyncEvent;
import com.ibm.websphere.samples.daytrader.events.HitEvent;

@Component
public class CDIEventProducer {

  private final ApplicationEventPublisher publisher;
  private final AsyncTaskExecutor mes;

  public CDIEventProducer(ApplicationEventPublisher publisher,
      @Qualifier("ManagedExecutorService") AsyncTaskExecutor mes) {
    this.publisher = publisher;
    this.mes = mes;
  }

  public void produceSyncEvent() {
    publisher.publishEvent(new HitEvent("hitCount++"));
  }

  public void produceAsyncEvent() {
    mes.submit(() -> publisher.publishEvent(new HitAsyncEvent("hitCount++")));
  }

}
