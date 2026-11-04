package com.chocomall.model.repository;

import com.chocomall.model.entity.ProductCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByParentIdOrderBySortDesc(Long parentId);

    Page<ProductCategory> findByParentId(Long parentId, Pageable pageable);

    List<ProductCategory> findByShowStatusTrueOrderBySortDesc();

    Optional<ProductCategory> findByNameAndParentId(String name, Long parentId);
}
