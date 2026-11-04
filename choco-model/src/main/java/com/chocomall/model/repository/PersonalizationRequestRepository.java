package com.chocomall.model.repository;

import com.chocomall.model.entity.PersonalizationRequest;
import com.chocomall.model.enums.PersonalizationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalizationRequestRepository extends JpaRepository<PersonalizationRequest, Long> {

    List<PersonalizationRequest> findByMemberIdOrderByIdDesc(Long memberId);

    Optional<PersonalizationRequest> findByIdAndMemberId(Long id, Long memberId);

    @Query("select r from PersonalizationRequest r where (:status is null or r.status = :status)")
    Page<PersonalizationRequest> search(@Param("status") PersonalizationStatus status, Pageable pageable);
}
