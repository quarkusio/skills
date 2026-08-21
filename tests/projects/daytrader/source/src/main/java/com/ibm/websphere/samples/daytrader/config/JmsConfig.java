package com.ibm.websphere.samples.daytrader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

import jakarta.jms.ConnectionFactory;

/**
 * Replaces Liberty JMS objects (wasJmsServer/Client, queues, topics).
 * We use embedded Artemis and configure separate factories for queue vs topic.
 */
@EnableJms
@Configuration
public class JmsConfig {
	@Bean
	public DefaultJmsListenerContainerFactory queueFactory(
			ConnectionFactory cf) {
		DefaultJmsListenerContainerFactory f = new DefaultJmsListenerContainerFactory();
		f.setConnectionFactory(cf);
		f.setPubSubDomain(false);
		f.setSessionTransacted(true);
		return f;
	}

	@Bean
	public DefaultJmsListenerContainerFactory topicFactory(
			ConnectionFactory cf) {
		DefaultJmsListenerContainerFactory f = new DefaultJmsListenerContainerFactory();
		f.setConnectionFactory(cf);
		f.setPubSubDomain(true);
		return f;
	}
}