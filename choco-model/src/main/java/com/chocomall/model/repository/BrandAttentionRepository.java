package com.chocomall.model.repository;

import com.chocomall.model.entity.BrandAttention;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandAttentionRepository extends JpaRepository<BrandAttention, Long> {

    Page<BrandAttention> findByMemberIdOrderByCreateTimeDesc(Long memberId, Pageable pageable);

    Optional<BrandAttention> findByMemberIdAndBrandId(Long memberId, Long brandId);

    void deleteByMemberIdAndBrandId(Long memberId, Long brandId);

    void deleteByMemberId(Long memberId);
}
