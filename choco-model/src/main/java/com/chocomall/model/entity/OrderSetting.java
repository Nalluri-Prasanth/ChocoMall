package com.chocomall.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "oms_order_setting")
public class OrderSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "normal_order_timeout_minutes", nullable = false)
    private Integer normalOrderTimeoutMinutes = 30;

    @Column(name = "auto_confirm_days", nullable = false)
    private Integer autoConfirmDays = 7;

    @Column(name = "comment_timeout_days", nullable = false)
    private Integer commentTimeoutDays = 15;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = new BigDecimal("4.50");

    @Column(name = "free_delivery_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold = new BigDecimal("40.00");

    @Column(name = "gift_packaging_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal giftPackagingFee = new BigDecimal("2.00");

    @Column(name = "points_per_currency_unit", nullable = false)
    private Integer pointsPerCurrencyUnit = 100;

    @Column(name = "max_redeem_percent", nullable = false)
    private Integer maxRedeemPercent = 20;

    @Column(name = "delivery_enabled", nullable = false)
    private boolean deliveryEnabled = true;

    @Column(name = "pickup_lead_minutes", nullable = false)
    private Integer pickupLeadMinutes = 30;

    @Column(name = "preorder_lead_days", nullable = false)
    private Integer preorderLeadDays = 3;

    @Column(name = "points_per_currency_spent", nullable = false)
    private Integer pointsPerCurrencySpent = 1;

    @Column(name = "return_window_days", nullable = false)
    private Integer returnWindowDays = 7;

    @Column(name = "experience_cancel_hours", nullable = false)
    private Integer experienceCancelHours = 24;
}
