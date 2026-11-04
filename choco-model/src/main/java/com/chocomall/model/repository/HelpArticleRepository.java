package com.chocomall.model.repository;

import com.chocomall.model.entity.HelpArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpArticleRepository extends JpaRepository<HelpArticle, Long> {

    List<HelpArticle> findByCategoryIdAndShowStatusTrue(Long categoryId);
}
