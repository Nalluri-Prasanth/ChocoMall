package com.chocomall.model.repository;

import com.chocomall.model.entity.Subject;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    @Query("select s from Subject s where (:kw is null or s.title like concat('%', :kw, '%')) and (:cat is null or s.category = :cat)")
    Page<Subject> search(@Param("kw") String keyword, @Param("cat") String category, Pageable pageable);

    Page<Subject> findByShowStatusTrue(Pageable pageable);

    List<Subject> findByShowStatusTrueAndRecommendStatusTrueOrderBySortDesc();
}
