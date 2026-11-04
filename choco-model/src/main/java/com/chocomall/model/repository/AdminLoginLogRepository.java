package com.chocomall.model.repository;

import com.chocomall.model.entity.AdminLoginLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLoginLogRepository extends JpaRepository<AdminLoginLog, Long> {

    List<AdminLoginLog> findTop20ByAdminIdOrderByCreateTimeDesc(Long adminId);
}
