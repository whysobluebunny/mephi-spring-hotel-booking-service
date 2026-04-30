package ru.mephi.abondarenko.booking.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.abondarenko.booking.api.dto.CreateBookingRequest;
import ru.mephi.abondarenko.booking.api.dto.HotelAvailabilityRequest;
import ru.mephi.abondarenko.booking.api.dto.HotelAvailabilityResponse;
import ru.mephi.abondarenko.booking.api.dto.HotelRoomResponse;
import ru.mephi.abondarenko.booking.domain.Booking;
import ru.mephi.abondarenko.booking.domain.BookingStatus;
import ru.mephi.abondarenko.booking.repository.BookingRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HotelClient hotelClient;

    @Transactional(noRollbackFor = IllegalStateException.class)
    public Booking createBooking(CreateBookingRequest request, String requestId, Jwt jwt) {
        validateDates(request);

        return bookingRepository.findByRequestId(requestId)
                .orElseGet(() -> startAndConfirmBooking(request, requestId, Long.valueOf(jwt.getSubject())));
    }

    @Transactional(readOnly = true)
    public Page<Booking> getBookingsForUser(Jwt jwt, Pageable pageable) {
        return bookingRepository.findAllByUserIdOrderByCreatedAtDesc(Long.valueOf(jwt.getSubject()), pageable);
    }

    @Transactional(readOnly = true)
    public Booking getBooking(String id, Jwt jwt) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        ensureOwner(booking, jwt);
        return booking;
    }

    @Transactional
    public Booking cancelBooking(String id, Jwt jwt) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        ensureOwner(booking, jwt);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        try {
            hotelClient.release(saved.getRoomId(), saved.getRequestId());
        } catch (Exception ex) {
            log.warn("Release failed for bookingId={} requestId={}: {}", saved.getId(), saved.getRequestId(), ex.getMessage());
        }
        return saved;
    }

    private Booking startAndConfirmBooking(CreateBookingRequest request, String requestId, Long userId) {
        Long roomId = request.autoSelect() ? selectRoom(request) : request.roomId();
        if (roomId == null) {
            throw new IllegalArgumentException("Room id is required when autoSelect=false");
        }

        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setRoomId(roomId);
        booking.setStartDate(request.startDate());
        booking.setEndDate(request.endDate());
        booking.setStatus(BookingStatus.PENDING);
        booking.setRequestId(requestId);
        booking.setCreatedAt(OffsetDateTime.now());
        Booking pending = bookingRepository.save(booking);

        log.info("bookingId={} requestId={} status=PENDING", pending.getId(), requestId);
        HotelAvailabilityRequest availabilityRequest = new HotelAvailabilityRequest(
                pending.getId(), requestId, request.startDate(), request.endDate());

        try {
            HotelAvailabilityResponse response = hotelClient.confirmAvailability(roomId, availabilityRequest);
            if (!response.confirmed()) {
                pending.setStatus(BookingStatus.CANCELLED);
                log.info("bookingId={} requestId={} status=CANCELLED reason={}", pending.getId(), requestId, response.message());
                return bookingRepository.save(pending);
            }

            pending.setStatus(BookingStatus.CONFIRMED);
            log.info("bookingId={} requestId={} status=CONFIRMED", pending.getId(), requestId);
            return bookingRepository.save(pending);
        } catch (Exception ex) {
            pending.setStatus(BookingStatus.CANCELLED);
            Booking cancelled = bookingRepository.save(pending);
            log.info("bookingId={} requestId={} status=CANCELLED compensation=START", pending.getId(), requestId);
            try {
                hotelClient.release(roomId, requestId);
                log.info("bookingId={} requestId={} compensation=SUCCESS", pending.getId(), requestId);
            } catch (Exception releaseEx) {
                log.warn("bookingId={} requestId={} compensation=FAILED message={}",
                        pending.getId(), requestId, releaseEx.getMessage());
            }
            throw new IllegalStateException("Booking failed: " + ex.getMessage(), ex);
        }
    }

    private Long selectRoom(CreateBookingRequest request) {
        List<HotelRoomResponse> rooms = hotelClient.getRecommendedRooms(request.hotelId(), request.startDate(), request.endDate());
        if (rooms.isEmpty()) {
            throw new IllegalStateException("No available rooms found");
        }
        return rooms.get(0).id();
    }

    private void validateDates(CreateBookingRequest request) {
        if (!request.startDate().isBefore(request.endDate())) {
            throw new IllegalArgumentException("startDate must be before endDate");
        }
    }

    private void ensureOwner(Booking booking, Jwt jwt) {
        boolean admin = jwt.getClaimAsStringList("roles") != null && jwt.getClaimAsStringList("roles").contains("ADMIN");
        if (!admin && !booking.getUserId().equals(Long.valueOf(jwt.getSubject()))) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
