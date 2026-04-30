package ru.mephi.abondarenko.hotel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.abondarenko.hotel.domain.Hotel;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}
