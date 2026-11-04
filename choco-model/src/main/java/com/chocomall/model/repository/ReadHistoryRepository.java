package com.chocomall.model.repository;

import com.chocomall.model.entity.ReadHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadHistoryRepository extends JpaRepository<ReadHistory, Long> {

    Page<ReadHistory> findByMemberIdOrderByViewTimeDesc(Long memberId, Pageable pageable);

    Optional<ReadHistory> findByMemberIdAndProductId(Long memberId, Long productId);

    void deleteByMemberId(Long memberId);
}
