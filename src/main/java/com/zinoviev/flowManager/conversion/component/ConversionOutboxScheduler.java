package com.zinoviev.flowManager.conversion.component;

import com.zinoviev.flowManager.conversion.service.ConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConversionOutboxScheduler {

    private final ConversionService conversionService;

    @Scheduled(cron = "${outbox.conversion.sending-period-cron}")
    public void sendTasksToKafkaBySchedule() throws ExecutionException, InterruptedException {
        log.info("Планировщик Outbox начинает отправку в Kafka");
        conversionService.sendTasksToKafka();
    }

}
