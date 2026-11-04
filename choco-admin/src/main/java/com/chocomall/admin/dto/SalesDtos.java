package com.chocomall.admin.dto;

import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.OrderOperateHistory;
import com.chocomall.model.entity.Payment;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.CouponType;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PersonalizationKind;
import com.chocomall.model.enums.ReturnStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Request and response shapes for orders, returns, settings, personalisation, members and marketing. */
public final class SalesDtos {

    private SalesDtos() {
    }

    // ------------------------------------------------------------------------ orders

    public record OrderQuery(String orderSn, String receiver, OrderStatus status, FulfillmentType fulfillmentType,
                             Boolean preorder, LocalDateTime createdFrom, LocalDateTime createdTo,
                             Integer pageNum, Integer pageSize) {
    }

    public record OrderDetail(ShopOrder order, List<OrderItem> items, List<OrderOperateHistory> history,
                              List<Payment> payments, List<CouponHistory> coupons) {
    }

    public record IdsReason(@NotEmpty Set<Long> ids, @Size(max = 200) String reason) {
    }

    public record ShipParam(@NotNull Long orderId, @NotBlank @Size(max = 100) String company,
                            @NotBlank @Size(max = 100) String trackingNumber) {
    }

    public record ManualPaymentParam(@NotNull Long orderId, @NotNull PayMethod method) {
    }

    public record ReceiverParam(@NotBlank @Size(max = 100) String name, @Size(max = 20) String phone,
                                @Size(max = 20) String postCode, @Size(max = 100) String province, @Size(max = 100) String city,
                                @Size(max = 100) String region, @Size(max = 300) String detailAddress) {
    }

    public record NoteParam(@Size(max = 500) String note) {
    }

    // ------------------------------------------------------------------------ returns & settings

    public record ReturnQuery(ReturnStatus status, String orderSn, String member, Integer pageNum, Integer pageSize) {
    }

    public record ReturnHandleParam(@NotNull ReturnStatus status, BigDecimal returnAmount, @Size(max = 500) String handleNote,
                                    @Size(max = 64) String receiveMan, @Size(max = 500) String receiveNote,
                                    boolean restock) {
    }

    public record ReasonParam(@NotBlank @Size(max = 200) String name, Integer sort, Boolean status) {
    }

    public record SettingParam(
            @NotNull @Min(5) Integer normalOrderTimeoutMinutes, @NotNull @Min(1) Integer autoConfirmDays,
            @NotNull @Min(1) Integer commentTimeoutDays, @NotNull @DecimalMin("0") BigDecimal deliveryFee,
            @NotNull @DecimalMin("0") BigDecimal freeDeliveryThreshold, @NotNull @DecimalMin("0") BigDecimal giftPackagingFee,
            @NotNull @Min(1) Integer pointsPerCurrencyUnit, @NotNull @Min(0) Integer maxRedeemPercent,
            @NotNull Boolean deliveryEnabled, @NotNull @Min(0) Integer pickupLeadMinutes,
            @NotNull @Min(0) Integer preorderLeadDays, @NotNull @Min(0) Integer pointsPerCurrencySpent,
            @NotNull @Min(0) Integer returnWindowDays, @NotNull @Min(0) Integer experienceCancelHours) {
    }

    public record LocationParam(@NotBlank @Size(max = 200) String name, @Size(max = 20) String phone,
                                @Size(max = 100) String province, @Size(max = 100) String city, @Size(max = 100) String region,
                                @Size(max = 300) String detailAddress, Boolean pickupEnabled, Boolean returnEnabled,
                                @Size(max = 200) String openingHours) {
    }

    // ------------------------------------------------------------------------ personalisation

    public record QuoteParam(@NotNull @DecimalMin("0.01") BigDecimal quoteAmount, @NotNull @DecimalMin("0") BigDecimal depositAmount,
                             @NotNull @Min(1) Integer leadTimeDays, @Size(max = 500) String adminNote) {
    }

    public record ProofParam(@NotBlank @Size(max = 1000) String artworkNote) {
    }

    public record PersonalizationQuery(com.chocomall.model.enums.PersonalizationStatus status, Integer pageNum, Integer pageSize) {
    }

    // ------------------------------------------------------------------------ members

    public record PointsAdjustParam(@NotNull Integer delta, @NotBlank @Size(max = 200) String note) {
    }

    public record LevelParam(@NotBlank @Size(max = 100) String name, @NotNull @Min(0) Integer growthPoint, Boolean defaultStatus,
                             @NotNull @DecimalMin("0.5") BigDecimal pointsMultiplier, BigDecimal freeDeliveryThreshold,
                             @Size(max = 200) String note) {
    }

    // ------------------------------------------------------------------------ marketing

    public record CouponParam(@NotNull CouponType couponType, @NotBlank @Size(max = 200) String name,
                              @NotNull @Min(1) Integer totalCount, @NotNull @DecimalMin("0.01") BigDecimal amount,
                              @NotNull @Min(1) Integer perLimit, @NotNull @DecimalMin("0") BigDecimal minSpend,
                              @NotNull LocalDateTime startTime, @NotNull LocalDateTime endTime, @Size(max = 500) String note,
                              @Size(max = 64) String code, Long memberLevelId, Boolean status,
                              Set<Long> productIds, Set<Long> categoryIds) {
    }

    public record GrantParam(@NotNull Long couponId, @NotNull Long memberId) {
    }

    public record SlotParam(@NotNull com.chocomall.model.enums.HomeSlotType slotType, Long refId, @Size(max = 200) String name,
                            @Size(max = 500) String pic, @Size(max = 500) String url, Integer sort, Boolean status,
                            LocalDateTime startTime, LocalDateTime endTime, @Size(max = 300) String note) {
    }

    // ------------------------------------------------------------------------ content

    public record SubjectParam(@NotBlank @Size(max = 200) String title, @Size(max = 500) String pic,
                               @Size(max = 500) String description, @Size(max = 4000) String content,
                               @Size(max = 100) String category, Boolean showStatus, Boolean recommendStatus, Integer sort,
                               Set<Long> productIds) {
    }

    public record HelpCategoryParam(@NotBlank @Size(max = 100) String name, Integer sort, Boolean showStatus) {
    }

    public record HelpArticleParam(@NotNull Long categoryId, @NotBlank @Size(max = 200) String title,
                                   @Size(max = 4000) String content, Boolean showStatus) {
    }

    public record PairingParam(@NotBlank @Size(max = 10) String code, @Size(max = 100) String foodGroup,
                               @NotBlank @Size(max = 100) String food, @Size(max = 200) String chocolateCombination,
                               @Size(max = 200) String shopFormat, @NotNull com.chocomall.model.enums.Priority priority) {
    }

    public record ServingParam(@NotBlank @Size(max = 10) String code, @NotBlank @Size(max = 100) String eatingFormat,
                               @Size(max = 300) String howToServe, @Size(max = 200) String suitableProduct,
                               @Size(max = 200) String salesApplication, @NotNull com.chocomall.model.enums.Priority priority) {
    }

    public record IdList(@NotEmpty @Valid List<Long> ids) {
    }
}
