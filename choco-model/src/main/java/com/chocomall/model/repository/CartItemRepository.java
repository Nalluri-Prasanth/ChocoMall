package com.chocomall.model.repository;

import com.chocomall.model.entity.CartItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByMemberIdOrderByIdDesc(Long memberId);

    Optional<CartItem> findByIdAndMemberId(Long id, Long memberId);

    List<CartItem> findByMemberIdAndIdIn(Long memberId, Collection<Long> ids);

    List<CartItem> findByMemberIdAndProductIdAndProductSkuId(Long memberId, Long productId, Long productSkuId);

    void deleteByMemberId(Long memberId);

    void deleteByMemberIdAndIdIn(Long memberId, Collection<Long> ids);
}
