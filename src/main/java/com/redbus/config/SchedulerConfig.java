package com.redbus.config;

import com.redbus.service.IdempotencyStore;
import com.redbus.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {
    private final OrderService orderService;
    private final IdempotencyStore idempotencyStore;

    @Scheduled(fixedRate = 5000) // Every 5 seconds
    public void expireReservedBookings() {
        try {
            orderService.expireReservedBookings();
        } catch (Exception e) {
            log.error("Error expiring reserved bookings", e);
        }
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredIdempotencyKeys() {
        try {
            idempotencyStore.removeExpired();
        } catch (Exception e) {
            log.error("Error cleaning up expired idempotency keys", e);
        }
    }
}
