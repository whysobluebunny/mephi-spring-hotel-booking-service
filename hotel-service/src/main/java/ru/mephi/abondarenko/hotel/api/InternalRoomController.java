package ru.mephi.abondarenko.hotel.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityRequest;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityResponse;
import ru.mephi.abondarenko.hotel.api.dto.ReleaseRequest;
import ru.mephi.abondarenko.hotel.service.HotelService;

@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
public class InternalRoomController {

    private final HotelService hotelService;

    @PostMapping("/{roomId}/confirm-availability")
    @PreAuthorize("hasRole('INTERNAL')")
    public AvailabilityResponse confirmAvailability(
            @PathVariable Long roomId,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return hotelService.confirmAvailability(roomId, request);
    }

    @PostMapping("/{roomId}/release")
    @PreAuthorize("hasRole('INTERNAL')")
    public AvailabilityResponse release(@PathVariable Long roomId, @Valid @RequestBody ReleaseRequest request) {
        return hotelService.release(roomId, request.requestId());
    }
}
