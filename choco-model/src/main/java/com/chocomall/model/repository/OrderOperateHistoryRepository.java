package com.chocomall.model.repository;

import com.chocomall.model.entity.OrderOperateHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderOperateHistoryRepository extends JpaRepository<OrderOperateHistory, Long> {

    List<OrderOperateHistory> findByOrderIdOrderByIdAsc(Long orderId);
}
