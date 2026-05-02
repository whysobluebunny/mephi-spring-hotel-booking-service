package ru.mephi.abondarenko.booking.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mephi.abondarenko.booking.api.dto.HotelAvailabilityRequest;
import ru.mephi.abondarenko.booking.api.dto.HotelAvailabilityResponse;
import ru.mephi.abondarenko.booking.api.dto.HotelRoomResponse;
import ru.mephi.abondarenko.booking.api.dto.ReleaseRequest;
import ru.mephi.abondarenko.booking.logging.TraceIdFilter;
import ru.mephi.abondarenko.booking.security.JwtService;

@Slf4j
@Component
public class HotelClient {

    private final RestTemplate restTemplate;
    private final JwtService jwtService;
    private final CircuitBreaker circuitBreaker;

    public HotelClient(RestTemplate restTemplate, JwtService jwtService, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restTemplate = restTemplate;
        this.jwtService = jwtService;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("hotel-service");
    }

    @Value("${app.integration.hotel-service-url:http://localhost:8082}")
    private String hotelServiceUrl;

    @Value("${app.integration.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.integration.initial-backoff-ms:200}")
    private long initialBackoffMs;

    public List<HotelRoomResponse> getRecommendedRooms(Long hotelId, LocalDate startDate, LocalDate endDate) {
        String url = UriComponentsBuilder.fromHttpUrl(hotelServiceUrl + "/api/rooms/recommend")
                .queryParam("hotelId", hotelId)
                .queryParam("startDate", startDate)
                .queryParam("endDate", endDate)
                .toUriString();

        ResponseEntity<HotelRoomResponse[]> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(internalHeaders()),
                HotelRoomResponse[].class);
        return Arrays.asList(response.getBody() == null ? new HotelRoomResponse[0] : response.getBody());
    }

    public HotelAvailabilityResponse confirmAvailability(Long roomId, HotelAvailabilityRequest request) {
        String url = hotelServiceUrl + "/internal/rooms/" + roomId + "/confirm-availability";
        HttpEntity<HotelAvailabilityRequest> entity = new HttpEntity<>(request, internalHeaders());
        return executeWithRetry(() -> restTemplate.postForObject(url, entity, HotelAvailabilityResponse.class));
    }

    public void release(Long roomId, String requestId) {
        String url = hotelServiceUrl + "/internal/rooms/" + roomId + "/release";
        HttpEntity<ReleaseRequest> entity = new HttpEntity<>(new ReleaseRequest(requestId), internalHeaders());
        executeWithRetry(() -> restTemplate.postForObject(url, entity, HotelAvailabilityResponse.class));
    }

    private HttpHeaders internalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtService.generateInternalToken());
        String traceId = MDC.get(TraceIdFilter.TRACE_MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            headers.set(TraceIdFilter.TRACE_HEADER, traceId);
        }
        return headers;
    }

    private <T> T executeWithRetry(RemoteAction<T> remoteAction) {
        if (!circuitBreaker.tryAcquirePermission()) {
            throw new IllegalStateException("Hotel service circuit breaker is open");
        }
        long backoff = initialBackoffMs;
        RestClientException lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = remoteAction.run();
                circuitBreaker.onSuccess(0, java.util.concurrent.TimeUnit.MILLISECONDS);
                return result;
            } catch (RestClientException ex) {
                lastError = ex;
                circuitBreaker.onError(0, java.util.concurrent.TimeUnit.MILLISECONDS, ex);
                log.warn("Hotel service attempt {} failed: {}", attempt, ex.getMessage());
                if (attempt == maxAttempts) {
                    break;
                }
                sleep(backoff);
                backoff *= 2;
            }
        }
        throw lastError == null ? new RestClientException("Hotel service call failed") : lastError;
    }

    private void sleep(long timeoutMs) {
        try {
            Thread.sleep(timeoutMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Retry interrupted", ex);
        }
    }

    @FunctionalInterface
    private interface RemoteAction<T> {
        T run();
    }
}
