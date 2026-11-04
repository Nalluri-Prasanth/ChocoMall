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
@Table(name = "cms_food_pairing")
public class FoodPairing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "food_group", length = 100)
    private String foodGroup;

    @Column(name = "food", nullable = false, length = 100)
    private String food;

    @Column(name = "chocolate_combination", length = 200)
    private String chocolateCombination;

    @Column(name = "shop_format", length = 200)
    private String shopFormat;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "priority", nullable = false, length = 30)
    private Priority priority;
}
