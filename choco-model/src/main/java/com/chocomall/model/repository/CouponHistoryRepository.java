package com.chocomall.model.repository;

import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.enums.CouponUseStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponHistoryRepository extends JpaRepository<CouponHistory, Long> {

    List<CouponHistory> findByMemberId(Long memberId);

    List<CouponHistory> findByMemberIdAndUseStatus(Long memberId, CouponUseStatus useStatus);

    long countByCouponIdAndMemberId(Long couponId, Long memberId);

    List<CouponHistory> findByOrderId(Long orderId);

    Optional<CouponHistory> findByIdAndMemberId(Long id, Long memberId);

    @Query("select h from CouponHistory h where (:couponId is null or h.couponId = :couponId) and (:status is null or h.useStatus = :status) and (:orderSn is null or h.orderSn = :orderSn)")
    Page<CouponHistory> search(@Param("couponId") Long couponId, @Param("status") CouponUseStatus status, @Param("orderSn") String orderSn, Pageable pageable);
}
