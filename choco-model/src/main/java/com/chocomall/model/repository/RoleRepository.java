package com.chocomall.model.repository;

import com.chocomall.model.entity.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    @Query("select r from Role r where :kw is null or r.name like concat('%', :kw, '%')")
    Page<Role> search(@Param("kw") String keyword, Pageable pageable);

    List<Role> findByStatus(Integer status);
}
