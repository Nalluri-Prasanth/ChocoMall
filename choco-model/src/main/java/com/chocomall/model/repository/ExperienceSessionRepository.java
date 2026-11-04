package com.chocomall.model.repository;

import com.chocomall.model.entity.ExperienceSession;
import com.chocomall.model.enums.SessionStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExperienceSessionRepository extends JpaRepository<ExperienceSession, Long> {

    List<ExperienceSession> findByProductIdAndStatusAndStartsAtAfterOrderByStartsAt(Long productId, SessionStatus status, LocalDateTime now);

    List<ExperienceSession> findByStatusAndStartsAtAfterOrderByStartsAt(SessionStatus status, LocalDateTime now);

    @Modifying
    @Query("update ExperienceSession s set s.booked = s.booked + :seats where s.id = :id and s.status = com.chocomall.model.enums.SessionStatus.OPEN and s.startsAt > :now and s.booked + :seats <= s.capacity")
    int reserveSeats(@Param("id") Long id, @Param("seats") int seats, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update ExperienceSession s set s.booked = s.booked - :seats where s.id = :id and s.booked >= :seats")
    int releaseSeats(@Param("id") Long id, @Param("seats") int seats);
}
