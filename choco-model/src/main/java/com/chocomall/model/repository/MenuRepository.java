package com.chocomall.model.repository;

import com.chocomall.model.entity.Menu;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findByParentIdOrderBySortDesc(Long parentId);

    Page<Menu> findByParentId(Long parentId, Pageable pageable);

    List<Menu> findAllByOrderBySortDesc();
}
