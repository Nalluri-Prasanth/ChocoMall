package com.chocomall.model.repository;

import com.chocomall.model.entity.Member;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    Optional<Member> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);

    @Modifying
    @Query("update Member m set m.integration = m.integration + :points, m.growth = m.growth + :growth, m.historyIntegration = m.historyIntegration + :history where m.id = :id and m.integration + :points >= 0")
    int addPoints(@Param("id") Long id, @Param("points") int points, @Param("growth") int growth, @Param("history") int history);

    @Query("select m.integration from Member m where m.id = :id")
    Integer integrationOf(@Param("id") Long id);

    @Query("select m.growth from Member m where m.id = :id")
    Integer growthOf(@Param("id") Long id);

    @Modifying
    @Query("update Member m set m.levelId = :levelId where m.id = :id")
    int setLevel(@Param("id") Long id, @Param("levelId") Long levelId);

    @Query("select m from Member m where :kw is null or m.username like concat('%', :kw, '%') or m.nickname like concat('%', :kw, '%') or m.phone like concat('%', :kw, '%')")
    Page<Member> search(@Param("kw") String keyword, Pageable pageable);
}
