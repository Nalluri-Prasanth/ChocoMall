package com.chocomall.model.entity;

import com.chocomall.model.enums.StockReason;
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
@Table(name = "pms_stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "change_units", nullable = false)
    private Integer changeUnits;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reason", nullable = false, length = 30)
    private StockReason reason;

    @Column(name = "ref_sn", length = 64)
    private String refSn;

    @Column(name = "operator", length = 64)
    private String operator;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
