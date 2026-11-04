package com.chocomall.model.repository;

import com.chocomall.model.entity.ResourceCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {

    List<ResourceCategory> findAllByOrderBySortDesc();
}
