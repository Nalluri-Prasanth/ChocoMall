package com.chocomall.model.repository;

import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

    boolean existsByProductSkuId(Long productSkuId);

    @Query("select count(i) > 0 from OrderItem i join ShopOrder o on o.id = i.orderId where o.memberId = :mid and i.productId = :pid and o.status = com.chocomall.model.enums.OrderStatus.COMPLETED")
    boolean hasCompletedPurchase(@Param("mid") Long memberId, @Param("pid") Long productId);

    @Query("select count(i) > 0 from OrderItem i join ShopOrder o on o.id = i.orderId where i.productId = :pid and o.status in (com.chocomall.model.enums.OrderStatus.PENDING_PAYMENT, com.chocomall.model.enums.OrderStatus.PREPARING, com.chocomall.model.enums.OrderStatus.READY_FOR_PICKUP, com.chocomall.model.enums.OrderStatus.SHIPPED)")
    boolean hasOpenOrder(@Param("pid") Long productId);

    @Query("select i.productId, i.productName, sum(i.productQuantity * i.unitsPerSku), sum(i.realAmount) from OrderItem i join ShopOrder o on o.id = i.orderId where o.status in (com.chocomall.model.enums.OrderStatus.PREPARING, com.chocomall.model.enums.OrderStatus.READY_FOR_PICKUP, com.chocomall.model.enums.OrderStatus.SHIPPED, com.chocomall.model.enums.OrderStatus.COMPLETED) and o.paymentTime >= :start and o.paymentTime < :end group by i.productId, i.productName order by sum(i.realAmount) desc")
    List<Object[]> productSales(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
