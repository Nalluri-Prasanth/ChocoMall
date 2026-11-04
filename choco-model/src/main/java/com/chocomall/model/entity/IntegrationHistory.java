package com.chocomall.model.entity;

import com.chocomall.model.enums.IntegrationChangeType;
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
@Table(name = "ums_integration_history")
public class IntegrationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "change_type", nullable = false, length = 30)
    private IntegrationChangeType changeType;

    @Column(name = "change_count", nullable = false)
    private Integer changeCount;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "order_sn", length = 64)
    private String orderSn;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
