package com.chocomall.model.repository;

import com.chocomall.model.entity.ProductionBatch;
import com.chocomall.model.enums.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {

    @Query("select b from ProductionBatch b where (:pid is null or b.productId = :pid) and (:status is null or b.status = :status)")
    Page<ProductionBatch> search(@Param("pid") Long productId, @Param("status") BatchStatus status, Pageable pageable);

    @Query("select coalesce(sum(b.wasteUnits), 0) from ProductionBatch b where b.productId = :pid")
    long totalWaste(@Param("pid") Long productId);

    @Query("select coalesce(sum(b.quantityUnits), 0) from ProductionBatch b where b.productId = :pid and b.status = com.chocomall.model.enums.BatchStatus.RELEASED")
    long totalProduced(@Param("pid") Long productId);
}
