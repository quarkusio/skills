package com.coffeeshop.counter.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;

import static com.coffeeshop.common.messaging.Topics.*;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    // --- Producer (String key/value)
    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaProperties props) {
        Map<String, Object> cfg = props.buildProducerProperties(null);
        cfg.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        cfg.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(cfg);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> pf) {
        return new KafkaTemplate<>(pf);
    }

    // --- Consumer defaults are taken from application.properties via Spring Boot
    // If you later need a custom ConsumerFactory, you can define it similarly.

    // --- Auto-create topics (optional; remove if your cluster pre-creates them)
    @Bean public NewTopic webUpdates() { return TopicBuilder.name(WEB_UPDATES).partitions(3).replicas(1).build(); }
    @Bean public NewTopic baristaIn()  { return TopicBuilder.name(BARISTA_IN).partitions(3).replicas(1).build(); }
    @Bean public NewTopic kitchenIn()  { return TopicBuilder.name(KITCHEN_IN).partitions(3).replicas(1).build(); }
    @Bean public NewTopic ordersUp()   { return TopicBuilder.name(ORDERS_UP).partitions(3).replicas(1).build(); }
}
