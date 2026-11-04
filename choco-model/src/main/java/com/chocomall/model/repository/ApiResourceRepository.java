package com.chocomall.model.repository;

import com.chocomall.model.entity.ApiResource;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiResourceRepository extends JpaRepository<ApiResource, Long> {

    @Query("select r from ApiResource r where (:cat is null or r.categoryId = :cat) and (:name is null or r.name like concat('%', :name, '%')) and (:url is null or r.url like concat('%', :url, '%'))")
    Page<ApiResource> search(@Param("cat") Long categoryId, @Param("name") String name, @Param("url") String url, Pageable pageable);

    List<ApiResource> findByCategoryId(Long categoryId);
}
