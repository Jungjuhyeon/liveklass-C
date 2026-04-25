package com.jung.notificationservice.infra.scheduler;

import com.jung.notificationservice.application.usecase.PollNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPollingWorker {

    private final PollNotificationsUseCase pollNotificationsUseCase;

    @Scheduled(fixedDelay = 10_000)
    public void poll() {
        log.debug("[PollingWorker] 폴링 실행");
        pollNotificationsUseCase.pollPending();
    }
}
