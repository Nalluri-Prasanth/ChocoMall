package com.chocomall.model.repository;

import com.chocomall.model.entity.IntegrationHistory;
import com.chocomall.model.enums.IntegrationChangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationHistoryRepository extends JpaRepository<IntegrationHistory, Long> {

    Page<IntegrationHistory> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);

    boolean existsByOrderSnAndChangeType(String orderSn, IntegrationChangeType changeType);
}
