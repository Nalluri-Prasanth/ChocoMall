package com.chocomall.portal.dto;

import com.chocomall.model.entity.CouponHistory;
import com.chocomall.model.entity.OrderItem;
import com.chocomall.model.entity.ShopOrder;
import com.chocomall.model.enums.ClaimType;
import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.PayMethod;
import com.chocomall.model.enums.PersonalizationKind;
import com.chocomall.model.enums.ProductKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Request and response shapes of the storefront API. Cached shapes are {@link Serializable} for Redis. */
public final class PortalDtos {

    private PortalDtos() {
    }

    public static final String PHONE = "\\+?[0-9]{8,15}";

    // ------------------------------------------------------------------------ authentication & profile

    public record RegisterParam(
            @NotBlank @Size(min = 3, max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "may only contain letters, digits, dots, underscores and hyphens") String username,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Pattern(regexp = PHONE, message = "must be a valid phone number") String telephone,
            @NotBlank String authCode, @Email @Size(max = 100) String email, Boolean marketingConsent) {
    }

    public record LoginParam(@NotBlank String username, @NotBlank String password) {
    }

    public record ResetPasswordParam(@NotBlank @Pattern(regexp = PHONE) String telephone,
                                     @NotBlank @Size(min = 8, max = 72) String password, @NotBlank String authCode) {
    }

    public record TokenResult(String token, String tokenHead, long expiresInSeconds) {
    }

    public record ProfileParam(@Size(max = 64) String nickname, @Size(max = 500) String icon, @Min(0) Integer gender,
                               LocalDate birthday, @Size(max = 64) String city, @Size(max = 100) String job,
                               @Size(max = 200) String personalizedSign, @Email @Size(max = 100) String email,
                               Boolean marketingConsent) {
    }

    public record MemberProfile(Long id, String username, String nickname, String phone, String email, String icon,
                                Integer gender, LocalDate birthday, String city, String job, String personalizedSign,
                                int points, int growth, String levelName, String nextLevelName, Integer growthToNextLevel,
                                boolean marketingConsent) {
    }

    public record AddressParam(@NotBlank @Size(max = 100) String name, @NotBlank @Pattern(regexp = PHONE) String phone,
                               @Size(max = 20) String postCode, @Size(max = 100) String province,
                               @Size(max = 100) String city, @Size(max = 100) String region,
                               @NotBlank @Size(max = 300) String detailAddress, Boolean defaultStatus) {
    }

    public record PreferenceParam(@NotNull Set<Long> flavorIds, boolean consent) {
    }

    // ------------------------------------------------------------------------ catalog views

    public record ClaimBadge(ClaimType type, String evidence) implements Serializable {
    }

    public record ProductCard(Long id, String productSn, String name, String subTitle, String pic, BigDecimal price,
                              BigDecimal originalPrice, ProductKind kind, boolean preorderOnly, boolean buildABox,
                              boolean requiresColdChain, boolean inStock, boolean lowStock, Integer sale,
                              List<String> allergens, List<ClaimBadge> claims, String categoryName, int skuCount) implements Serializable {
    }

    public record SkuView(Long id, String skuCode, String label, Integer units, BigDecimal price) implements Serializable {
    }

    public record NamedItem(Long id, String name) implements Serializable {
    }

    public record WindowView(Long id, String name, String occasion, LocalDateTime orderCutoff, LocalDate fulfilFrom,
                             LocalDate fulfilTo, int remainingUnits) implements Serializable {
    }

    public record SessionView(Long id, Long productId, String productName, LocalDateTime startsAt, Integer durationMinutes,
                              String location, BigDecimal price, int seatsLeft) implements Serializable {
    }

    public record BundleItemView(Long productId, String name, String skuLabel, Integer quantity) implements Serializable {
    }

    public record BundleView(Long id, String name, String description, String pic, BigDecimal price, BigDecimal originalPrice,
                             List<BundleItemView> items) implements Serializable {
    }

    public record ProductDetailView(ProductCard card, String description, Long categoryId, String categoryName, String brandName,
                                    String chocolateType, List<NamedItem> flavors, List<SkuView> skus, Double averageStar,
                                    long commentCount, Integer shelfLifeDays, List<BundleView> bundles,
                                    List<WindowView> preorderWindows, List<SessionView> sessions, Integer weightGrams,
                                    boolean favourite) {
    }

    public record CategoryNode(Long id, String name, String icon, List<CategoryNode> children) implements Serializable {
    }

    public record BrandView(Long id, String name, String region, String associatedMarket, String segment, String typicalFormats,
                            String logo, String referenceUrl, boolean followed) {
    }

