package com.chocomall.model.repository;

import com.chocomall.model.entity.ReturnReason;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnReasonRepository extends JpaRepository<ReturnReason, Long> {

    List<ReturnReason> findByStatusTrueOrderBySortDesc();
}
