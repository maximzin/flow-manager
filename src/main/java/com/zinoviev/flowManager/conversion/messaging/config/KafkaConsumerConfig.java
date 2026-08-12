package com.zinoviev.flowManager.conversion.messaging.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private final KafkaConsumerProperties properties;

    public KafkaConsumerConfig(KafkaConsumerProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    conversionEventsKafkaListenerContainerFactory() {
        return createFactory("conversion-events");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    subscriptionEventsKafkaListenerContainerFactory() {
        return createFactory("subscription-events");
    }

    private ConcurrentKafkaListenerContainerFactory<String, Object>
    createFactory(String consumerName) {

        KafkaConsumerProperties.ConsumerConfig config = properties.getConsumers().get(consumerName);

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, config.getGroupId());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, config.getTrustedPackages());
        props.put(JacksonJsonDeserializer.TYPE_MAPPINGS,
                config.getTypeMapping().getSource() + ":" + config.getTypeMapping().getTarget());
        props.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, config.getDefaultType());

        ConsumerFactory<String, Object> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        return factory;
    }
}