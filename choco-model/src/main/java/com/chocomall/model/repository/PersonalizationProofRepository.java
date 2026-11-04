package com.chocomall.model.repository;

import com.chocomall.model.entity.PersonalizationProof;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalizationProofRepository extends JpaRepository<PersonalizationProof, Long> {

    List<PersonalizationProof> findByRequestIdOrderByVersionAsc(Long requestId);

    Optional<PersonalizationProof> findByRequestIdAndVersion(Long requestId, Integer version);
}
