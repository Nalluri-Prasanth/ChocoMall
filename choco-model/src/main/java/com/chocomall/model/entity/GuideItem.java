package com.chocomall.model.entity;

import com.chocomall.model.enums.GuideStatus;
import com.chocomall.model.enums.GuideType;
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
@Table(name = "ops_guide_item")
public class GuideItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "guide_type", nullable = false, length = 30)
    private GuideType guideType;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "detail", length = 1500)
    private String detail;

    @Column(name = "purpose", length = 500)
    private String purpose;

    @Column(name = "applies_to", length = 500)
    private String appliesTo;

    @Column(name = "control_note", length = 1000)
    private String controlNote;

    @Column(name = "phase", length = 50)
    private String phase;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "priority", length = 30)
    private Priority priority;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private GuideStatus status = GuideStatus.TODO;

    @Column(name = "implemented_by", length = 300)
    private String implementedBy;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;
}
