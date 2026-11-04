package com.chocomall.model.repository;

import com.chocomall.model.entity.Brand;
import com.chocomall.model.enums.BrandStockingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    Optional<Brand> findByCode(String code);

    @Query("select b from Brand b where (:kw is null or b.name like concat('%', :kw, '%')) and (:region is null or b.region = :region) and (:status is null or b.stockingStatus = :status) and (:show is null or b.showStatus = :show)")
    Page<Brand> search(@Param("kw") String keyword, @Param("region") String region, @Param("status") BrandStockingStatus status, @Param("show") Boolean show, Pageable pageable);

    List<Brand> findByStockingStatusAndShowStatusTrue(BrandStockingStatus status);

    @Query("select distinct b.region from Brand b order by b.region")
    List<String> regions();
}
