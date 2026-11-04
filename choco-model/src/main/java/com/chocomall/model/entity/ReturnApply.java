package com.chocomall.model.entity;

import com.chocomall.model.enums.ReturnStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "oms_order_return_apply")
public class ReturnApply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "order_sn", length = 64)
    private String orderSn;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_username", length = 64)
    private String memberUsername;

    @Column(name = "return_amount", precision = 10, scale = 2)
    private BigDecimal returnAmount;

    @Column(name = "return_name", length = 100)
    private String returnName;

    @Column(name = "return_phone", length = 20)
    private String returnPhone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private ReturnStatus status = ReturnStatus.PENDING;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "product_quantity")
    private Integer productQuantity;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "proof_pics", length = 1000)
    private String proofPics;

    @Column(name = "handle_note", length = 500)
    private String handleNote;

    @Column(name = "handle_man", length = 64)
    private String handleMan;

    @Column(name = "receive_man", length = 64)
    private String receiveMan;

    @Column(name = "receive_time")
    private LocalDateTime receiveTime;

    @Column(name = "receive_note", length = 500)
    private String receiveNote;
}
