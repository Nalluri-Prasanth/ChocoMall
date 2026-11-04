package com.chocomall.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ums_member_level")
public class MemberLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "growth_point", nullable = false)
    private Integer growthPoint = 0;

    @Column(name = "default_status", nullable = false)
    private boolean defaultStatus = false;

    @Column(name = "points_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal pointsMultiplier = new BigDecimal("1.00");

    @Column(name = "free_delivery_threshold", precision = 10, scale = 2)
    private BigDecimal freeDeliveryThreshold;

    @Column(name = "note", length = 200)
    private String note;
}
