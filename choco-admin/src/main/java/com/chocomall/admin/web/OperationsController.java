package com.chocomall.admin.web;

import com.chocomall.admin.dto.OpsDtos.BatchParam;
import com.chocomall.admin.dto.OpsDtos.BatchReleaseParam;
import com.chocomall.admin.dto.OpsDtos.BundleParam;
import com.chocomall.admin.dto.OpsDtos.GuideUpdateParam;
import com.chocomall.admin.dto.OpsDtos.RejectParam;
import com.chocomall.admin.dto.OpsDtos.SessionParam;
import com.chocomall.admin.dto.OpsDtos.StockAdjustParam;
import com.chocomall.admin.dto.OpsDtos.StockCountParam;
import com.chocomall.admin.dto.OpsDtos.StockRow;
import com.chocomall.admin.dto.OpsDtos.WindowParam;
import com.chocomall.admin.service.GuideService;
import com.chocomall.admin.service.PlanningService;
import com.chocomall.admin.service.ProductionService;
import com.chocomall.admin.service.StockAdminService;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.api.CommonResult;
import com.chocomall.model.entity.Bundle;
import com.chocomall.model.entity.ExperienceBooking;
import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.entity.GuideItem;
import com.chocomall.model.entity.GuideSource;
import com.chocomall.model.entity.PreorderWindow;
import com.chocomall.model.entity.ProductionBatch;
import com.chocomall.model.entity.StockMovement;
import com.chocomall.model.enums.BatchStatus;
import com.chocomall.model.enums.GuideStatus;
import com.chocomall.model.enums.GuideType;
import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.WindowStatus;
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

/** Stock, capacity planning, production and the launch playbook. */
@Tag(name = "Operations")
@RestController
@RequiredArgsConstructor
public class OperationsController {

    private final StockAdminService stock;
    private final PlanningService planning;
    private final ProductionService production;
    private final GuideService guides;

    // ---- stock
    @GetMapping("/stock/list")
    public CommonResult<CommonPage<StockRow>> inventory(@RequestParam(required = false) String keyword,
                                                        @RequestParam(defaultValue = "false") boolean lowOnly,
                                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                                        @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(stock.inventory(keyword, lowOnly, pageNum, pageSize));
    }

    @GetMapping("/stock/low")
    public CommonResult<List<StockRow>> lowStock() {
        return CommonResult.success(stock.lowStock());
    }

    @Operation(summary = "Add or remove units by hand (waste, production or correction)")
    @PostMapping("/stock/adjust")
    public CommonResult<Integer> adjust(@Valid @RequestBody StockAdjustParam p) {
        return CommonResult.success(stock.adjust(p));
    }

    @Operation(summary = "Stock-take: set the counted quantity")
    @PostMapping("/stock/count")
    public CommonResult<Integer> count(@Valid @RequestBody StockCountParam p) {
        return CommonResult.success(stock.count(p));
    }

    @GetMapping("/stock/ledger/{productId}")
    public CommonResult<CommonPage<StockMovement>> ledger(@PathVariable Long productId,
                                                          @RequestParam(defaultValue = "1") Integer pageNum,
                                                          @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(stock.ledger(productId, pageNum, pageSize));
    }

    // ---- preorder windows
    @GetMapping("/preorderWindow/list")
    public CommonResult<List<PreorderWindow>> windows() {
        return CommonResult.success(planning.windows());
    }

    @GetMapping("/preorderWindow/{id}")
    public CommonResult<PreorderWindow> window(@PathVariable Long id) {
        return CommonResult.success(planning.window(id));
    }

    @PostMapping("/preorderWindow/create")
    public CommonResult<PreorderWindow> createWindow(@Valid @RequestBody WindowParam p) {
        return CommonResult.success(planning.createWindow(p));
    }

    @PostMapping("/preorderWindow/update/{id}")
    public CommonResult<PreorderWindow> updateWindow(@PathVariable Long id, @Valid @RequestBody WindowParam p) {
        return CommonResult.success(planning.updateWindow(id, p));
    }

    @PostMapping("/preorderWindow/status/{id}")
    public CommonResult<PreorderWindow> windowStatus(@PathVariable Long id, @RequestParam WindowStatus status) {
        return CommonResult.success(planning.setWindowStatus(id, status));
    }

    @PostMapping("/preorderWindow/delete/{id}")
    public CommonResult<Void> deleteWindow(@PathVariable Long id) {
        planning.deleteWindow(id);
        return CommonResult.success();
    }

    // ---- experience sessions
    @GetMapping("/experience/session/list")
    public CommonResult<List<ExperienceSession>> sessions(@RequestParam(required = false) Long productId) {
        return CommonResult.success(planning.upcomingSessions(productId));
    }

    @GetMapping("/experience/session/{id}")
    public CommonResult<ExperienceSession> session(@PathVariable Long id) {
        return CommonResult.success(planning.session(id));
    }

