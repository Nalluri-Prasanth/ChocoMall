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
@Table(name = "ums_admin")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "icon", length = 500)
    private String icon;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "nick_name", length = 200)
    private String nickName;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "ums_admin_role_relation", joinColumns = @JoinColumn(name = "admin_id"))
    @Column(name = "role_id")
    private Set<Long> roleIds = new LinkedHashSet<>();
}
