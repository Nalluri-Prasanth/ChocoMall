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
@Table(name = "pms_flavor")
public class Flavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "flavor_group", length = 100)
    private String flavorGroup;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "chocolate_base", length = 100)
    private String chocolateBase;

    @Column(name = "application", length = 300)
    private String application;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "priority", nullable = false, length = 30)
    private Priority priority;
}
