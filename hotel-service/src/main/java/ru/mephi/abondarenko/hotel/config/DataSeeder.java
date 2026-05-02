package ru.mephi.abondarenko.hotel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityRequest;
import ru.mephi.abondarenko.hotel.api.dto.CreateHotelRequest;
import ru.mephi.abondarenko.hotel.api.dto.CreateRoomRequest;
import ru.mephi.abondarenko.hotel.domain.Hotel;
import ru.mephi.abondarenko.hotel.domain.Room;
import ru.mephi.abondarenko.hotel.service.HotelService;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final HotelService hotelService;

    @Bean
    CommandLineRunner hotelSeedRunner() {
        return args -> {
            if (!hotelService.getHotels().isEmpty()) {
                return;
            }

            Hotel aurora = hotelService.createHotel(new CreateHotelRequest("Aurora", "Moscow, Tverskaya 10"));
            Room aurora101 = hotelService.createRoom(new CreateRoomRequest(aurora.getId(), "101", true));
            Room aurora102 = hotelService.createRoom(new CreateRoomRequest(aurora.getId(), "102", true));
            hotelService.createRoom(new CreateRoomRequest(aurora.getId(), "103", false));

            Hotel neva = hotelService.createHotel(new CreateHotelRequest("Neva Palace", "Saint Petersburg, Nevsky 20"));
            Room neva201 = hotelService.createRoom(new CreateRoomRequest(neva.getId(), "201", true));
            hotelService.createRoom(new CreateRoomRequest(neva.getId(), "202", true));

            seedBookingHistory(aurora101, "seed-aurora-101", 2);
            seedBookingHistory(aurora102, "seed-aurora-102", 1);
            seedBookingHistory(neva201, "seed-neva-201", 3);
        };
    }

    private void seedBookingHistory(Room room, String prefix, int confirmations) {
        for (int i = 0; i < confirmations; i++) {
            hotelService.confirmAvailability(room.getId(), new AvailabilityRequest(
                    prefix + "-booking-" + i,
                    prefix + "-request-" + i,
                    LocalDate.of(2026, 1, 1 + i * 3),
                    LocalDate.of(2026, 1, 2 + i * 3)));
        }
    }
}
