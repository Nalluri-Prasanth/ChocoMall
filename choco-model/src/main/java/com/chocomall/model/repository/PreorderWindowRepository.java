package com.chocomall.model.repository;

import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.enums.WindowStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreorderWindowRepository extends JpaRepository<PreorderWindow, Long> {

    List<PreorderWindow> findByStatusAndOrderCutoffAfterOrderByOrderCutoff(WindowStatus status, LocalDateTime now);

    @Modifying
    @Query("update PreorderWindow w set w.reservedUnits = w.reservedUnits + :units where w.id = :id and w.status = com.chocomall.model.enums.WindowStatus.OPEN and w.orderCutoff > :now and w.reservedUnits + :units <= w.capacityUnits")
    int reserve(@Param("id") Long id, @Param("units") int units, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update PreorderWindow w set w.reservedUnits = w.reservedUnits - :units where w.id = :id and w.reservedUnits >= :units")
    int release(@Param("id") Long id, @Param("units") int units);
}
