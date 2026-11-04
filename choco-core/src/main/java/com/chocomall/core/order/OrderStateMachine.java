package com.chocomall.core.order;

import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The allowed life of an order.
 * <pre>
 * PENDING_PAYMENT -> PREPARING -> READY_FOR_PICKUP -> COMPLETED      (pickup)
 *                              -> SHIPPED          -> COMPLETED      (delivery)
 * PENDING_PAYMENT, PREPARING -> CANCELLED
 * PENDING_PAYMENT, CANCELLED -> INVALID
 * </pre>
 */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(OrderStatus.class);

    static {
        NEXT.put(OrderStatus.PENDING_PAYMENT, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED, OrderStatus.INVALID));
        NEXT.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        NEXT.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.COMPLETED));
        NEXT.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.COMPLETED));
        NEXT.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        NEXT.put(OrderStatus.CANCELLED, EnumSet.of(OrderStatus.INVALID));
        NEXT.put(OrderStatus.INVALID, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStateMachine() {
    }

    public static boolean canMove(OrderStatus from, OrderStatus to, FulfillmentType fulfillment) {
        if (!NEXT.get(from).contains(to)) {
            return false;
        }
        // a pickup order is never shipped, and a delivery order is never "ready for pickup"
        if (to == OrderStatus.SHIPPED && fulfillment != FulfillmentType.DELIVERY) {
            return false;
        }
        return !(to == OrderStatus.READY_FOR_PICKUP && fulfillment != FulfillmentType.PICKUP);
    }

    public static boolean isPaid(OrderStatus status) {
        return status == OrderStatus.PREPARING || status == OrderStatus.READY_FOR_PICKUP
                || status == OrderStatus.SHIPPED || status == OrderStatus.COMPLETED;
    }
}
