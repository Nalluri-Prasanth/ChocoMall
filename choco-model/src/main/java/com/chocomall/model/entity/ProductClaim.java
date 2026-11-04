package com.chocomall.model.entity;

import com.chocomall.model.enums.ClaimType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pms_product_claim")
public class ProductClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "claim_type", nullable = false, length = 30)
    private ClaimType claimType;

    @Column(name = "evidence", length = 500)
    private String evidence;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Column(name = "verified_by", length = 64)
    private String verifiedBy;

    @Column(name = "verified_time")
    private LocalDateTime verifiedTime;
}
