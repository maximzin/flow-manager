package com.zinoviev.flowManager.conversion.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("local")
public class LocalKafkaTopicConfig {

    @Value("${topic.conversion.created.events}")
    private String conversionCreatedEventsTopicName;

    @Value("${topic.conversion.processed.events}")
    private String conversionProcessedEventsTopicName;

    @Bean
    NewTopic createConversionCreateEventsTopic() {
        return TopicBuilder.name(conversionCreatedEventsTopicName)
                .partitions(1)
                .build();
    }

    @Bean
    NewTopic createConversionProcessedEventsTopic() {
        return TopicBuilder.name(conversionProcessedEventsTopicName)
                .partitions(1)
                .build();
    }

}
