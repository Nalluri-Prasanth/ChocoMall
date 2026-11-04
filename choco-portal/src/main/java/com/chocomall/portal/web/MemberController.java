package com.chocomall.portal.web;

import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.IntegrationHistory;
import com.chocomall.model.entity.MemberAddress;
import com.chocomall.model.entity.ProductComment;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.portal.dto.PortalDtos.AddressParam;
import com.chocomall.portal.dto.PortalDtos.CollectionView;
import com.chocomall.portal.dto.PortalDtos.CommentParam;
import com.chocomall.portal.dto.PortalDtos.CouponClaimResult;
import com.chocomall.portal.dto.PortalDtos.CouponView;
import com.chocomall.portal.dto.PortalDtos.PreferenceParam;
import com.chocomall.portal.dto.PortalDtos.ProductCard;
import com.chocomall.portal.dto.PortalDtos.ReadHistoryView;
import com.chocomall.portal.security.CurrentMember;
import com.chocomall.portal.service.MemberCenterService;
import com.chocomall.portal.service.SsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in customer's account area. */
@Tag(name = "Member centre")
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberCenterService center;
    private final SsoService sso;

    // ---- addresses
    @GetMapping("/member/address/list")
    public CommonResult<List<MemberAddress>> addresses() {
        return CommonResult.success(center.addresses(CurrentMember.id()));
    }

    @PostMapping("/member/address/add")
    public CommonResult<MemberAddress> addAddress(@Valid @RequestBody AddressParam p) {
        return CommonResult.success(center.addAddress(CurrentMember.id(), p));
    }

    @PostMapping("/member/address/update/{id}")
    public CommonResult<MemberAddress> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressParam p) {
        return CommonResult.success(center.updateAddress(CurrentMember.id(), id, p));
    }

    @PostMapping("/member/address/delete/{id}")
    public CommonResult<Void> deleteAddress(@PathVariable Long id) {
        center.deleteAddress(CurrentMember.id(), id);
        return CommonResult.success();
    }

    // ---- coupons
    @GetMapping("/member/coupon/list")
    public CommonResult<List<CouponView>> coupons(@RequestParam(required = false) CouponUseStatus useStatus) {
        return CommonResult.success(center.myCoupons(CurrentMember.id(), useStatus));
    }

    @Operation(summary = "Coupons this member can still claim")
    @GetMapping("/member/coupon/claimable")
    public CommonResult<List<CouponView>> claimable() {
        return CommonResult.success(center.claimable(CurrentMember.id()));
    }

    @PostMapping("/member/coupon/claim/{couponId}")
    public CommonResult<CouponClaimResult> claim(@PathVariable Long couponId) {
        CouponHistory h = center.claim(CurrentMember.id(), couponId);
        return CommonResult.success(new CouponClaimResult(h.getId(), "Coupon added to your wallet"));
    }

    // ---- history, favourites, brands
    @GetMapping("/member/readHistory/list")
    public CommonResult<CommonPage<ReadHistoryView>> history(@RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(center.history(CurrentMember.id(), pageNum, pageSize));
    }

    @PostMapping("/member/readHistory/clear")
    public CommonResult<Void> clearHistory() {
        center.clearHistory(CurrentMember.id());
        return CommonResult.success();
    }

    @GetMapping("/member/productCollection/list")
    public CommonResult<CommonPage<CollectionView>> favourites(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(center.favourites(CurrentMember.id(), pageNum, pageSize));
    }

    @PostMapping("/member/productCollection/add/{productId}")
    public CommonResult<Void> addFavourite(@PathVariable Long productId) {
        center.addFavourite(CurrentMember.id(), productId);
        return CommonResult.success();
    }

    @PostMapping("/member/productCollection/delete/{productId}")
    public CommonResult<Void> removeFavourite(@PathVariable Long productId) {
        center.removeFavourite(CurrentMember.id(), productId);
        return CommonResult.success();
    }

    @PostMapping("/member/productCollection/clear")
    public CommonResult<Void> clearFavourites() {
        center.clearFavourites(CurrentMember.id());
        return CommonResult.success();
    }

    @GetMapping("/member/attention/list")
    public CommonResult<CommonPage<Map<String, Object>>> followed(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(center.followedBrands(CurrentMember.id(), pageNum, pageSize));
    }

    @PostMapping("/member/attention/add/{brandId}")
    public CommonResult<Void> follow(@PathVariable Long brandId) {
        center.followBrand(CurrentMember.id(), brandId);
        return CommonResult.success();
    }

    @PostMapping("/member/attention/delete/{brandId}")
    public CommonResult<Void> unfollow(@PathVariable Long brandId) {
        center.unfollowBrand(CurrentMember.id(), brandId);
        return CommonResult.success();
    }

    // ---- preferences (consent based)
    @GetMapping("/member/preference")
    public CommonResult<Map<String, Object>> preferences() {
        return CommonResult.success(center.preferences(CurrentMember.id()));
    }

    @Operation(summary = "Save favourite flavors for recommendations (requires consent: true)")
    @PostMapping("/member/preference")
    public CommonResult<Void> savePreferences(@Valid @RequestBody PreferenceParam p) {
        center.savePreferences(CurrentMember.id(), p.flavorIds(), p.consent());
        return CommonResult.success();
    }

    @Operation(summary = "Delete saved flavor preferences")
    @PostMapping("/member/preference/delete")
    public CommonResult<Void> deletePreferences() {
        center.deletePreferences(CurrentMember.id());
        return CommonResult.success();
    }

    @GetMapping("/member/forYou")
    public CommonResult<List<ProductCard>> forYou(@RequestParam(defaultValue = "8") int limit) {
        return CommonResult.success(center.forYou(CurrentMember.id(), Math.min(limit, 24)));
    }

    // ---- points & reviews
    @GetMapping("/member/points")
    public CommonResult<CommonPage<IntegrationHistory>> points(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(center.points(CurrentMember.id(), pageNum, pageSize));
    }

    @PostMapping("/member/review/create")
    public CommonResult<ProductComment> review(@Valid @RequestBody CommentParam p) {
        String nickname = sso.profile(CurrentMember.id()).nickname();
        return CommonResult.success(center.review(CurrentMember.id(), nickname, p));
    }
}
