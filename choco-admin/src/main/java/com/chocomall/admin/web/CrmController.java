package com.chocomall.admin.web;

import com.chocomall.admin.dto.SalesDtos.CouponParam;
import com.chocomall.admin.dto.SalesDtos.GrantParam;
import com.chocomall.admin.dto.SalesDtos.HelpArticleParam;
import com.chocomall.admin.dto.SalesDtos.HelpCategoryParam;
import com.chocomall.admin.dto.SalesDtos.LevelParam;
import com.chocomall.admin.dto.SalesDtos.PairingParam;
import com.chocomall.admin.dto.SalesDtos.PointsAdjustParam;
import com.chocomall.admin.dto.SalesDtos.ServingParam;
import com.chocomall.admin.dto.SalesDtos.SlotParam;
import com.chocomall.admin.dto.SalesDtos.SubjectParam;
import com.chocomall.admin.service.ContentAdminService;
import com.chocomall.admin.service.MarketingAdminService;
import com.chocomall.admin.service.MemberAdminService;
import com.chocomall.admin.service.MemberAdminService.MemberView;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.Coupon;
import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.FoodPairing;
import com.chocomall.model.entity.HelpArticle;
import com.chocomall.model.entity.HelpCategory;
import com.chocomall.model.entity.HomeSlot;
import com.chocomall.model.entity.IntegrationHistory;
import com.chocomall.model.entity.MemberLevel;
import com.chocomall.model.entity.ServingFormat;
import com.chocomall.model.entity.Subject;
import com.chocomall.model.enums.CouponType;
import com.chocomall.model.enums.CouponUseStatus;
import com.chocomall.model.enums.HomeSlotType;
import com.chocomall.model.enums.Priority;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Members, marketing (coupons, home page) and editorial content. */
@Tag(name = "Members, marketing and content")
@RestController
@RequiredArgsConstructor
public class CrmController {

    private final MemberAdminService members;
    private final MarketingAdminService marketing;
    private final ContentAdminService content;

    // ---- members
    @GetMapping("/member/list")
    public CommonResult<CommonPage<MemberView>> members(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(members.list(keyword, pageNum, pageSize));
    }

    @GetMapping("/member/{id}")
    public CommonResult<MemberView> member(@PathVariable Long id) {
        return CommonResult.success(members.get(id));
    }

    @PostMapping("/member/updateStatus/{id}")
    public CommonResult<Void> memberStatus(@PathVariable Long id, @RequestParam Integer status) {
        members.setStatus(id, status);
        return CommonResult.success();
    }

    @PostMapping("/member/{id}/points")
    public CommonResult<Void> adjustPoints(@PathVariable Long id, @Valid @RequestBody PointsAdjustParam p) {
        members.adjustPoints(id, p.delta(), p.note());
        return CommonResult.success();
    }

    @GetMapping("/member/{id}/points/history")
    public CommonResult<CommonPage<IntegrationHistory>> pointsHistory(@PathVariable Long id,
                                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                                      @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(members.pointsHistory(id, pageNum, pageSize));
    }

    @GetMapping("/memberLevel/list")
    public CommonResult<List<MemberLevel>> levels() {
        return CommonResult.success(members.levels());
    }

    @PostMapping("/memberLevel/create")
    public CommonResult<MemberLevel> createLevel(@Valid @RequestBody LevelParam p) {
        return CommonResult.success(members.createLevel(p));
    }

    @PostMapping("/memberLevel/update/{id}")
    public CommonResult<MemberLevel> updateLevel(@PathVariable Long id, @Valid @RequestBody LevelParam p) {
        return CommonResult.success(members.updateLevel(id, p));
    }

    @PostMapping("/memberLevel/delete/{id}")
    public CommonResult<Void> deleteLevel(@PathVariable Long id) {
        members.deleteLevel(id);
        return CommonResult.success();
    }

