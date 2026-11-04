package com.chocomall.core.messaging;

import com.chocomall.core.order.OrderLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ topology and adapters ({@code chocomall.messaging.type=rabbit}).
 *
 * <p>Order timeout uses the delayed-message pattern: a message with a per-message TTL waits in the delay queue;
 * when it expires it is dead-lettered to the cancel queue, whose consumer cancels the order if it is still unpaid.
 */
@Configuration
@ConditionalOnProperty(name = "chocomall.messaging.type", havingValue = "rabbit")
public class RabbitMessaging {

    public static final String CANCEL_EXCHANGE = "chocomall.order.cancel";
    public static final String CANCEL_QUEUE = "chocomall.order.cancel.queue";
    public static final String CANCEL_KEY = "order.cancel";
    public static final String DELAY_EXCHANGE = "chocomall.order.delay";
    public static final String DELAY_QUEUE = "chocomall.order.delay.queue";
    public static final String DELAY_KEY = "order.delay";
    public static final String SEARCH_EXCHANGE = "chocomall.search";
    public static final String SEARCH_QUEUE = "chocomall.search.sync";
    public static final String SEARCH_KEY = "product.sync";

    @Bean
    DirectExchange orderCancelExchange() {
        return new DirectExchange(CANCEL_EXCHANGE, true, false);
    }

    @Bean
    Queue orderCancelQueue() {
        return QueueBuilder.durable(CANCEL_QUEUE).build();
    }

    @Bean
    Binding orderCancelBinding() {
        return BindingBuilder.bind(orderCancelQueue()).to(orderCancelExchange()).with(CANCEL_KEY);
    }

    @Bean
    DirectExchange orderDelayExchange() {
        return new DirectExchange(DELAY_EXCHANGE, true, false);
    }

    @Bean
    Queue orderDelayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", CANCEL_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CANCEL_KEY)
                .build();
    }

    @Bean
    Binding orderDelayBinding() {
        return BindingBuilder.bind(orderDelayQueue()).to(orderDelayExchange()).with(DELAY_KEY);
    }

    @Bean
    DirectExchange searchExchange() {
        return new DirectExchange(SEARCH_EXCHANGE, true, false);
    }

    @Bean
    Queue searchQueue() {
        return QueueBuilder.durable(SEARCH_QUEUE).build();
    }

    @Bean
    Binding searchBinding() {
        return BindingBuilder.bind(searchQueue()).to(searchExchange()).with(SEARCH_KEY);
    }

    @Bean
    OrderTimeoutPublisher rabbitOrderTimeoutPublisher(RabbitTemplate template) {
        return (orderId, delayMillis) -> template.convertAndSend(DELAY_EXCHANGE, DELAY_KEY, String.valueOf(orderId), message -> {
            message.getMessageProperties().setExpiration(String.valueOf(delayMillis));
            return message;
        });
    }

    @Bean
    SearchSyncPublisher rabbitSearchSyncPublisher(RabbitTemplate template) {
        return new SearchSyncPublisher() {
            @Override
            public void productChanged(Long productId) {
                template.convertAndSend(SEARCH_EXCHANGE, SEARCH_KEY, "changed:" + productId);
            }

            @Override
            public void productRemoved(Long productId) {
                template.convertAndSend(SEARCH_EXCHANGE, SEARCH_KEY, "removed:" + productId);
            }
        };
    }

    /** Consumes expired delay messages. Idempotent: an order that was paid or cancelled meanwhile is left alone. */
    @Slf4j
    @Component
    @ConditionalOnProperty(name = "chocomall.messaging.type", havingValue = "rabbit")
    @RequiredArgsConstructor
    static class OrderCancelConsumer {

        private final OrderLifecycleService lifecycle;

        @RabbitListener(queues = CANCEL_QUEUE)
        void onExpired(String orderId) {
            log.info("Payment window expired for order {}", orderId);
            lifecycle.cancelIfUnpaid(Long.parseLong(orderId));
        }
    }
}
