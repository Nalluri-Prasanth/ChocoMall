package com.chocomall.model.repository;

import com.chocomall.model.entity.MemberLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberLevelRepository extends JpaRepository<MemberLevel, Long> {

    Optional<MemberLevel> findFirstByDefaultStatusTrue();

    List<MemberLevel> findAllByOrderByGrowthPointAsc();
}
