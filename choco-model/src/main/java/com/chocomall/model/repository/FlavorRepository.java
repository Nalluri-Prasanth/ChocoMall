package com.chocomall.model.repository;

import com.chocomall.model.entity.Flavor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlavorRepository extends JpaRepository<Flavor, Long> {

    Optional<Flavor> findByCode(String code);

    List<Flavor> findByFlavorGroup(String flavorGroup);
}
