package com.chocomall.model.repository;

import com.chocomall.model.entity.ProductSku;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    List<ProductSku> findByProductIdOrderBySortAscIdAsc(Long productId);

    List<ProductSku> findByProductIdInAndActiveTrue(Collection<Long> productIds);

    Optional<ProductSku> findBySkuCode(String skuCode);

    void deleteByProductId(Long productId);
}
