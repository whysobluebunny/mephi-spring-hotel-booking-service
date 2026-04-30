package ru.mephi.abondarenko.hotel.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mephi.abondarenko.hotel.domain.RoomLock;
import ru.mephi.abondarenko.hotel.domain.RoomLockStatus;

public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {

    Optional<RoomLock> findByRequestId(String requestId);

    List<RoomLock> findAllByRoomId(Long roomId);

    long countByRoomIdAndStatus(Long roomId, RoomLockStatus status);

    @Query("""
            select l from RoomLock l
            where l.roomId = :roomId
              and l.status = :status
              and l.startDate < :endDate
              and l.endDate > :startDate
            """)
    List<RoomLock> findOverlappingLocks(Long roomId, RoomLockStatus status, LocalDate startDate, LocalDate endDate);
}
