package com.chocomall.model.entity;

import com.chocomall.model.enums.BatchStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
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
@Table(name = "ops_production_batch")
public class ProductionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_code", nullable = false, unique = true, length = 40)
    private String batchCode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "process_code", length = 10)
    private String processCode;

    @Column(name = "quantity_units", nullable = false)
    private Integer quantityUnits;

    @Column(name = "waste_units", nullable = false)
    private Integer wasteUnits = 0;

    @Column(name = "produced_at")
    private LocalDateTime producedAt;

    @Column(name = "best_before")
    private LocalDate bestBefore;

    @Column(name = "allergen_checked", nullable = false)
    private boolean allergenChecked = false;

    @Column(name = "temper_test_passed")
    private Boolean temperTestPassed;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private BatchStatus status = BatchStatus.PLANNED;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