    public record SubjectView(Long id, String title, String pic, String description, String category, Integer readCount) implements Serializable {
    }

    public record HomeContent(List<NamedItemWithPic> banners, List<NamedItem> brands, List<ProductCard> newProducts,
                              List<ProductCard> recommendProducts, List<SubjectView> subjects, List<CategoryNode> categories,
                              List<WindowView> preorderWindows, List<BundleView> bundles) implements Serializable {
    }

    public record NamedItemWithPic(Long id, String name, String pic, String url) implements Serializable {
    }

    public record CommentParam(@NotNull Long productId, @NotNull @Min(1) Integer star, @Size(max = 1000) String content) {
    }

    // ------------------------------------------------------------------------ cart & checkout

    public record BoxLineParam(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {
    }

    public record CartAddParam(@NotNull Long productId, Long skuId, @NotNull @Min(1) Integer quantity,
                               @Valid List<BoxLineParam> box) {
    }

    public record CartItemView(Long id, Long productId, Long skuId, String name, String skuLabel, String pic, int quantity,
                               BigDecimal unitPrice, BigDecimal lineTotal, boolean priceChanged, boolean available,
                               boolean requiresColdChain, boolean preorderOnly, List<BoxLineView> box) {
    }

    public record BoxLineView(Long productId, String name, int quantity) {
    }

    public record CartView(List<CartItemView> items, BigDecimal subtotal, int itemCount) {
    }

    public record CheckoutParam(@NotEmpty Set<Long> cartItemIds, @NotNull FulfillmentType fulfillmentType, Long addressId,
                                Long couponHistoryId, @Min(0) Integer useIntegration, Long preorderWindowId,
                                LocalDateTime pickupSlot, boolean giftSleeve, @Size(max = 300) String giftMessage,
                                @Size(max = 500) String note) {
    }

    public record CouponOption(Long couponHistoryId, String name, BigDecimal discount, boolean applicable, String reason) {
    }

    public record CheckoutPreview(List<CartItemView> items, BigDecimal subtotal, BigDecimal bundleDiscount,
                                  BigDecimal couponDiscount, BigDecimal pointsDiscount, BigDecimal deliveryFee,
                                  BigDecimal giftPackagingFee, BigDecimal payAmount, int pointsUsed, int maxRedeemablePoints,
                                  int pointsToEarn, boolean preorder, boolean deliveryAvailable, List<CouponOption> coupons,
                                  List<String> problems, List<String> notices) {
    }

    public record OrderView(ShopOrder order, List<OrderItem> items) {
    }

    public record PayParam(@NotNull Long orderId, @NotNull PayMethod method, @Size(max = 100) String cardToken) {
    }

    public record CancelParam(@Size(max = 200) String reason) {
    }

    public record ReturnParam(@NotNull Long orderId, @NotNull Long productId, @NotNull @Min(1) Integer quantity,
                              @NotBlank @Size(max = 200) String reason, @Size(max = 500) String description,
                              @Size(max = 1000) String proofPics) {
    }

    public record CouponView(Long historyId, Long couponId, String name, BigDecimal amount, BigDecimal minSpend,
                             java.time.LocalDateTime endTime, String useStatus, String type, String note) {
    }

    // ------------------------------------------------------------------------ personalisation & experiences

    public record PersonalizationSubmit(@NotNull PersonalizationKind kind, @NotBlank @Size(max = 60) String text,
                                        @NotNull @Min(1) Integer quantity, @Size(max = 500) String occasionNote,
                                        LocalDate neededBy) {
    }

    public record ApproveParam(@NotNull @Min(1) Integer version) {
    }

    public record ChangesParam(@NotBlank @Size(max = 500) String note) {
    }

    public record TokenParam(@Size(max = 100) String cardToken) {
    }

    public record BookParam(@NotNull Long sessionId, @NotNull @Min(1) Integer seats) {
    }

    public record ShopInfo(String name, String openingHours, String address, boolean deliveryEnabled, BigDecimal deliveryFee,
                           BigDecimal freeDeliveryThreshold, BigDecimal giftPackagingFee, int pickupLeadMinutes,
                           int paymentWindowMinutes, int pointsPerCurrencyUnit, int maxRedeemPercent, int returnWindowDays) {
    }

    public record CouponClaimResult(Long historyId, String message) {
    }

    public record ReadHistoryView(Long productId, String name, String pic, BigDecimal price, LocalDateTime viewedAt) {
    }

    public record CollectionView(Long productId, String name, String pic, BigDecimal price, LocalDateTime addedAt) {
    }

    public record CouponHistoryView(CouponHistory history, String name, BigDecimal amount) {
    }
}
