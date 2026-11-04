package com.chocomall.model.repository;

import com.chocomall.model.entity.OrderSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderSettingRepository extends JpaRepository<OrderSetting, Long> {
}
