package com.chocomall.model.repository;

import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long>, JpaSpecificationExecutor<ShopOrder> {

    Optional<ShopOrder> findByOrderSn(String orderSn);

    Optional<ShopOrder> findByIdAndMemberId(Long id, Long memberId);

    Page<ShopOrder> findByMemberIdAndDeletedFalse(Long memberId, Pageable pageable);

    Page<ShopOrder> findByMemberIdAndStatusAndDeletedFalse(Long memberId, OrderStatus status, Pageable pageable);

    List<ShopOrder> findByStatusAndCreateTimeBefore(OrderStatus status, LocalDateTime before);

    List<ShopOrder> findByStatusInAndDeliveryTimeBefore(Collection<OrderStatus> statuses, LocalDateTime before);

    long countByStatus(OrderStatus status);

    long countByMemberIdAndStatusAndDeletedFalse(Long memberId, OrderStatus status);

    @Query("select coalesce(sum(o.payAmount), 0) from ShopOrder o where o.status in (com.chocomall.model.enums.OrderStatus.PREPARING, com.chocomall.model.enums.OrderStatus.READY_FOR_PICKUP, com.chocomall.model.enums.OrderStatus.SHIPPED, com.chocomall.model.enums.OrderStatus.COMPLETED) and o.paymentTime >= :start and o.paymentTime < :end")
    BigDecimal revenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select count(o) from ShopOrder o where o.status in (com.chocomall.model.enums.OrderStatus.PREPARING, com.chocomall.model.enums.OrderStatus.READY_FOR_PICKUP, com.chocomall.model.enums.OrderStatus.SHIPPED, com.chocomall.model.enums.OrderStatus.COMPLETED) and o.paymentTime >= :start and o.paymentTime < :end")
    long paidCountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select cast(o.paymentTime as date), count(o), sum(o.payAmount) from ShopOrder o where o.status in (com.chocomall.model.enums.OrderStatus.PREPARING, com.chocomall.model.enums.OrderStatus.READY_FOR_PICKUP, com.chocomall.model.enums.OrderStatus.SHIPPED, com.chocomall.model.enums.OrderStatus.COMPLETED) and o.paymentTime >= :start and o.paymentTime < :end group by cast(o.paymentTime as date) order by cast(o.paymentTime as date)")
    List<Object[]> dailySales(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
