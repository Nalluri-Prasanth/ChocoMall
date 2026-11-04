package com.chocomall.model.repository;

import com.chocomall.model.entity.GuideItem;
import com.chocomall.model.enums.GuideStatus;
import com.chocomall.model.enums.GuideType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuideItemRepository extends JpaRepository<GuideItem, Long> {

    List<GuideItem> findByGuideTypeOrderBySortAscCodeAsc(GuideType guideType);

    Optional<GuideItem> findByCode(String code);

    long countByGuideTypeAndStatus(GuideType guideType, GuideStatus status);

    long countByGuideType(GuideType guideType);
}
