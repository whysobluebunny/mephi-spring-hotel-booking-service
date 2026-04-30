package ru.mephi.abondarenko.hotel.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.mephi.abondarenko.hotel.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByAvailableTrue();

    List<Room> findAllByHotelIdAndAvailableTrue(Long hotelId);

    List<Room> findAllByHotelId(Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Room> findWithLockById(Long id);
}
