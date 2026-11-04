package com.chocomall.model.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ops_guide_source")
public class GuideSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source", nullable = false, length = 200)
    private String source;

    @Column(name = "scope", length = 500)
    private String scope;

    @Column(name = "checked_on")
    private LocalDate checkedOn;

    @Column(name = "reference_url", length = 500)
    private String referenceUrl;
}
