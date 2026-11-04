package com.chocomall.model.entity;

import com.chocomall.model.enums.CouponGetType;
import com.chocomall.model.enums.CouponUseStatus;
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
@Table(name = "sms_coupon_history")
public class CouponHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "coupon_code", length = 64)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "get_type", nullable = false, length = 30)
    private CouponGetType getType;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "use_status", nullable = false, length = 30)
    private CouponUseStatus useStatus = CouponUseStatus.UNUSED;

    @Column(name = "use_time")
    private LocalDateTime useTime;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_sn", length = 64)
    private String orderSn;
}
