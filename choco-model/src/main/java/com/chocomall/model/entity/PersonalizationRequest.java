package com.chocomall.model.entity;

import com.chocomall.model.enums.PersonalizationKind;
import com.chocomall.model.enums.PersonalizationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "oms_personalization_request")
public class PersonalizationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_sn", nullable = false, unique = true, length = 64)
    private String requestSn;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "kind", nullable = false, length = 30)
    private PersonalizationKind kind;

    @Column(name = "text_content", length = 300)
    private String textContent;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "occasion_note", length = 500)
    private String occasionNote;

    @Column(name = "requested_date")
    private LocalDate requestedDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private PersonalizationStatus status = PersonalizationStatus.SUBMITTED;

    @Column(name = "quote_amount", precision = 10, scale = 2)
    private BigDecimal quoteAmount;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "proof_version", nullable = false)
    private Integer proofVersion = 0;

    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
