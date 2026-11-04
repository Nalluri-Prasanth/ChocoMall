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
@Table(name = "cms_subject")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "pic", length = 500)
    private String pic;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "content", length = 4000)
    private String content;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "show_status", nullable = false)
    private boolean showStatus = true;

    @Column(name = "recommend_status", nullable = false)
    private boolean recommendStatus = false;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "read_count", nullable = false)
    private Integer readCount = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "cms_subject_product", joinColumns = @JoinColumn(name = "subject_id"))
    @Column(name = "product_id")
    private Set<Long> productIds = new LinkedHashSet<>();
}
