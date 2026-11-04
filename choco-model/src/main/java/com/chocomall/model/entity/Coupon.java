package com.chocomall.model.entity;

import com.chocomall.model.enums.CouponType;
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
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sms_coupon")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "coupon_type", nullable = false, length = 30)
    private CouponType couponType = CouponType.ALL;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "per_limit", nullable = false)
    private Integer perLimit = 1;

    @Column(name = "min_spend", nullable = false, precision = 10, scale = 2)
    private BigDecimal minSpend = new BigDecimal("0");

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "received_count", nullable = false, updatable = false)
    private Integer receivedCount = 0;

    @Column(name = "used_count", nullable = false, updatable = false)
    private Integer usedCount = 0;

    @Column(name = "code", unique = true, length = 64)
    private String code;

    @Column(name = "member_level_id")
    private Long memberLevelId;

    @Column(name = "status", nullable = false)
    private boolean status = true;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "sms_coupon_product", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "product_id")
    private Set<Long> productIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "sms_coupon_category", joinColumns = @JoinColumn(name = "coupon_id"))
    @Column(name = "category_id")
    private Set<Long> categoryIds = new LinkedHashSet<>();
}
