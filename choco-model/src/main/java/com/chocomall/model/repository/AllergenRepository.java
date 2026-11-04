package com.chocomall.model.repository;

import com.chocomall.model.entity.Allergen;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergenRepository extends JpaRepository<Allergen, Long> {

    Optional<Allergen> findByCode(String code);
}
