package com.chocomall.model.repository;

import com.chocomall.model.entity.Coupon;
import com.chocomall.model.enums.CouponType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("select c from Coupon c where (:kw is null or c.name like concat('%', :kw, '%')) and (:ctype is null or c.couponType = :ctype)")
    Page<Coupon> search(@Param("kw") String keyword, @Param("ctype") CouponType type, Pageable pageable);

    List<Coupon> findByStatusTrueAndStartTimeBeforeAndEndTimeAfter(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("update Coupon c set c.receivedCount = c.receivedCount + 1 where c.id = :id and c.receivedCount < c.totalCount")
    int incrementReceived(@Param("id") Long id);

    @Modifying
    @Query("update Coupon c set c.usedCount = c.usedCount + :delta where c.id = :id")
    int addUsed(@Param("id") Long id, @Param("delta") int delta);
}
