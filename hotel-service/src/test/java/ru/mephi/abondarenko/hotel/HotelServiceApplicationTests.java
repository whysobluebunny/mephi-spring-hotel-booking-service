package ru.mephi.abondarenko.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityRequest;
import ru.mephi.abondarenko.hotel.api.dto.AvailabilityResponse;
import ru.mephi.abondarenko.hotel.service.HotelService;

@SpringBootTest
@AutoConfigureMockMvc
class HotelServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelService hotelService;

    @Test
    void shouldCreateAndRecommendRooms() throws Exception {
        String hotelBody = """
                {"name":"Vector","address":"Moscow"}
                """;

        String roomBody = """
                {"hotelId":1,"number":"999","available":true}
                """;

        mockMvc.perform(post("/api/hotels")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(hotelBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isOk());

        String content = mockMvc.perform(get("/api/rooms/recommend")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .param("startDate", "2026-04-24")
                        .param("endDate", "2026-04-26"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("\"number\":\"999\"");
    }

    @Test
    void shouldUpdateAndDeleteHotelAndRoom() throws Exception {
        String createHotelResponse = mockMvc.perform(post("/api/hotels")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Orbit","address":"Saint Petersburg"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(createHotelResponse).contains("\"name\":\"Orbit\"");

        String createRoomResponse = mockMvc.perform(post("/api/rooms")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hotelId":2,"number":"201","available":true}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(createRoomResponse).contains("\"number\":\"201\"");

        String updatedHotel = mockMvc.perform(patch("/api/hotels")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":2,"name":"Orbit Plaza","address":"Kazan"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(updatedHotel).contains("Orbit Plaza").contains("Kazan");

        String updatedRoom = mockMvc.perform(patch("/api/rooms")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":4,"number":"202","available":false}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(updatedRoom).contains("\"number\":\"202\"").contains("\"available\":false");

        mockMvc.perform(delete("/api/rooms/4")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/hotels/2")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectParallelOverlappingConfirmations() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);

        Future<AvailabilityResponse> first = executor.submit(() -> {
            gate.await();
            return hotelService.confirmAvailability(1L, new AvailabilityRequest(
                    "booking-A", "parallel-A", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)));
        });
        Future<AvailabilityResponse> second = executor.submit(() -> {
            gate.await();
            return hotelService.confirmAvailability(1L, new AvailabilityRequest(
                    "booking-B", "parallel-B", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3)));
        });

        gate.countDown();
        AvailabilityResponse firstResult = first.get();
        AvailabilityResponse secondResult = second.get();
        executor.shutdown();

        assertThat(java.util.List.of(firstResult.confirmed(), secondResult.confirmed()))
                .containsExactlyInAnyOrder(true, false);
    }

    @Test
    void shouldReturnRoomStatsForAdmin() throws Exception {
        String content = mockMvc.perform(get("/api/rooms/stats")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("timesBooked");
    }

    @Test
    void shouldReturnHotelAnalyticsForAdmin() throws Exception {
        String content = mockMvc.perform(get("/api/hotels/analytics")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", java.util.List.of("ADMIN")))
                                .authorities(createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(content).contains("hotelName").contains("leastBookedRoomNumber").contains("mostBookedRoomNumber");
    }
}
