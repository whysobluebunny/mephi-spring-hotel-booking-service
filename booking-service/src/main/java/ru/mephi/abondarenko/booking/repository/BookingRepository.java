package ru.mephi.abondarenko.booking.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.abondarenko.booking.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, String> {

    Optional<Booking> findByRequestId(String requestId);

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
