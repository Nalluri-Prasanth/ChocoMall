package com.chocomall.core.order;

import com.chocomall.core.experience.ExperienceBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Safety nets that keep orders and bookings moving even when the message broker is absent or a message was lost. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "chocomall.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class OrderMaintenanceJobs {

    private final OrderLifecycleService lifecycle;
    private final ExperienceBookingService bookings;

    @Scheduled(fixedDelayString = "${chocomall.jobs.expire-orders-delay-ms:60000}", initialDelayString = "${chocomall.jobs.initial-delay-ms:30000}")
    public void cancelExpiredOrders() {
        int cancelled = lifecycle.cancelExpired();
        int released = bookings.expirePending();
        if (cancelled > 0 || released > 0) {
            log.info("Cancelled {} unpaid order(s) and released {} unpaid booking(s)", cancelled, released);
        }
    }

    @Scheduled(cron = "${chocomall.jobs.auto-complete-cron:0 0 * * * *}")
    public void autoCompleteOrders() {
        int completed = lifecycle.autoComplete();
        if (completed > 0) {
            log.info("Auto-completed {} order(s)", completed);
        }
    }
}
