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
@Table(name = "cms_serving_format")
public class ServingFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "eating_format", nullable = false, length = 100)
    private String eatingFormat;

    @Column(name = "how_to_serve", length = 300)
    private String howToServe;

    @Column(name = "suitable_product", length = 200)
    private String suitableProduct;

    @Column(name = "sales_application", length = 200)
    private String salesApplication;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "priority", nullable = false, length = 30)
    private Priority priority;
}
