package com.zinoviev.flowManager.conversion.messaging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaConsumerProperties {

    private Map<String, ConsumerConfig> consumers;

    @Data
    public static class ConsumerConfig {
        private String topic;
        private String groupId;
        private String trustedPackages;
        private TypeMapping typeMapping;
        private String defaultType;
    }

    @Data
    public static class TypeMapping {
        private String source;
        private String target;
    }
}