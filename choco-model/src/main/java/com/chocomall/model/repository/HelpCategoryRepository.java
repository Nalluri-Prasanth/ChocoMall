package com.chocomall.model.repository;

import com.chocomall.model.entity.HelpCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpCategoryRepository extends JpaRepository<HelpCategory, Long> {

    List<HelpCategory> findByShowStatusTrueOrderBySortDesc();
}
