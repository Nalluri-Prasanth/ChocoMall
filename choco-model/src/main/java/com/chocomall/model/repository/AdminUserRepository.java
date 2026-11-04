package com.chocomall.model.repository;

import com.chocomall.model.entity.AdminUser;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("select a from AdminUser a where :kw is null or a.username like concat('%', :kw, '%') or a.nickName like concat('%', :kw, '%')")
    Page<AdminUser> search(@Param("kw") String keyword, Pageable pageable);

    @Query("select count(a) from AdminUser a join a.roleIds r where r = :roleId")
    long countByRole(@Param("roleId") Long roleId);
}
