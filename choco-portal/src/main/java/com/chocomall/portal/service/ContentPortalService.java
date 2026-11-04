package com.chocomall.portal.service;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.core.order.OrderLifecycleService;
import com.chocomall.model.entity.FoodPairing;
import com.chocomall.model.entity.HelpArticle;
import com.chocomall.model.entity.HelpCategory;
import com.chocomall.model.entity.OrderSetting;
import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.entity.ShopLocation;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.repository.FoodPairingRepository;
import com.chocomall.model.repository.HelpArticleRepository;
import com.chocomall.model.repository.HelpCategoryRepository;
import com.chocomall.model.repository.ServingFormatRepository;
import com.chocomall.model.repository.ShopLocationRepository;
import com.chocomall.portal.dto.PortalDtos.ShopInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public guidance content: the help centre, the food-pairing guide, ways to enjoy chocolate, and shop information.
 * Pairings and serving formats from the shop guide are only shown at the Core and Expand tiers; Test and
 * Specialist ideas stay internal until the shop offers them.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentPortalService {

    private static final List<Priority> PUBLIC_TIERS = List.of(Priority.CORE, Priority.EXPAND);

    private final HelpCategoryRepository helpCategories;
    private final HelpArticleRepository helpArticles;
    private final FoodPairingRepository pairings;
    private final ServingFormatRepository servings;
    private final ShopLocationRepository locations;
    private final OrderLifecycleService lifecycle;

    public List<Map<String, Object>> helpCentre() {
        return helpCategories.findByShowStatusTrueOrderBySortDesc().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("articles", helpArticles.findByCategoryIdAndShowStatusTrue(c.getId()).stream()
                    .map(a -> Map.of("id", a.getId(), "title", a.getTitle())).toList());
            return m;
        }).toList();
    }

    @Transactional
    public HelpArticle article(Long id) {
        HelpArticle a = Asserts.found(helpArticles.findById(id).filter(HelpArticle::isShowStatus), "Article");
        a.setReadCount(a.getReadCount() + 1);
        return a;
    }

    public List<String> pairingGroups() {
        return pairings.groups();
    }

    public CommonPage<FoodPairing> pairings(String group, String keyword, Integer pageNum, Integer pageSize) {
        return CommonPage.of(pairings.searchIn(blank(group), blank(keyword), PUBLIC_TIERS, PageParams.of(pageNum, pageSize, Sort.by("code"))));
    }

    public CommonPage<ServingFormat> servingFormats(String keyword, Integer pageNum, Integer pageSize) {
        return CommonPage.of(servings.searchIn(blank(keyword), PUBLIC_TIERS, PageParams.of(pageNum, pageSize, Sort.by("code"))));
    }

    public ShopInfo shopInfo() {
        OrderSetting s = lifecycle.setting();
        ShopLocation l = locations.findAll().stream().findFirst().orElse(null);
        String address = l == null ? null : java.util.stream.Stream.of(l.getDetailAddress(), l.getRegion(), l.getCity(), l.getProvince())
                .filter(x -> x != null && !x.isBlank()).collect(java.util.stream.Collectors.joining(", "));
        return new ShopInfo(l == null ? null : l.getName(), l == null ? null : l.getOpeningHours(), address, s.isDeliveryEnabled(),
                s.getDeliveryFee(), s.getFreeDeliveryThreshold(), s.getGiftPackagingFee(), s.getPickupLeadMinutes(),
                s.getNormalOrderTimeoutMinutes(), s.getPointsPerCurrencyUnit(), s.getMaxRedeemPercent(), s.getReturnWindowDays());
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
