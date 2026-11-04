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
@Table(name = "cms_help")
public class HelpArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", length = 4000)
    private String content;

    @Column(name = "show_status", nullable = false)
    private boolean showStatus = true;

    @Column(name = "read_count", nullable = false)
    private Integer readCount = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}
