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
@Table(name = "pms_product_sku")
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sku_code", nullable = false, unique = true, length = 50)
    private String skuCode;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "units", nullable = false)
    private Integer units = 1;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "pic", length = 500)
    private String pic;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
