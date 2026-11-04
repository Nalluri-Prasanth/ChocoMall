package com.chocomall.model.repository;

import com.chocomall.model.entity.ChocolateType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChocolateTypeRepository extends JpaRepository<ChocolateType, Long> {

    Optional<ChocolateType> findByCode(String code);

    List<ChocolateType> findByClassification(String classification);
}
