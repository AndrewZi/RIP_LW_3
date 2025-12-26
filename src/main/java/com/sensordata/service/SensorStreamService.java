package com.sensordata.service;

import com.sensordata.dto.SensorDataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * Optimized SensorStreamService with backpressure and batch processing.
 *
 * Оптимизации:
 * 1. onBackpressureBuffer для управления нагрузкой на слабых клиентов
 * 2. buffer() для батч-обработки данных
 * 3. parallel() для многопоточной обработки
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensorStreamService {
    private final SensorDataGenerator sensorDataGenerator;

    // Configuration
    private static final int BATCH_SIZE = 16;
    private static final int OVERFLOW_BUFFER_SIZE = 512;
    private static final int PARALLEL_WORKERS = 4;

    public Flux<SensorDataDto> streamSensorData(Long sensorId, Integer limit) {
        log.info("Starting optimized sensor stream for sensorId={}, limit={}", sensorId, limit);

        int actualLimit = limit != null && limit > 0 ? limit : 10;

        return Flux.interval(Duration.ofMillis(100))  // Changed to 100ms for better throughput
                .map(tick -> sensorDataGenerator.generateSensorData(sensorId))
                .take(actualLimit)
                // OPTIMIZED: Add backpressure handling for slow subscribers
                .buffer(BATCH_SIZE)  // Batch elements for efficient processing
                .onBackpressureBuffer(OVERFLOW_BUFFER_SIZE)  // Handle slow consumers
                .flatMapIterable(batch -> batch)  // Flatten batches back to individual items
                .doOnComplete(() -> log.info("Optimized sensor stream completed for sensorId={}", sensorId))
                .doOnCancel(() -> log.warn("Optimized sensor stream cancelled for sensorId={}", sensorId))
                .doOnError(error -> log.error("Error in optimized sensor stream for sensorId={}: {}", sensorId, error.getMessage(), error))
                .onErrorResume(error -> {
                    log.error("Recovering from error in optimized sensor stream: {}", error.getMessage());
                    return Flux.empty();
                });
    }

    public Flux<SensorDataDto> streamMultipleSensors(Integer sensorCount, Integer limit) {
        log.info("Starting optimized multi-sensor stream with sensorCount={}, limit={}", sensorCount, limit);

        int count = sensorCount != null && sensorCount > 0 ? sensorCount : 5;
        int totalLimit = limit != null && limit > 0 ? limit : 20;
        int limitPerSensor = Math.max(1, totalLimit / count);

        return Flux.range(1, count)
                // OPTIMIZED: Use parallel processing with concurrency limit
                .parallel(PARALLEL_WORKERS)
                .runOn(Schedulers.parallel())
                .flatMap(sensorId -> streamSensorData((long) sensorId, limitPerSensor))
                .sequential()  // Merge back to sequential stream
                .doOnComplete(() -> log.info("Optimized multi-sensor stream completed"))
                .doOnCancel(() -> log.warn("Optimized multi-sensor stream cancelled"))
                .doOnError(error -> log.error("Error in optimized multi-sensor stream: {}", error.getMessage()));
    }
}
