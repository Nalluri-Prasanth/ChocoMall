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
@Table(name = "oms_personalization_proof")
public class PersonalizationProof {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "artwork_note", length = 1000)
    private String artworkNote;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "approved_by_member", nullable = false)
    private boolean approvedByMember = false;

    @Column(name = "approved_time")
    private LocalDateTime approvedTime;
}
