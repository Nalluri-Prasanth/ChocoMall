package com.chocomall.portal.web;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.FoodPairing;
import com.chocomall.model.entity.HelpArticle;
import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.enums.ClaimType;
import com.chocomall.model.enums.ProductKind;
import com.chocomall.portal.dto.PortalDtos.BrandView;
import com.chocomall.portal.dto.PortalDtos.BundleView;
import com.chocomall.portal.dto.PortalDtos.CategoryNode;
import com.chocomall.portal.dto.PortalDtos.HomeContent;
import com.chocomall.portal.dto.PortalDtos.NamedItem;
import com.chocomall.portal.dto.PortalDtos.ProductCard;
import com.chocomall.portal.dto.PortalDtos.ProductDetailView;
import com.chocomall.portal.dto.PortalDtos.SessionView;
import com.chocomall.portal.dto.PortalDtos.ShopInfo;
import com.chocomall.portal.dto.PortalDtos.SubjectView;
import com.chocomall.portal.dto.PortalDtos.WindowView;
import com.chocomall.portal.security.CurrentMember;
import com.chocomall.portal.service.CatalogViewService;
import com.chocomall.portal.service.CatalogViewService.SearchQuery;
import com.chocomall.portal.service.ContentPortalService;
import com.chocomall.portal.service.HomeService;
import com.chocomall.portal.service.MemberCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only browsing: home page, search, product pages, brands, bundles, guides and help. */
@Tag(name = "Browse")
@SecurityRequirements
@RestController
@RequiredArgsConstructor
public class ShopController {

    private final HomeService home;
    private final CatalogViewService catalog;
    private final ContentPortalService content;
    private final MemberCenterService memberCenter;

    // ---- home
    @Operation(summary = "Everything the home page needs: banners, brands, new and recommended items, subjects, categories")
    @GetMapping("/home/content")
    public CommonResult<HomeContent> homeContent() {
        return CommonResult.success(home.content());
    }

    @GetMapping("/home/recommendProductList")
    public CommonResult<List<ProductCard>> recommended(@RequestParam(defaultValue = "8") int limit) {
        return CommonResult.success(home.recommended(Math.min(limit, 50)));
    }

    @GetMapping("/home/newProductList")
    public CommonResult<List<ProductCard>> newProducts(@RequestParam(defaultValue = "8") int limit) {
        return CommonResult.success(home.newProducts(Math.min(limit, 50)));
    }

    @GetMapping("/home/hotProductList")
    public CommonResult<List<ProductCard>> hot(@RequestParam(defaultValue = "8") int limit) {
        return CommonResult.success(home.bestSellers(Math.min(limit, 50)));
    }

