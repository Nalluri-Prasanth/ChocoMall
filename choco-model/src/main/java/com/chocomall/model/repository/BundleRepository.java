package com.chocomall.model.repository;

import com.chocomall.model.entity.Bundle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BundleRepository extends JpaRepository<Bundle, Long> {

    List<Bundle> findByStatusTrueOrderBySortDesc();
}
