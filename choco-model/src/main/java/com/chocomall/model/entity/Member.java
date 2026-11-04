package com.chocomall.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ums_member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_id")
    private Long levelId;

    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", length = 64)
    private String nickname;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "icon", length = 500)
    private String icon;

    @Column(name = "gender", nullable = false)
    private Integer gender = 0;

    @Column(name = "birthday")
    private LocalDate birthday;

    @Column(name = "city", length = 64)
    private String city;

    @Column(name = "job", length = 100)
    private String job;

    @Column(name = "personalized_sign", length = 200)
    private String personalizedSign;

    @Column(name = "integration", nullable = false, updatable = false)
    private Integer integration = 0;

    @Column(name = "growth", nullable = false, updatable = false)
    private Integer growth = 0;

    @Column(name = "history_integration", nullable = false, updatable = false)
    private Integer historyIntegration = 0;

    @Column(name = "marketing_consent", nullable = false)
    private boolean marketingConsent = false;
}