    // ---- coupons
    @GetMapping("/coupon/list")
    public CommonResult<CommonPage<Coupon>> coupons(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) CouponType type,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(marketing.list(keyword, type, pageNum, pageSize));
    }

    @GetMapping("/coupon/{id}")
    public CommonResult<Coupon> coupon(@PathVariable Long id) {
        return CommonResult.success(marketing.get(id));
    }

    @PostMapping("/coupon/create")
    public CommonResult<Coupon> createCoupon(@Valid @RequestBody CouponParam p) {
        return CommonResult.success(marketing.create(p));
    }

    @PostMapping("/coupon/update/{id}")
    public CommonResult<Coupon> updateCoupon(@PathVariable Long id, @Valid @RequestBody CouponParam p) {
        return CommonResult.success(marketing.update(id, p));
    }

    @PostMapping("/coupon/delete/{id}")
    public CommonResult<Void> deleteCoupon(@PathVariable Long id) {
        marketing.delete(id);
        return CommonResult.success();
    }

    @PostMapping("/coupon/grant")
    public CommonResult<CouponHistory> grant(@Valid @RequestBody GrantParam p) {
        return CommonResult.success(marketing.grant(p.couponId(), p.memberId()));
    }

    @GetMapping("/couponHistory/list")
    public CommonResult<CommonPage<CouponHistory>> couponHistory(@RequestParam(required = false) Long couponId,
                                                                 @RequestParam(required = false) CouponUseStatus useStatus,
                                                                 @RequestParam(required = false) String orderSn,
                                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(marketing.history(couponId, useStatus, orderSn, pageNum, pageSize));
    }

    // ---- home page slots
    @GetMapping("/home/slot/list")
    public CommonResult<CommonPage<HomeSlot>> slots(@RequestParam HomeSlotType type,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(marketing.slots(type, pageNum, pageSize));
    }

    @PostMapping("/home/slot/create")
    public CommonResult<HomeSlot> createSlot(@Valid @RequestBody SlotParam p) {
        return CommonResult.success(marketing.createSlot(p));
    }

    @PostMapping("/home/slot/update/{id}")
    public CommonResult<HomeSlot> updateSlot(@PathVariable Long id, @Valid @RequestBody SlotParam p) {
        return CommonResult.success(marketing.updateSlot(id, p));
    }

    @PostMapping("/home/slot/delete")
    public CommonResult<Void> deleteSlots(@RequestParam Set<Long> ids) {
        marketing.deleteSlots(ids);
        return CommonResult.success();
    }

    @PostMapping("/home/slot/status/{id}")
    public CommonResult<Void> slotStatus(@PathVariable Long id, @RequestParam boolean status) {
        marketing.setSlotStatus(id, status);
        return CommonResult.success();
    }

    // ---- subjects
    @GetMapping("/subject/list")
    public CommonResult<CommonPage<Subject>> subjects(@RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String category,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(content.subjects(keyword, category, pageNum, pageSize));
    }

    @GetMapping("/subject/{id}")
    public CommonResult<Subject> subject(@PathVariable Long id) {
        return CommonResult.success(content.subject(id));
    }

    @PostMapping("/subject/create")
    public CommonResult<Subject> createSubject(@Valid @RequestBody SubjectParam p) {
        return CommonResult.success(content.createSubject(p));
    }

    @PostMapping("/subject/update/{id}")
    public CommonResult<Subject> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectParam p) {
        return CommonResult.success(content.updateSubject(id, p));
    }

    @PostMapping("/subject/delete")
    public CommonResult<Void> deleteSubjects(@RequestParam Set<Long> ids) {
        content.deleteSubjects(ids);
        return CommonResult.success();
    }

    // ---- help centre
    @GetMapping("/help/category/list")
    public CommonResult<List<HelpCategory>> helpCategories() {
        return CommonResult.success(content.helpCategories());
    }

    @PostMapping("/help/category/create")
    public CommonResult<HelpCategory> createHelpCategory(@Valid @RequestBody HelpCategoryParam p) {
        return CommonResult.success(content.createHelpCategory(p));
    }

    @PostMapping("/help/category/update/{id}")
    public CommonResult<HelpCategory> updateHelpCategory(@PathVariable Long id, @Valid @RequestBody HelpCategoryParam p) {
        return CommonResult.success(content.updateHelpCategory(id, p));
    }

    @PostMapping("/help/category/delete/{id}")
    public CommonResult<Void> deleteHelpCategory(@PathVariable Long id) {
        content.deleteHelpCategory(id);
        return CommonResult.success();
    }

    @GetMapping("/help/article/list")
    public CommonResult<List<HelpArticle>> articles(@RequestParam(required = false) Long categoryId) {
        return CommonResult.success(content.articles(categoryId));
    }

    @PostMapping("/help/article/create")
    public CommonResult<HelpArticle> createArticle(@Valid @RequestBody HelpArticleParam p) {
        return CommonResult.success(content.createArticle(p));
    }

    @PostMapping("/help/article/update/{id}")
    public CommonResult<HelpArticle> updateArticle(@PathVariable Long id, @Valid @RequestBody HelpArticleParam p) {
        return CommonResult.success(content.updateArticle(id, p));
    }

    @PostMapping("/help/article/delete/{id}")
    public CommonResult<Void> deleteArticle(@PathVariable Long id) {
        content.deleteArticle(id);
        return CommonResult.success();
    }

    // ---- pairings & serving formats
    @GetMapping("/pairing/list")
    public CommonResult<CommonPage<FoodPairing>> pairings(@RequestParam(required = false) String group,
                                                          @RequestParam(required = false) String keyword,
                                                          @RequestParam(required = false) Priority priority,
                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(content.pairings(group, keyword, priority, pageNum, pageSize));
    }

    @GetMapping("/pairing/groups")
    public CommonResult<List<String>> pairingGroups() {
        return CommonResult.success(content.pairingGroups());
    }

    @PostMapping("/pairing/create")
    public CommonResult<FoodPairing> createPairing(@Valid @RequestBody PairingParam p) {
        return CommonResult.success(content.createPairing(p));
    }

    @PostMapping("/pairing/update/{id}")
    public CommonResult<FoodPairing> updatePairing(@PathVariable Long id, @Valid @RequestBody PairingParam p) {
        return CommonResult.success(content.updatePairing(id, p));
    }

    @PostMapping("/pairing/delete/{id}")
    public CommonResult<Void> deletePairing(@PathVariable Long id) {
        content.deletePairing(id);
        return CommonResult.success();
    }

    @GetMapping("/serving/list")
    public CommonResult<CommonPage<ServingFormat>> servings(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Priority priority,
                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(content.servings(keyword, priority, pageNum, pageSize));
    }

    @PostMapping("/serving/create")
    public CommonResult<ServingFormat> createServing(@Valid @RequestBody ServingParam p) {
        return CommonResult.success(content.createServing(p));
    }

    @PostMapping("/serving/update/{id}")
    public CommonResult<ServingFormat> updateServing(@PathVariable Long id, @Valid @RequestBody ServingParam p) {
        return CommonResult.success(content.updateServing(id, p));
    }

    @PostMapping("/serving/delete/{id}")
    public CommonResult<Void> deleteServing(@PathVariable Long id) {
        content.deleteServing(id);
        return CommonResult.success();
    }
}
