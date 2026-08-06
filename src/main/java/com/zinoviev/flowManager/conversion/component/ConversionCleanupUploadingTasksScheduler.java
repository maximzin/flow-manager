package com.zinoviev.flowManager.conversion.component;

import com.zinoviev.flowManager.conversion.service.conversion.ConversionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ConversionCleanupUploadingTasksScheduler {

    private final ConversionService conversionService;

    @Scheduled(cron = "${conversion.uploading-tasks.cleanup-period-cron}")
    public void cleanupUploadingTasks() {
        log.info("Планировщик ConversionCleanupUploadingTasks начинает работу");
        conversionService.cleanupUploadingTasks();
    }

}
