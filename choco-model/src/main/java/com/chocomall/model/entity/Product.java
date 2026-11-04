package com.chocomall.model.entity;

import com.chocomall.model.enums.Priority;
import com.chocomall.model.enums.ProductKind;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@SQLRestriction("deleted = false")
@Table(name = "pms_product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "chocolate_type_id")
    private Long chocolateTypeId;

    @Column(name = "product_sn", nullable = false, unique = true, length = 20)
    private String productSn;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "pic", length = 500)
    private String pic;

    @Column(name = "sub_title", length = 300)
    private String subTitle;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "original_price", precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "stock", nullable = false, updatable = false)
    private Integer stock = 0;

    @Column(name = "lock_stock", nullable = false, updatable = false)
    private Integer lockStock = 0;

    @Column(name = "low_stock", nullable = false)
    private Integer lowStock = 10;

    @Column(name = "sale", nullable = false, updatable = false)
    private Integer sale = 0;

    @Column(name = "unit", length = 30)
    private String unit = "piece";

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "kind", nullable = false, length = 30)
    private ProductKind kind = ProductKind.MADE_IN_SHOP;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "launch_tier", nullable = false, length = 30)
    private Priority launchTier = Priority.TEST;

    @Column(name = "selling_format", length = 200)
    private String sellingFormat;

    @Column(name = "sales_purpose", length = 300)
    private String salesPurpose;

    @Column(name = "published", nullable = false)
    private boolean published = false;

    @Column(name = "new_status", nullable = false)
    private boolean newStatus = false;

    @Column(name = "recommend_status", nullable = false)
    private boolean recommendStatus = false;

    @Column(name = "preorder_only", nullable = false)
    private boolean preorderOnly = false;

    @Column(name = "build_a_box", nullable = false)
    private boolean buildABox = false;

    @Column(name = "requires_cold_chain", nullable = false)
    private boolean requiresColdChain = false;

    @Column(name = "allergens_reviewed", nullable = false)
    private boolean allergensReviewed = false;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(name = "ingredient_cost", precision = 10, scale = 2)
    private BigDecimal ingredientCost;

    @Column(name = "packaging_cost", precision = 10, scale = 2)
    private BigDecimal packagingCost;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "selling_fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal sellingFeeRate = new BigDecimal("0");

    @Column(name = "keywords", length = 200)
    private String keywords;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "pms_product_flavor", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "flavor_id")
    private Set<Long> flavorIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "pms_product_allergen", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "allergen_id")
    private Set<Long> allergenIds = new LinkedHashSet<>();
}
