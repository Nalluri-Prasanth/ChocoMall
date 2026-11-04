package com.chocomall.admin.dto;

import com.chocomall.model.enums.GuideStatus;
import com.chocomall.model.enums.StockReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes for stock, planning (preorders, sessions, bundles), production and the playbook. */
public final class OpsDtos {

    private OpsDtos() {
    }

    public record StockRow(Long productId, String productSn, String name, String unit, int stock, int locked, int available,
                           int lowStock, boolean low) {
    }

    public record StockAdjustParam(@NotNull Long productId, @NotNull Integer delta, @NotNull StockReason reason,
                                   @Size(max = 64) String reference) {
    }

    public record StockCountParam(@NotNull Long productId, @NotNull @Min(0) Integer counted) {
    }

    public record WindowParam(@NotBlank @Size(max = 200) String name, @Size(max = 100) String occasion,
                              @NotNull LocalDateTime orderCutoff, @NotNull LocalDate fulfilFrom, @NotNull LocalDate fulfilTo,
                              @NotNull @Min(1) Integer capacityUnits, @Size(max = 500) String note) {
    }

    public record SessionParam(@NotNull Long productId, @NotNull LocalDateTime startsAt, Integer durationMinutes,
                               @NotNull @Min(1) Integer capacity, BigDecimal price, @Size(max = 200) String location) {
    }

    public record BundleItemParam(@NotNull Long productId, Long skuId, @NotNull @Min(1) Integer quantity) {
    }

    public record BundleParam(@NotBlank @Size(max = 200) String name, @Size(max = 500) String description,
                              @Size(max = 500) String pic, @NotNull BigDecimal price, BigDecimal originalPrice,
                              LocalDateTime startTime, LocalDateTime endTime, Boolean status, Integer sort,
                              @NotEmpty @Valid List<BundleItemParam> items) {
    }

    public record BatchParam(@NotNull Long productId, @Size(max = 10) String processCode,
                             @NotNull @Min(1) Integer quantityUnits, LocalDateTime producedAt, @Size(max = 500) String note) {
    }

    public record BatchReleaseParam(boolean allergenChecked, Boolean temperTestPassed, LocalDate bestBefore,
                                    @Min(0) Integer wasteUnits) {
    }

    public record RejectParam(@NotBlank @Size(max = 500) String reason) {
    }

    public record GuideUpdateParam(@NotNull GuideStatus status, @Size(max = 300) String implementedBy) {
    }
}
