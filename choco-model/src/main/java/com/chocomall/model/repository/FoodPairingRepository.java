package com.chocomall.model.repository;

import com.chocomall.model.entity.FoodPairing;
import com.chocomall.model.enums.Priority;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodPairingRepository extends JpaRepository<FoodPairing, Long> {

    @Query("select f from FoodPairing f where (:grp is null or f.foodGroup = :grp) and (:kw is null or f.food like concat('%', :kw, '%')) and (:priority is null or f.priority = :priority)")
    Page<FoodPairing> search(@Param("grp") String group, @Param("kw") String keyword, @Param("priority") Priority priority, Pageable pageable);

    @Query("select f from FoodPairing f where (:grp is null or f.foodGroup = :grp) and (:kw is null or f.food like concat('%', :kw, '%')) and f.priority in :priorities")
    Page<FoodPairing> searchIn(@Param("grp") String group, @Param("kw") String keyword, @Param("priorities") Collection<Priority> priorities, Pageable pageable);

    @Query("select distinct f.foodGroup from FoodPairing f order by f.foodGroup")
    List<String> groups();
}
