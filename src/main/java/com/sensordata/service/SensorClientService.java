package com.sensordata.service;

import com.sensordata.dto.SensorDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorClientService {
    private final WebClient webClient;

    @Value("${app.sensor-server.url:http://localhost:8080}")
    private String sensorServerUrl;

    public Flux<SensorDataDto> getSensorStream(Long sensorId, Integer limit) {
        log.info("Fetching sensor stream from server for sensorId={}, limit={}", sensorId, limit);

        String uri = "/api/sensors/stream";
        var uriSpec = webClient.get().uri(uri, uriBuilder -> {
            if (sensorId != null) {
                uriBuilder.queryParam("sensorId", sensorId);
            }
            if (limit != null) {
                uriBuilder.queryParam("limit", limit);
            }
            return uriBuilder.build();
        });

        return uriSpec
                .retrieve()
                .bodyToFlux(SensorDataDto.class)
                .timeout(Duration.ofSeconds(30))
                .doOnNext(data -> log.debug("Received sensor data: sensorId={}, value={}", data.getSensorId(), data.getValue()))
                .doOnComplete(() -> log.info("Sensor stream completed for sensorId={}", sensorId))
                .doOnCancel(() -> log.warn("Sensor stream cancelled by client for sensorId={}", sensorId))
                .doOnError(error -> log.error("Error receiving sensor stream for sensorId={}: {}", sensorId, error.getMessage(), error))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("Retrying sensor stream request, attempt={}", signal.totalRetries() + 1)))
                .onErrorResume(error -> {
                    log.error("Failed to fetch sensor stream after retries: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    public Flux<SensorDataDto> getMultipleSensorStream(Integer sensorCount, Integer limit) {
        log.info("Fetching multiple sensor stream from server with sensorCount={}, limit={}", sensorCount, limit);

        String uri = "/api/sensors/stream/multi";
        var uriSpec = webClient.get().uri(uri, uriBuilder -> {
            if (sensorCount != null) {
                uriBuilder.queryParam("sensorCount", sensorCount);
            }
            if (limit != null) {
                uriBuilder.queryParam("limit", limit);
            }
            return uriBuilder.build();
        });

        return uriSpec
                .retrieve()
                .bodyToFlux(SensorDataDto.class)
                .timeout(Duration.ofSeconds(30))
                .doOnNext(data -> log.debug("Received multi-sensor data: sensorId={}, temp={}", data.getSensorId(), data.getTemperature()))
                .doOnComplete(() -> log.info("Multi-sensor stream completed"))
                .doOnCancel(() -> log.warn("Multi-sensor stream cancelled by client"))
                .doOnError(error -> log.error("Error receiving multi-sensor stream: {}", error.getMessage()))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .onErrorResume(error -> {
                    log.error("Failed to fetch multi-sensor stream: {}", error.getMessage());
                    return Flux.empty();
                });
    }
}
