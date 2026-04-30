package ru.mephi.abondarenko.booking.api;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mephi.abondarenko.booking.api.dto.BookingResponse;
import ru.mephi.abondarenko.booking.api.dto.CreateBookingRequest;
import ru.mephi.abondarenko.booking.api.mapper.BookingMapper;
import ru.mephi.abondarenko.booking.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;

    @PostMapping
    public BookingResponse create(
            @Valid @RequestBody CreateBookingRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String effectiveRequestId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        return bookingMapper.toResponse(bookingService.createBooking(request, effectiveRequestId, jwt));
    }

    @GetMapping
    public Page<BookingResponse> getMyBookings(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return bookingService.getBookingsForUser(jwt, PageRequest.of(page, size)).map(bookingMapper::toResponse);
    }

    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return bookingMapper.toResponse(bookingService.getBooking(id, jwt));
    }

    @DeleteMapping("/{id}")
    public BookingResponse cancel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return bookingMapper.toResponse(bookingService.cancelBooking(id, jwt));
    }
}
