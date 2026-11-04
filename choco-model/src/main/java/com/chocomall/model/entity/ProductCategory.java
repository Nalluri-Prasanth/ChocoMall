package com.chocomall.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pms_product_category")
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "level", nullable = false)
    private Integer level = 0;

    @Column(name = "product_count", nullable = false)
    private Integer productCount = 0;

    @Column(name = "nav_status", nullable = false)
    private boolean navStatus = true;

    @Column(name = "show_status", nullable = false)
    private boolean showStatus = true;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "icon", length = 500)
    private String icon;

    @Column(name = "keywords", length = 200)
    private String keywords;

    @Column(name = "description", length = 500)
    private String description;
}
