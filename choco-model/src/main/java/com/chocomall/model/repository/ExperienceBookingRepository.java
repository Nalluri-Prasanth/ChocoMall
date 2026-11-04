package com.chocomall.model.repository;

import com.chocomall.model.entity.ExperienceBooking;
import com.chocomall.model.enums.BookingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceBookingRepository extends JpaRepository<ExperienceBooking, Long> {

    List<ExperienceBooking> findByMemberIdOrderByIdDesc(Long memberId);

    Optional<ExperienceBooking> findByIdAndMemberId(Long id, Long memberId);

    List<ExperienceBooking> findByStatusAndCreateTimeBefore(BookingStatus status, LocalDateTime before);

    Page<ExperienceBooking> findBySessionId(Long sessionId, Pageable pageable);
}
