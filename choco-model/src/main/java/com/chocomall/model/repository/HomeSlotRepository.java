package com.chocomall.model.repository;

import com.chocomall.model.entity.HomeSlot;
import com.chocomall.model.enums.HomeSlotType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeSlotRepository extends JpaRepository<HomeSlot, Long> {

    List<HomeSlot> findBySlotTypeOrderBySortDesc(HomeSlotType slotType);

    List<HomeSlot> findBySlotTypeAndStatusTrueOrderBySortDesc(HomeSlotType slotType);

    Page<HomeSlot> findBySlotType(HomeSlotType slotType, Pageable pageable);
}
