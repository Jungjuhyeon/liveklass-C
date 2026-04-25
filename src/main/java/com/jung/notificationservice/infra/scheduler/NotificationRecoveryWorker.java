package com.jung.notificationservice.infra.scheduler;

import com.jung.notificationservice.application.usecase.RecoverNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRecoveryWorker {

    private final RecoverNotificationsUseCase recoverNotificationsUseCase;

    @Scheduled(fixedDelay = 300_000)
    public void recover() {
        log.debug("[RecoveryWorker] 복구 실행");
        recoverNotificationsUseCase.recoverStuck();
    }
}
