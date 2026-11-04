package com.chocomall.admin.service;

import com.chocomall.admin.dto.OpsDtos.BatchParam;
import com.chocomall.admin.dto.OpsDtos.BatchReleaseParam;
import com.chocomall.admin.security.CurrentAdmin;
import com.chocomall.common.api.CommonPage;
import com.chocomall.common.exception.Asserts;
import com.chocomall.common.util.PageParams;
import com.chocomall.common.util.SnGenerator;
import com.chocomall.core.stock.StockService;
import com.chocomall.model.entity.GuideItem;
import com.chocomall.model.entity.Product;
import com.chocomall.model.entity.ProductionBatch;
import com.chocomall.model.enums.BatchStatus;
import com.chocomall.model.enums.GuideType;
import com.chocomall.model.enums.StockReason;
import com.chocomall.model.repository.GuideItemRepository;
import com.chocomall.model.repository.ProductRepository;
import com.chocomall.model.repository.ProductionBatchRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production batches with the controls from the guide's process map: allergen check before release (M32),
 * tempering test for molded and coated work (M09), a best-before date for anything made in the shop (M31, M34),
 * and waste recorded against the recipe (M34). Releasing a batch is what adds finished goods to stock.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductionService {

    /** Process codes whose output depends on correctly tempered chocolate (M04-M12 in the guide). */
    private static final List<String> TEMPERED = List.of("M04", "M05", "M06", "M07", "M08", "M09", "M10", "M11", "M12");

    private final ProductionBatchRepository batches;
    private final ProductRepository products;
    private final GuideItemRepository guides;
    private final StockService stock;

    public ProductionBatch plan(BatchParam p) {
        Product product = Asserts.found(products.findById(p.productId()), "Product");
        Asserts.state(StockService.tracksStock(product), product.getName() + " is not a stocked item, so it has no production batches");
        String process = blank(p.processCode());
        if (process != null) {
            GuideItem g = Asserts.found(guides.findByCode(process), "Process " + process);
            Asserts.isTrue(g.getGuideType() == GuideType.PRODUCTION, process + " is not a production process");
        }
        ProductionBatch b = new ProductionBatch();
        b.setBatchCode(SnGenerator.next("BATCH"));
        b.setProductId(p.productId());
        b.setProcessCode(process);
        b.setQuantityUnits(p.quantityUnits());
        b.setProducedAt(p.producedAt() == null ? LocalDateTime.now() : p.producedAt());
        b.setNote(p.note());
        b.setCreatedBy(CurrentAdmin.username());
        return batches.save(b);
    }

    public ProductionBatch start(Long id) {
        ProductionBatch b = load(id);
        Asserts.state(b.getStatus() == BatchStatus.PLANNED, "Only a planned batch can be started");
        b.setStatus(BatchStatus.IN_PROGRESS);
        return b;
    }

    /** Releases finished goods: records the checks, adds good units to stock and keeps the waste for reporting. */
    public ProductionBatch release(Long id, BatchReleaseParam p) {
        ProductionBatch b = load(id);
        Asserts.state(b.getStatus() == BatchStatus.IN_PROGRESS || b.getStatus() == BatchStatus.PLANNED,
                "A batch that is " + b.getStatus() + " cannot be released");
        Asserts.isTrue(p.allergenChecked(), "Confirm the allergen check before releasing a batch (labels and staff information must match the recipe)");
        if (b.getProcessCode() != null && TEMPERED.contains(b.getProcessCode())) {
            Asserts.isTrue(p.temperTestPassed() != null, "Record the temper test result for " + b.getProcessCode());
            Asserts.state(p.temperTestPassed(), "Reject a streaked or soft batch and correct the process (temper test failed)");
        }
        Product product = Asserts.found(products.findById(b.getProductId()), "Product");
        Asserts.isTrue(p.bestBefore() != null, "Enter the best-before date");
        LocalDate produced = (b.getProducedAt() == null ? LocalDateTime.now() : b.getProducedAt()).toLocalDate();
        Asserts.isTrue(p.bestBefore().isAfter(produced), "The best-before date must be after the production date");
        if (product.getShelfLifeDays() != null) {
            Asserts.isTrue(!p.bestBefore().isAfter(produced.plusDays(product.getShelfLifeDays())),
                    "The best-before date exceeds the validated shelf life of " + product.getShelfLifeDays() + " days");
        }
        int waste = p.wasteUnits() == null ? 0 : p.wasteUnits();
        Asserts.isTrue(waste <= b.getQuantityUnits(), "Waste cannot exceed the batch quantity");

        b.setAllergenChecked(true);
        b.setTemperTestPassed(p.temperTestPassed());
        b.setBestBefore(p.bestBefore());
        b.setWasteUnits(waste);
        b.setStatus(BatchStatus.RELEASED);
        int good = b.getQuantityUnits() - waste;
        if (good > 0) {
            stock.adjust(b.getProductId(), good, StockReason.PRODUCTION, b.getBatchCode(), CurrentAdmin.username());
        }
        return b;
    }

    public ProductionBatch reject(Long id, String reason) {
        ProductionBatch b = load(id);
        Asserts.state(b.getStatus() == BatchStatus.PLANNED || b.getStatus() == BatchStatus.IN_PROGRESS,
                "Only a batch that has not been released can be rejected");
        b.setStatus(BatchStatus.REJECTED);
        b.setWasteUnits(b.getQuantityUnits());
        b.setNote((b.getNote() == null ? "" : b.getNote() + " | ") + "Rejected: " + reason);
        return b;
    }

    @Transactional(readOnly = true)
    public ProductionBatch get(Long id) {
        return load(id);
    }

    @Transactional(readOnly = true)
    public CommonPage<ProductionBatch> list(Long productId, BatchStatus status, Integer pageNum, Integer pageSize) {
        return CommonPage.of(batches.search(productId, status, PageParams.of(pageNum, pageSize, Sort.by(Sort.Order.desc("id")))));
    }

    /** Waste against output per product: the number that tells whether a recipe is worth keeping. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> wasteReport() {
        return products.findAll().stream().map(p -> {
            long produced = batches.totalProduced(p.getId());
            long waste = batches.totalWaste(p.getId());
            if (produced == 0 && waste == 0) {
                return null;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("productId", p.getId());
            row.put("productSn", p.getProductSn());
            row.put("name", p.getName());
            row.put("releasedUnits", produced);
            row.put("wasteUnits", waste);
            long total = produced + waste;
            row.put("wastePercent", total == 0 ? 0 : Math.round(waste * 1000.0 / total) / 10.0);
            return row;
        }).filter(java.util.Objects::nonNull).toList();
    }

    private ProductionBatch load(Long id) {
        return Asserts.found(batches.findById(id), "Batch");
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
