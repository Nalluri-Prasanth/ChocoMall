package com.chocomall.model.repository;

import com.chocomall.model.entity.MemberPreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, Long> {

    List<MemberPreference> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}
