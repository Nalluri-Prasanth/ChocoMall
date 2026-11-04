package com.chocomall.model.repository;

import com.chocomall.model.entity.ProductComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCommentRepository extends JpaRepository<ProductComment, Long> {

    Page<ProductComment> findByProductIdAndShowStatusTrue(Long productId, Pageable pageable);

    Page<ProductComment> findByProductId(Long productId, Pageable pageable);

    boolean existsByProductIdAndMemberId(Long productId, Long memberId);

    @Query("select avg(c.star) from ProductComment c where c.productId = :pid and c.showStatus = true")
    Double averageStar(@Param("pid") Long productId);
}
