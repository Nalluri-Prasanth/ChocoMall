package com.chocomall.model.repository;

import com.chocomall.model.entity.Payment;
import com.chocomall.model.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByIdDesc(Long orderId);

    List<Payment> findByPersonalizationId(Long personalizationId);

    List<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findByTransactionRef(String transactionRef);

    Page<Payment> findByStatusOrderByIdDesc(PaymentStatus status, Pageable pageable);
}
