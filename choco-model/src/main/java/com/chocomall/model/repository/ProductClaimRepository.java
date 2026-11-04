package com.chocomall.model.repository;

import com.chocomall.model.entity.ProductClaim;
import com.chocomall.model.enums.ClaimType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductClaimRepository extends JpaRepository<ProductClaim, Long> {

    List<ProductClaim> findByProductId(Long productId);

    List<ProductClaim> findByProductIdInAndVerifiedTrue(Collection<Long> productIds);

    Optional<ProductClaim> findByProductIdAndClaimType(Long productId, ClaimType claimType);
}
