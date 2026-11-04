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
@Table(name = "oms_order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_sn", length = 64)
    private String orderSn;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_pic", length = 500)
    private String productPic;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_sn", length = 20)
    private String productSn;

    @Column(name = "product_sku_id")
    private Long productSkuId;

    @Column(name = "product_sku_code", length = 50)
    private String productSkuCode;

    @Column(name = "sku_label", length = 100)
    private String skuLabel;

    @Column(name = "units_per_sku", nullable = false)
    private Integer unitsPerSku = 1;

    @Column(name = "product_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal productPrice;

    @Column(name = "product_quantity", nullable = false)
    private Integer productQuantity;

    @Column(name = "promotion_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal promotionAmount = new BigDecimal("0");

    @Column(name = "coupon_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal couponAmount = new BigDecimal("0");

    @Column(name = "integration_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal integrationAmount = new BigDecimal("0");

    @Column(name = "real_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal realAmount;

    @Column(name = "box_selection", length = 2000)
    private String boxSelection;
}
