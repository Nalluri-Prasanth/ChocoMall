package com.chocomall.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ums_role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "admin_count", nullable = false)
    private Integer adminCount = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "ums_role_menu_relation", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "menu_id")
    private Set<Long> menuIds = new LinkedHashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "ums_role_resource_relation", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "resource_id")
    private Set<Long> resourceIds = new LinkedHashSet<>();
}
