package com.chocomall.model.repository;

import com.chocomall.model.entity.ReturnApply;
import com.chocomall.model.enums.ReturnStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReturnApplyRepository extends JpaRepository<ReturnApply, Long> {

    @Query("select r from ReturnApply r where (:status is null or r.status = :status) and (:orderSn is null or r.orderSn = :orderSn) and (:memberName is null or r.memberUsername like concat('%', :memberName, '%'))")
    Page<ReturnApply> search(@Param("status") ReturnStatus status, @Param("orderSn") String orderSn, @Param("memberName") String member, Pageable pageable);

    List<ReturnApply> findByMemberIdOrderByIdDesc(Long memberId);

    boolean existsByOrderIdAndProductIdAndStatusIn(Long orderId, Long productId, Collection<ReturnStatus> statuses);
}
