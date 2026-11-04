package com.chocomall.model.entity;

import com.chocomall.model.enums.BrandStockingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pms_brand")
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "associated_market", length = 100)
    private String associatedMarket;

    @Column(name = "segment", length = 100)
    private String segment;

    @Column(name = "typical_formats", length = 300)
    private String typicalFormats;

    @Column(name = "shop_role", length = 200)
    private String shopRole;

    @Column(name = "evidence_status", length = 200)
    private String evidenceStatus;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "stocking_status", nullable = false, length = 30)
    private BrandStockingStatus stockingStatus = BrandStockingStatus.CANDIDATE;

    @Column(name = "authorized_supplier", length = 200)
    private String authorizedSupplier;

    @Column(name = "landed_cost_note", length = 300)
    private String landedCostNote;

    @Column(name = "logo", length = 500)
    private String logo;

    @Column(name = "show_status", nullable = false)
    private boolean showStatus = true;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "product_count", nullable = false)
    private Integer productCount = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
