package ru.mephi.abondarenko.hotel.service;

import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityRequest;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityResponse;
import ru.mephi.abondarenko.hotel.api.dto.CreateHotelRequest;
import ru.mephi.abondarenko.hotel.api.dto.CreateRoomRequest;
import ru.mephi.abondarenko.hotel.api.dto.RoomOccupancyStatsResponse;
import ru.mephi.abondarenko.hotel.api.dto.UpdateHotelRequest;
import ru.mephi.abondarenko.hotel.api.dto.UpdateRoomRequest;
import ru.mephi.abondarenko.hotel.domain.Hotel;
import ru.mephi.abondarenko.hotel.domain.Room;
import ru.mephi.abondarenko.hotel.domain.RoomLock;
import ru.mephi.abondarenko.hotel.domain.RoomLockStatus;
import ru.mephi.abondarenko.hotel.repository.HotelRepository;
import ru.mephi.abondarenko.hotel.repository.RoomLockRepository;
import ru.mephi.abondarenko.hotel.repository.RoomRepository;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomLockRepository roomLockRepository;

    @Transactional
    public Hotel createHotel(CreateHotelRequest request) {
        Hotel hotel = new Hotel();
        hotel.setName(request.name());
        hotel.setAddress(request.address());
        return hotelRepository.save(hotel);
    }

    @Transactional
    public Room createRoom(CreateRoomRequest request) {
        Hotel hotel = hotelRepository.findById(request.hotelId())
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));

        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(request.number());
        room.setAvailable(request.available());
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<Hotel> getHotels() {
        return hotelRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<RoomOccupancyStatsResponse> getRoomOccupancyStats() {
        return roomRepository.findAll().stream()
                .map(room -> new RoomOccupancyStatsResponse(
                        room.getId(),
                        room.getHotel().getId(),
                        room.getHotel().getName(),
                        room.getNumber(),
                        room.isAvailable(),
                        room.getTimesBooked(),
                        roomLockRepository.countByRoomIdAndStatus(room.getId(), RoomLockStatus.ACTIVE)))
                .sorted(Comparator.comparing(RoomOccupancyStatsResponse::timesBooked).reversed()
                        .thenComparing(RoomOccupancyStatsResponse::roomId))
                .toList();
    }

    @Transactional
    public Hotel updateHotel(UpdateHotelRequest request) {
        Hotel hotel = hotelRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));

        if (request.name() != null && !request.name().isBlank()) {
            hotel.setName(request.name());
        }
        if (request.address() != null && !request.address().isBlank()) {
            hotel.setAddress(request.address());
        }
        return hotelRepository.save(hotel);
    }

    @Transactional
    public void deleteHotel(Long hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));

        List<Room> rooms = roomRepository.findAllByHotelId(hotelId);
        for (Room room : rooms) {
            roomLockRepository.deleteAll(roomLockRepository.findAllByRoomId(room.getId()));
            roomRepository.delete(room);
        }
        hotelRepository.delete(hotel);
    }

    @Transactional
    public Room updateRoom(UpdateRoomRequest request) {
        Room room = roomRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (request.hotelId() != null && !request.hotelId().equals(room.getHotel().getId())) {
            Hotel hotel = hotelRepository.findById(request.hotelId())
                    .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
            room.setHotel(hotel);
        }
        if (request.number() != null && !request.number().isBlank()) {
            room.setNumber(request.number());
        }
        if (request.available() != null) {
            room.setAvailable(request.available());
        }
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        roomLockRepository.deleteAll(roomLockRepository.findAllByRoomId(roomId));
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms(Long hotelId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        List<Room> rooms = hotelId == null
                ? roomRepository.findAllByAvailableTrue()
                : roomRepository.findAllByHotelIdAndAvailableTrue(hotelId);

        return rooms.stream()
                .filter(room -> roomLockRepository.findOverlappingLocks(
                        room.getId(), RoomLockStatus.ACTIVE, startDate, endDate).isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Room> getRecommendedRooms(Long hotelId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return getAvailableRooms(hotelId, startDate, endDate).stream()
                .sorted(Comparator.comparingLong(Room::getTimesBooked).thenComparing(Room::getId))
                .toList();
    }

    @Transactional
    public AvailabilityResponse confirmAvailability(Long roomId, AvailabilityRequest request) {
        Room room = roomRepository.findWithLockById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.isAvailable()) {
            return new AvailabilityResponse(false, request.requestId(), request.bookingId(), "Room is unavailable");
        }

        return roomLockRepository.findByRequestId(request.requestId())
                .map(existing -> new AvailabilityResponse(
                        existing.getStatus() == RoomLockStatus.ACTIVE,
                        existing.getRequestId(),
                        existing.getBookingId(),
                        "Request already processed"))
                .orElseGet(() -> createLock(room, request));
    }

    private AvailabilityResponse createLock(Room room, AvailabilityRequest request) {
        List<RoomLock> overlaps = roomLockRepository.findOverlappingLocks(
                room.getId(), RoomLockStatus.ACTIVE, request.startDate(), request.endDate());
        if (!overlaps.isEmpty()) {
            return new AvailabilityResponse(false, request.requestId(), request.bookingId(), "Room already booked");
        }

        RoomLock lock = new RoomLock();
        lock.setRoomId(room.getId());
        lock.setBookingId(request.bookingId());
        lock.setRequestId(request.requestId());
        lock.setStartDate(request.startDate());
        lock.setEndDate(request.endDate());
        lock.setStatus(RoomLockStatus.ACTIVE);
        lock.setCreatedAt(java.time.OffsetDateTime.now());
        roomLockRepository.save(lock);

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);

        return new AvailabilityResponse(true, request.requestId(), request.bookingId(), "Availability confirmed");
    }

    @Transactional
    public AvailabilityResponse release(Long roomId, String requestId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        RoomLock lock = roomLockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (!lock.getRoomId().equals(room.getId())) {
            throw new IllegalArgumentException("Request does not belong to room");
        }

        if (lock.getStatus() == RoomLockStatus.RELEASED) {
            return new AvailabilityResponse(true, lock.getRequestId(), lock.getBookingId(), "Lock already released");
        }

        lock.setStatus(RoomLockStatus.RELEASED);
        lock.setReleasedAt(java.time.OffsetDateTime.now());
        roomLockRepository.save(lock);
        return new AvailabilityResponse(true, lock.getRequestId(), lock.getBookingId(), "Lock released");
    }
}
