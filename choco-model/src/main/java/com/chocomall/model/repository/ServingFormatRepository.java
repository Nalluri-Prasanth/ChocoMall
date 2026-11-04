package com.chocomall.model.repository;

import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.enums.Priority;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServingFormatRepository extends JpaRepository<ServingFormat, Long> {

    @Query("select s from ServingFormat s where (:kw is null or s.eatingFormat like concat('%', :kw, '%')) and (:priority is null or s.priority = :priority)")
    Page<ServingFormat> search(@Param("kw") String keyword, @Param("priority") Priority priority, Pageable pageable);

    @Query("select s from ServingFormat s where (:kw is null or s.eatingFormat like concat('%', :kw, '%')) and s.priority in :priorities")
    Page<ServingFormat> searchIn(@Param("kw") String keyword, @Param("priorities") Collection<Priority> priorities, Pageable pageable);
}
