package com.chocomall.model.repository;

import com.chocomall.model.entity.MemberAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberAddressRepository extends JpaRepository<MemberAddress, Long> {

    List<MemberAddress> findByMemberId(Long memberId);

    Optional<MemberAddress> findByIdAndMemberId(Long id, Long memberId);

    @Modifying
    @Query("update MemberAddress a set a.defaultStatus = false where a.memberId = :memberId")
    void clearDefault(@Param("memberId") Long memberId);
}
