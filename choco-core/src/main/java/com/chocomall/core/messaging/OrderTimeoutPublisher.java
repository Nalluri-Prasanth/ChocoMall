package com.chocomall.core.messaging;

/**
 * Schedules the automatic cancellation of an unpaid order. The RabbitMQ implementation uses a delayed message
 * (TTL queue + dead-letter exchange); without a broker the maintenance job still cancels expired orders within
 * a minute, so this is an optimisation, never the only safeguard.
 */
public interface OrderTimeoutPublisher {

    void schedule(Long orderId, long delayMillis);
}