    @GetMapping("/home/subjectList")
    public CommonResult<CommonPage<SubjectView>> subjects(@RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "6") Integer pageSize) {
        return CommonResult.success(home.subjects(pageNum, pageSize));
    }

    @GetMapping("/home/subject/{id}")
    public CommonResult<Map<String, Object>> subject(@PathVariable Long id) {
        return CommonResult.success(home.subject(id));
    }

    // ---- products
    @Operation(summary = "Search published products. Filters: keyword, category, brand, kind, flavor, chocolate type, allergens to avoid, verified claim, price range")
    @GetMapping("/product/search")
    public CommonResult<CommonPage<ProductCard>> search(@RequestParam(required = false) String keyword,
                                                        @RequestParam(required = false) Long categoryId,
                                                        @RequestParam(required = false) Long brandId,
                                                        @RequestParam(required = false) ProductKind kind,
                                                        @RequestParam(required = false) Long flavorId,
                                                        @RequestParam(required = false) Long chocolateTypeId,
                                                        @RequestParam(required = false) Set<Long> excludeAllergens,
                                                        @RequestParam(required = false) ClaimType claim,
                                                        @RequestParam(required = false) BigDecimal minPrice,
                                                        @RequestParam(required = false) BigDecimal maxPrice,
                                                        @RequestParam(defaultValue = "0") Integer sort,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "12") Integer pageSize) {
        return CommonResult.success(catalog.search(new SearchQuery(keyword, categoryId, brandId, kind, flavorId, chocolateTypeId,
                excludeAllergens, claim, minPrice, maxPrice, sort, pageNum, pageSize)));
    }

    @Operation(summary = "Product page: SKUs, flavors, allergens, verified claims, bundles, preorder windows or sessions")
    @GetMapping("/product/detail/{id}")
    public CommonResult<ProductDetailView> detail(@PathVariable Long id) {
        Long memberId = CurrentMember.idOrNull();
        ProductDetailView view = catalog.detail(id, memberId);
        if (memberId != null) {
            memberCenter.recordView(memberId, id);
        }
        return CommonResult.success(view);
    }

    @GetMapping("/product/related/{id}")
    public CommonResult<List<ProductCard>> related(@PathVariable Long id, @RequestParam(defaultValue = "4") int limit) {
        return CommonResult.success(catalog.related(id, Math.min(limit, 12)));
    }

    @Operation(summary = "Allergens shoppers can filter on")
    @GetMapping("/product/allergens")
    public CommonResult<List<NamedItem>> allergens() {
        return CommonResult.success(catalog.allergenList());
    }

    @GetMapping("/product/flavors")
    public CommonResult<List<NamedItem>> flavors() {
        return CommonResult.success(catalog.flavorList());
    }

    @GetMapping("/product/chocolateTypes")
    public CommonResult<List<NamedItem>> chocolateTypes() {
        return CommonResult.success(catalog.chocolateTypeList());
    }

    @Operation(summary = "Bonbons that can go into a build-a-box right now")
    @GetMapping("/product/boxCandidates")
    public CommonResult<List<ProductCard>> boxCandidates() {
        return CommonResult.success(catalog.cards(catalog.boxCandidates()));
    }

    @GetMapping("/product/categoryTreeList")
    public CommonResult<List<CategoryNode>> categories() {
        return CommonResult.success(catalog.categoryTree());
    }

    @GetMapping("/product/{id}/reviews")
    public CommonResult<CommonPage<com.chocomall.model.entity.ProductComment>> reviews(@PathVariable Long id,
                                                                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(memberCenter.reviews(id, pageNum, pageSize));
    }

    // ---- brands, bundles, windows, sessions
    @GetMapping("/brand/list")
    public CommonResult<CommonPage<BrandView>> brands(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String region,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "12") Integer pageSize) {
        return CommonResult.success(catalog.brands(keyword, region, pageNum, pageSize, CurrentMember.idOrNull()));
    }

    @GetMapping("/brand/detail/{id}")
    public CommonResult<Map<String, Object>> brand(@PathVariable Long id) {
        return CommonResult.success(Map.of("brand", catalog.brand(id, CurrentMember.idOrNull()), "products", catalog.brandProducts(id)));
    }

    @GetMapping("/bundle/list")
    public CommonResult<List<BundleView>> bundles() {
        return CommonResult.success(catalog.activeBundles());
    }

    @Operation(summary = "Open preorder windows with the capacity left")
    @GetMapping("/preorder/windows")
    public CommonResult<List<WindowView>> windows() {
        return CommonResult.success(catalog.openWindows());
    }

    @Operation(summary = "Upcoming tastings and workshops with the seats left")
    @GetMapping("/experience/list")
    public CommonResult<List<SessionView>> sessions() {
        return CommonResult.success(catalog.allUpcomingSessions());
    }

    // ---- guides & help
    @GetMapping("/guide/pairings")
    public CommonResult<CommonPage<FoodPairing>> pairings(@RequestParam(required = false) String group,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(content.pairings(group, keyword, pageNum, pageSize));
    }

    @GetMapping("/guide/pairingGroups")
    public CommonResult<List<String>> pairingGroups() {
        return CommonResult.success(content.pairingGroups());
    }

    @GetMapping("/guide/serving")
    public CommonResult<CommonPage<ServingFormat>> serving(@RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(content.servingFormats(keyword, pageNum, pageSize));
    }

    @GetMapping("/help/centre")
    public CommonResult<List<Map<String, Object>>> help() {
        return CommonResult.success(content.helpCentre());
    }

    @GetMapping("/help/article/{id}")
    public CommonResult<HelpArticle> article(@PathVariable Long id) {
        return CommonResult.success(content.article(id));
    }

    @Operation(summary = "Opening hours, delivery fees, pickup lead time, loyalty and return rules")
    @GetMapping("/shop/info")
    public CommonResult<ShopInfo> info() {
        return CommonResult.success(content.shopInfo());
    }
}
