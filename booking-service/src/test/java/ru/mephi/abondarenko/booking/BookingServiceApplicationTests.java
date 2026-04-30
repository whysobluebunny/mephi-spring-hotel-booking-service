package ru.mephi.abondarenko.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import ru.mephi.abondarenko.booking.repository.BookingRepository;

@SpringBootTest
@AutoConfigureMockMvc
class BookingServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        bookingRepository.deleteAll();
        circuitBreakerRegistry.circuitBreaker("hotel-service").reset();
    }

    @Test
    void shouldConfirmBooking() throws Exception {
        server.expect(requestTo("http://localhost:8082/internal/rooms/12/confirm-availability"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"confirmed":true,"requestId":"req-1","bookingId":"booking-1","message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        String response = mockMvc.perform(post("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .header("X-Request-Id", "req-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":12,"startDate":"2026-05-01","endDate":"2026-05-03","autoSelect":false}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"status\":\"CONFIRMED\"");
        server.verify();
    }

    @Test
    void shouldCancelWhenRemoteServiceFailsAfterRetries() throws Exception {
        server.expect(requestTo("http://localhost:8082/internal/rooms/15/confirm-availability")).andExpect(method(POST)).andRespond(withServerError());
        server.expect(requestTo("http://localhost:8082/internal/rooms/15/confirm-availability")).andExpect(method(POST)).andRespond(withServerError());
        server.expect(requestTo("http://localhost:8082/internal/rooms/15/confirm-availability")).andExpect(method(POST)).andRespond(withServerError());
        server.expect(requestTo("http://localhost:8082/internal/rooms/15/release")).andExpect(method(POST)).andRespond(withSuccess("""
                {"confirmed":true,"requestId":"req-2","bookingId":"booking-1","message":"released"}
                """, MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("7").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .header("X-Request-Id", "req-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":15,"startDate":"2026-05-01","endDate":"2026-05-03","autoSelect":false}
                                """))
                .andExpect(status().isConflict());

        assertThat(bookingRepository.findByRequestId("req-2")).isPresent();
        assertThat(bookingRepository.findByRequestId("req-2").orElseThrow().getStatus().name()).isEqualTo("CANCELLED");
        server.verify();
    }

    @Test
    void shouldReturnSameBookingForDuplicateRequestId() throws Exception {
        server.expect(requestTo("http://localhost:8082/internal/rooms/21/confirm-availability"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"confirmed":true,"requestId":"dup-1","bookingId":"booking-1","message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        String first = mockMvc.perform(post("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("5").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .header("X-Request-Id", "dup-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":21,"startDate":"2026-05-10","endDate":"2026-05-11","autoSelect":false}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(post("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("5").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .header("X-Request-Id", "dup-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":21,"startDate":"2026-05-10","endDate":"2026-05-11","autoSelect":false}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(bookingRepository.findAll()).hasSize(1);
        assertThat(first).isEqualTo(second);
        server.verify();
    }

    @Test
    void shouldRetryAfterTimeoutAndThenConfirm() throws Exception {
        server.expect(requestTo("http://localhost:8082/internal/rooms/22/confirm-availability"))
                .andExpect(method(POST))
                .andRespond(withException(new java.io.IOException("Read timed out")));
        server.expect(requestTo("http://localhost:8082/internal/rooms/22/confirm-availability"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"confirmed":true,"requestId":"timeout-1","bookingId":"booking-1","message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        String response = mockMvc.perform(post("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("9").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .header("X-Request-Id", "timeout-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":22,"startDate":"2026-06-01","endDate":"2026-06-02","autoSelect":false}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"status\":\"CONFIRMED\"");
        server.verify();
    }

    @Test
    void shouldReturnForbiddenForOtherUsersBooking() throws Exception {
        server.expect(requestTo("http://localhost:8082/internal/rooms/44/confirm-availability"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {"confirmed":true,"requestId":"owned-1","bookingId":"booking-1","message":"ok"}
                        """, MediaType.APPLICATION_JSON));

        String created = mockMvc.perform(post("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("5").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .header("X-Request-Id", "owned-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":44,"startDate":"2026-06-10","endDate":"2026-06-12","autoSelect":false}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bookingId = created.split("\"id\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/bookings/" + bookingId)
                        .with(jwt().jwt(jwt -> jwt.subject("6").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPaginateBookings() throws Exception {
        for (int i = 0; i < 3; i++) {
            String requestId = "page-" + i;
            long roomId = 60 + i;
            server.expect(requestTo("http://localhost:8082/internal/rooms/" + roomId + "/confirm-availability"))
                    .andExpect(method(POST))
                    .andRespond(withSuccess("""
                            {"confirmed":true,"requestId":"%s","bookingId":"booking-1","message":"ok"}
                            """.formatted(requestId), MediaType.APPLICATION_JSON));
        }

        for (int i = 0; i < 3; i++) {
            String requestId = "page-" + i;
            long roomId = 60 + i;
            mockMvc.perform(post("/api/bookings")
                            .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", java.util.List.of("USER")))
                                    .authorities(createAuthorityList("ROLE_USER")))
                            .header("X-Request-Id", requestId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"roomId":%d,"startDate":"2026-07-01","endDate":"2026-07-02","autoSelect":false}
                                    """.formatted(roomId)))
                    .andExpect(status().isOk());
        }

        String response = mockMvc.perform(get("/api/bookings")
                        .with(jwt().jwt(jwt -> jwt.subject("10").claim("roles", java.util.List.of("USER")))
                                .authorities(createAuthorityList("ROLE_USER")))
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"totalElements\":3");
        assertThat(response).contains("\"size\":2");
    }
}
