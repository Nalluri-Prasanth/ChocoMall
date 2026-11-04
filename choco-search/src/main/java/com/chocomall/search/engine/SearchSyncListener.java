package com.chocomall.search.engine;

import com.chocomall.core.messaging.RabbitMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Keeps Elasticsearch current: the back office publishes {@code changed:<id>} / {@code removed:<id>} when a product
 * is edited, published or deleted. Only active when both Elasticsearch and RabbitMQ are enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${search.engine:database}' == 'elasticsearch' and '${chocomall.messaging.type:none}' == 'rabbit'")
public class SearchSyncListener {

    private final SearchEngine engine;

    @RabbitListener(queues = RabbitMessaging.SEARCH_QUEUE)
    public void onProductEvent(String message) {
        String[] parts = message.split(":", 2);
        if (parts.length != 2) {
            log.warn("Ignoring malformed search sync message '{}'", message);
            return;
        }
        long id = Long.parseLong(parts[1]);
        if ("removed".equals(parts[0])) {
            engine.remove(id);
        } else {
            engine.index(id);
        }
    }
}
