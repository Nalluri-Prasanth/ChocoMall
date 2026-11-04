package com.chocomall.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.chocomall.core.order.OrderItemUnits;
import com.chocomall.core.order.OrderStateMachine;
import com.chocomall.core.store.InMemoryExpiringStore;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Pure unit tests: no Spring context, no database. */
class UnitTests {

    @Test
    void stateMachineAllowsOnlyTheDocumentedMoves() {
        assertThat(OrderStateMachine.canMove(OrderStatus.PENDING_PAYMENT, OrderStatus.PREPARING, FulfillmentType.PICKUP)).isTrue();
        assertThat(OrderStateMachine.canMove(OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, FulfillmentType.PICKUP)).isTrue();
        assertThat(OrderStateMachine.canMove(OrderStatus.PREPARING, OrderStatus.SHIPPED, FulfillmentType.DELIVERY)).isTrue();
        assertThat(OrderStateMachine.canMove(OrderStatus.PREPARING, OrderStatus.SHIPPED, FulfillmentType.PICKUP)).isFalse();
        assertThat(OrderStateMachine.canMove(OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, FulfillmentType.DELIVERY)).isFalse();
        assertThat(OrderStateMachine.canMove(OrderStatus.COMPLETED, OrderStatus.CANCELLED, FulfillmentType.PICKUP)).isFalse();
        assertThat(OrderStateMachine.canMove(OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED, FulfillmentType.PICKUP)).isFalse();
        assertThat(OrderStateMachine.canMove(OrderStatus.SHIPPED, OrderStatus.PENDING_PAYMENT, FulfillmentType.DELIVERY)).isFalse();
    }

    @Test
    void paidStatusesAreExactlyThoseAfterPayment() {
        assertThat(OrderStateMachine.isPaid(OrderStatus.PENDING_PAYMENT)).isFalse();
        assertThat(OrderStateMachine.isPaid(OrderStatus.CANCELLED)).isFalse();
        assertThat(OrderStateMachine.isPaid(OrderStatus.PREPARING)).isTrue();
        assertThat(OrderStateMachine.isPaid(OrderStatus.COMPLETED)).isTrue();
    }

    @Test
    void aBoxLineConsumesThePiecesOfEachChosenProduct() {
        OrderItemUnits units = new OrderItemUnits(new ObjectMapper());
        OrderItem box = new OrderItem();
        box.setProductId(99L);
        box.setProductQuantity(2); // two boxes
        box.setUnitsPerSku(6);
        box.setBoxSelection("[{\"productId\":1,\"quantity\":4},{\"productId\":2,\"quantity\":2}]");
        OrderItem bar = new OrderItem();
        bar.setProductId(1L);
        bar.setProductQuantity(3);
        bar.setUnitsPerSku(1);

        Map<Long, Integer> result = units.unitsByProduct(List.of(box, bar));

        assertThat(result).containsEntry(1L, 4 * 2 + 3).containsEntry(2L, 2 * 2).doesNotContainKey(99L);
    }

    @Test
    void aPackLineConsumesQuantityTimesPackSize() {
        OrderItemUnits units = new OrderItemUnits(new ObjectMapper());
        OrderItem box12 = new OrderItem();
        box12.setProductId(5L);
        box12.setProductQuantity(2);
        box12.setUnitsPerSku(12);

        assertThat(units.unitsByProduct(List.of(box12))).containsEntry(5L, 24);
    }

    @Test
    void theInMemoryStoreExpiresEntriesAndCountsAtomically() throws Exception {
        InMemoryExpiringStore store = new InMemoryExpiringStore();
        store.put("k", "v", Duration.ofMillis(60));
        assertThat(store.get("k")).contains("v");
        Thread.sleep(120);
        assertThat(store.get("k")).isEmpty();

        assertThat(store.increment("c", Duration.ofMinutes(1))).isEqualTo(1);
        assertThat(store.increment("c", Duration.ofMinutes(1))).isEqualTo(2);
        store.delete("c");
        assertThat(store.increment("c", Duration.ofMinutes(1))).isEqualTo(1);
    }
}
