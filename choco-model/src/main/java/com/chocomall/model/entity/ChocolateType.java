package com.chocomall.model.entity;

import com.chocomall.model.enums.Priority;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pms_chocolate_type")
public class ChocolateType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "classification", length = 100)
    private String classification;

    @Column(name = "distinguishing", length = 500)
    private String distinguishing;

    @Column(name = "best_shop_use", length = 300)
    private String bestShopUse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "priority", nullable = false, length = 30)
    private Priority priority;
}
