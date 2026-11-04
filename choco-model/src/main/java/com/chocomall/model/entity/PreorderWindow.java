package com.chocomall.model.entity;

import com.chocomall.model.enums.WindowStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pms_preorder_window")
public class PreorderWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "occasion", length = 100)
    private String occasion;

    @Column(name = "order_cutoff", nullable = false)
    private LocalDateTime orderCutoff;

    @Column(name = "fulfil_from", nullable = false)
    private LocalDate fulfilFrom;

    @Column(name = "fulfil_to", nullable = false)
    private LocalDate fulfilTo;

    @Column(name = "capacity_units", nullable = false)
    private Integer capacityUnits;

    @Column(name = "reserved_units", nullable = false, updatable = false)
    private Integer reservedUnits = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private WindowStatus status = WindowStatus.OPEN;

    @Column(name = "note", length = 500)
    private String note;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
