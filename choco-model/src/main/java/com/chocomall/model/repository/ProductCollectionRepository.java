package com.chocomall.model.repository;

import com.chocomall.model.entity.ProductCollection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCollectionRepository extends JpaRepository<ProductCollection, Long> {

    Page<ProductCollection> findByMemberIdOrderByCreateTimeDesc(Long memberId, Pageable pageable);

    Optional<ProductCollection> findByMemberIdAndProductId(Long memberId, Long productId);

    void deleteByMemberIdAndProductId(Long memberId, Long productId);

    void deleteByMemberId(Long memberId);
}