    @PostMapping("/experience/session/create")
    public CommonResult<ExperienceSession> createSession(@Valid @RequestBody SessionParam p) {
        return CommonResult.success(planning.createSession(p));
    }

    @PostMapping("/experience/session/update/{id}")
    public CommonResult<ExperienceSession> updateSession(@PathVariable Long id, @Valid @RequestBody SessionParam p) {
        return CommonResult.success(planning.updateSession(id, p));
    }

    @Operation(summary = "Cancel a session; every booking is cancelled and refunded")
    @PostMapping("/experience/session/cancel/{id}")
    public CommonResult<Integer> cancelSession(@PathVariable Long id) {
        return CommonResult.success(planning.cancelSession(id));
    }

    @GetMapping("/experience/session/{id}/bookings")
    public CommonResult<CommonPage<ExperienceBooking>> bookings(@PathVariable Long id,
                                                                @RequestParam(defaultValue = "1") Integer pageNum,
                                                                @RequestParam(defaultValue = "20") Integer pageSize) {
        return CommonResult.success(planning.bookings(id, pageNum, pageSize));
    }

    // ---- bundles
    @GetMapping("/bundle/list")
    public CommonResult<List<Map<String, Object>>> bundles() {
        return CommonResult.success(planning.bundles());
    }

    @PostMapping("/bundle/create")
    public CommonResult<Bundle> createBundle(@Valid @RequestBody BundleParam p) {
        return CommonResult.success(planning.createBundle(p));
    }

    @PostMapping("/bundle/update/{id}")
    public CommonResult<Bundle> updateBundle(@PathVariable Long id, @Valid @RequestBody BundleParam p) {
        return CommonResult.success(planning.updateBundle(id, p));
    }

    @PostMapping("/bundle/delete/{id}")
    public CommonResult<Void> deleteBundle(@PathVariable Long id) {
        planning.deleteBundle(id);
        return CommonResult.success();
    }

    // ---- production
    @PostMapping("/production/create")
    public CommonResult<ProductionBatch> plan(@Valid @RequestBody BatchParam p) {
        return CommonResult.success(production.plan(p));
    }

    @GetMapping("/production/list")
    public CommonResult<CommonPage<ProductionBatch>> batches(@RequestParam(required = false) Long productId,
                                                             @RequestParam(required = false) BatchStatus status,
                                                             @RequestParam(defaultValue = "1") Integer pageNum,
                                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return CommonResult.success(production.list(productId, status, pageNum, pageSize));
    }

    @GetMapping("/production/{id}")
    public CommonResult<ProductionBatch> batch(@PathVariable Long id) {
        return CommonResult.success(production.get(id));
    }

    @PostMapping("/production/{id}/start")
    public CommonResult<ProductionBatch> start(@PathVariable Long id) {
        return CommonResult.success(production.start(id));
    }

    @Operation(summary = "Release a batch to stock after the allergen check, temper test and best-before date")
    @PostMapping("/production/{id}/release")
    public CommonResult<ProductionBatch> release(@PathVariable Long id, @Valid @RequestBody BatchReleaseParam p) {
        return CommonResult.success(production.release(id, p));
    }

    @PostMapping("/production/{id}/reject")
    public CommonResult<ProductionBatch> reject(@PathVariable Long id, @Valid @RequestBody RejectParam p) {
        return CommonResult.success(production.reject(id, p.reason()));
    }

    @Operation(summary = "Waste against released output, per product")
    @GetMapping("/production/report/waste")
    public CommonResult<List<Map<String, Object>>> waste() {
        return CommonResult.success(production.wasteReport());
    }

    // ---- launch playbook
    @Operation(summary = "Guide items of one type (PRODUCTION, PRINTING, SALES_LAUNCH, SHOP_FORMAT, ROADMAP, SHOP_CONCEPT)")
    @GetMapping("/guide/list")
    public CommonResult<List<GuideItem>> guideItems(@RequestParam GuideType type,
                                                    @RequestParam(required = false) GuideStatus status,
                                                    @RequestParam(required = false) Priority priority,
                                                    @RequestParam(required = false) String phase) {
        return CommonResult.success(guides.list(type, status, priority, phase));
    }

    @GetMapping("/guide/{id}")
    public CommonResult<GuideItem> guideItem(@PathVariable Long id) {
        return CommonResult.success(guides.get(id));
    }

    @PostMapping("/guide/update/{id}")
    public CommonResult<GuideItem> updateGuide(@PathVariable Long id, @Valid @RequestBody GuideUpdateParam p) {
        return CommonResult.success(guides.update(id, p));
    }

    @GetMapping("/guide/progress")
    public CommonResult<Map<String, Object>> progress() {
        return CommonResult.success(guides.progress());
    }

    @GetMapping("/guide/launch-checklist")
    public CommonResult<Map<String, List<GuideItem>>> checklist() {
        return CommonResult.success(guides.launchChecklist());
    }

    @GetMapping("/guide/sources")
    public CommonResult<List<GuideSource>> sources() {
        return CommonResult.success(guides.sources());
    }
}
