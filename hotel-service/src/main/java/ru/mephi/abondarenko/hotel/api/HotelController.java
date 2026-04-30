package ru.mephi.abondarenko.hotel.api;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mephi.abondarenko.hotel.api.dto.CreateHotelRequest;
import ru.mephi.abondarenko.hotel.api.dto.CreateRoomRequest;
import ru.mephi.abondarenko.hotel.api.dto.HotelResponse;
import ru.mephi.abondarenko.hotel.api.dto.RoomOccupancyStatsResponse;
import ru.mephi.abondarenko.hotel.api.dto.RoomResponse;
import ru.mephi.abondarenko.hotel.api.dto.UpdateHotelRequest;
import ru.mephi.abondarenko.hotel.api.dto.UpdateRoomRequest;
import ru.mephi.abondarenko.hotel.api.mapper.HotelMapper;
import ru.mephi.abondarenko.hotel.service.HotelService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;
    private final HotelMapper hotelMapper;

    @PostMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelResponse createHotel(@Valid @RequestBody CreateHotelRequest request) {
        return hotelMapper.toResponse(hotelService.createHotel(request));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return hotelMapper.toResponse(hotelService.createRoom(request));
    }

    @PatchMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelResponse updateHotel(@Valid @RequestBody UpdateHotelRequest request) {
        return hotelMapper.toResponse(hotelService.updateHotel(request));
    }

    @DeleteMapping("/hotels/{hotelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteHotel(@PathVariable Long hotelId) {
        hotelService.deleteHotel(hotelId);
    }

    @PatchMapping("/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse updateRoom(@Valid @RequestBody UpdateRoomRequest request) {
        return hotelMapper.toResponse(hotelService.updateRoom(request));
    }

    @DeleteMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRoom(@PathVariable Long roomId) {
        hotelService.deleteRoom(roomId);
    }

    @GetMapping("/hotels")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'INTERNAL')")
    public List<HotelResponse> getHotels() {
        return hotelService.getHotels().stream().map(hotelMapper::toResponse).toList();
    }

    @GetMapping("/rooms/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public List<RoomOccupancyStatsResponse> getRoomOccupancyStats() {
        return hotelService.getRoomOccupancyStats();
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'INTERNAL')")
    public List<RoomResponse> getAvailableRooms(
            @RequestParam(required = false) Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return hotelService.getAvailableRooms(hotelId, startDate, endDate).stream()
                .map(hotelMapper::toResponse)
                .toList();
    }

    @GetMapping("/rooms/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'INTERNAL')")
    public List<RoomResponse> getRecommendedRooms(
            @RequestParam(required = false) Long hotelId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return hotelService.getRecommendedRooms(hotelId, startDate, endDate).stream()
                .map(hotelMapper::toResponse)
                .toList();
    }
}
