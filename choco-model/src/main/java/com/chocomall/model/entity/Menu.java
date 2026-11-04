package com.chocomall.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ums_menu")
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id", nullable = false)
    private Long parentId = 0L;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "level", nullable = false)
    private Integer level = 0;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "icon", length = 200)
    private String icon;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
