package com.chocomall.model.entity;

import com.chocomall.model.enums.SessionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "pms_experience_session")
public class ExperienceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 60;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "booked", nullable = false, updatable = false)
    private Integer booked = 0;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "location", length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 30)
    private SessionStatus status = SessionStatus.OPEN;
}
