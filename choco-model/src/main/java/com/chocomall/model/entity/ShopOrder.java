package com.chocomall.model.entity;

import com.chocomall.model.enums.FulfillmentType;
import com.chocomall.model.enums.OrderStatus;
import com.chocomall.model.enums.PayMethod;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oms_order")
public class ShopOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "coupon_history_id")
    private Long couponHistoryId;

    @Column(name = "order_sn", nullable = false, unique = true, length = 64)
    private String orderSn;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "member_username", length = 64)
    private String memberUsername;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "pay_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal payAmount;

    @Column(name = "freight_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal freightAmount = new BigDecimal("0");

    @Column(name = "gift_packaging_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal giftPackagingAmount = new BigDecimal("0");

    @Column(name = "promotion_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal promotionAmount = new BigDecimal("0");

    @Column(name = "integration_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal integrationAmount = new BigDecimal("0");

    @Column(name = "coupon_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal couponAmount = new BigDecimal("0");

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "pay_method", length = 30)
    private PayMethod payMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "fulfillment_type", nullable = false, length = 30)
    private FulfillmentType fulfillmentType;

    @Column(name = "preorder", nullable = false)
    private boolean preorder = false;

    @Column(name = "preorder_window_id")
    private Long preorderWindowId;

    @Column(name = "preorder_units")
    private Integer preorderUnits;

    @Column(name = "pickup_slot_start")
    private LocalDateTime pickupSlotStart;

    @Column(name = "pickup_slot_end")
    private LocalDateTime pickupSlotEnd;

    @Column(name = "delivery_company", length = 100)
    private String deliveryCompany;

    @Column(name = "delivery_sn", length = 100)
    private String deliverySn;

    @Column(name = "auto_confirm_days", nullable = false)
    private Integer autoConfirmDays = 7;

    @Column(name = "integration_earned", nullable = false)
    private Integer integrationEarned = 0;

    @Column(name = "growth_earned", nullable = false)
    private Integer growthEarned = 0;

    @Column(name = "receiver_name", length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", length = 20)
    private String receiverPhone;

    @Column(name = "receiver_post_code", length = 20)
    private String receiverPostCode;

    @Column(name = "receiver_province", length = 100)
    private String receiverProvince;

    @Column(name = "receiver_city", length = 100)
    private String receiverCity;

    @Column(name = "receiver_region", length = 100)
    private String receiverRegion;

    @Column(name = "receiver_detail_address", length = 300)
    private String receiverDetailAddress;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "gift_message", length = 300)
    private String giftMessage;

    @Column(name = "gift_sleeve", nullable = false)
    private boolean giftSleeve = false;

    @Column(name = "use_integration", nullable = false)
    private Integer useIntegration = 0;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "receive_time")
    private LocalDateTime receiveTime;

    @Column(name = "comment_time")
    private LocalDateTime commentTime;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
