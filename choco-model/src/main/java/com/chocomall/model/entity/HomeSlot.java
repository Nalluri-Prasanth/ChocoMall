package com.chocomall.model.entity;

import com.chocomall.model.enums.HomeSlotType;
import jakarta.persistence.*;
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
@Table(name = "sms_home_slot")
public class HomeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "slot_type", nullable = false, length = 30)
    private HomeSlotType slotType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "pic", length = 500)
    private String pic;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "sort", nullable = false)
    private Integer sort = 0;

    @Column(name = "status", nullable = false)
    private boolean status = true;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "note", length = 300)
    private String note;
}
