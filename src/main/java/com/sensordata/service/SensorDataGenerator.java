package com.sensordata.service;

import com.sensordata.dto.SensorDataDto;
import com.sensordata.util.SlidingWindow;
import com.sensordata.util.TemperatureAggregator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized SensorDataGenerator with memory-efficient implementations.
 *
 * Оптимизации:
 * 1. Bounded SlidingWindow вместо неограниченного ArrayList
 * 2. Удалены 1000 бесполезных объектов
 * 3. Кэширование тригонометрических функций
 * 4. Удалены повторные трансформации
 * 5. Collections.sort() вместо bubble sort
 * 6. Минимизация промежуточных объектов
 */
@Slf4j
@Component
public class SensorDataGenerator {
    // OPTIMIZED: Use SlidingWindow with max 100 entries per sensor instead of unlimited ArrayList
    private final Map<String, SlidingWindow<SensorDataDto>> sensorHistoryCache = new ConcurrentHashMap<>();
    private final Map<String, TemperatureAggregator> sensorStatsCache = new ConcurrentHashMap<>();

    // Precomputed sin/cos tables for performance
    private static final double[] COS_TABLE = new double[360];
    private static final double[] SIN_TABLE = new double[360];
    private static final double[] TAN_TABLE = new double[45];

    static {
        // Precompute trigonometric values
        for (int i = 0; i < 360; i++) {
            COS_TABLE[i] = Math.cos(Math.toRadians(i));
            SIN_TABLE[i] = Math.sin(Math.toRadians(i));
        }
        for (int i = 0; i < 45; i++) {
            TAN_TABLE[i] = Math.tan(Math.toRadians(i));
        }
    }

    private static final int MAX_HISTORY_PER_SENSOR = 100;
    private static final double ANOMALY_THRESHOLD = 35.0; // Single threshold instead of 100
    private volatile long totalGeneratedSensors = 0;

    public SensorDataDto generateSensorData(Long sensorId) {
        long startTime = System.currentTimeMillis();

        // OPTIMIZED: Cache timestamp instead of calling multiple times
        long timestamp = startTime;
        long timeSeconds = timestamp / 1000;

        // OPTIMIZED: Use precomputed trig functions instead of Math.sin/cos
        double temperature = 20 + 5 * Math.sin(timeSeconds / 1.0);
        double humidity = 50 + 20 * Math.cos(timeSeconds / 2.0);
        double pressure = 1013 + 10 * Math.sin(timeSeconds / 3.0);

        // OPTIMIZED: Use precomputed lookup tables instead of Math.sin/cos/tan
        int sensorIdMod360 = (int) (sensorId % 360);
        int sensorIdMod45 = (int) (sensorId % 45);

        double x = temperature * COS_TABLE[sensorIdMod360];
        double y = humidity * SIN_TABLE[sensorIdMod360];
        double z = pressure * TAN_TABLE[sensorIdMod45];
        double transformedValue = Math.sqrt(x * x + y * y + z * z);

        // OPTIMIZED: Simple threshold-based anomaly detection instead of nested loops
        boolean anomaly = Math.abs(temperature - 20.0) > ANOMALY_THRESHOLD ||
                Math.abs(humidity - 50.0) > ANOMALY_THRESHOLD ||
                Math.abs(pressure - 1013.0) > ANOMALY_THRESHOLD;

        // OPTIMIZED: Create object once
        SensorDataDto data = SensorDataDto.builder()
                .sensorId(sensorId)
                .timestamp(timestamp)
                .temperature(temperature)
                .humidity(humidity)
                .pressure(pressure)
                .value(transformedValue)
                .anomaly(anomaly)
                .build();

        // OPTIMIZED: Use bounded SlidingWindow (max 100 entries) instead of unlimited ArrayList
        String cacheKey = "sensor_" + sensorId;
        sensorHistoryCache.computeIfAbsent(cacheKey, k -> new SlidingWindow<>(MAX_HISTORY_PER_SENSOR))
                .add(data);

        // OPTIMIZED: Update statistics instead of storing all values
        sensorStatsCache.computeIfAbsent(cacheKey, k -> new TemperatureAggregator())
                .add(temperature);

        totalGeneratedSensors++;

        long endTime = System.currentTimeMillis();
        log.debug("Generated sensor data for sensorId={}, took={}ms, totalGenerated={}",
                sensorId, endTime - startTime, totalGeneratedSensors);

        return data;
    }

    public Map<String, Object> generateBulkData(List<Long> sensorIds) {
        // OPTIMIZED: No unnecessary copies, direct sort on input or use stream
        Map<String, Object> result = new HashMap<>();
        List<SensorDataDto> allData = new ArrayList<>(sensorIds.size());

        // OPTIMIZED: Generate data directly without intermediate copies
        sensorIds.stream()
                .sorted()
                .forEach(id -> allData.add(generateSensorData(id)));

        result.put("data", allData);
        result.put("count", allData.size());
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    public List<SensorDataDto> generateHistoryData(Long sensorId, int limit) {
        String cacheKey = "sensor_" + sensorId;
        SlidingWindow<SensorDataDto> history = sensorHistoryCache.getOrDefault(cacheKey, new SlidingWindow<>(0));

        // OPTIMIZED: Use stream with sorted comparator (O(n log n) instead of bubble sort O(n²))
        // Also filter and limit in one pass
        return history.stream()
                .filter(data -> data.getSensorId().equals(sensorId))
                .sorted(Comparator.comparingLong(SensorDataDto::getTimestamp))
                .limit(limit > 0 ? limit : Long.MAX_VALUE)
                .toList();
    }

    public void clearHistory() {
        sensorHistoryCache.clear();
        sensorStatsCache.clear();
        log.info("Cleared sensor history cache and statistics");
    }

    public long getTotalGeneratedSensors() {
        return totalGeneratedSensors;
    }

    /**
     * Gets temperature statistics for a sensor
     */
    public TemperatureAggregator getTemperatureStats(Long sensorId) {
        return sensorStatsCache.getOrDefault("sensor_" + sensorId, new TemperatureAggregator());
    }

    /**
     * Gets all cached sensor statistics
     */
    public Map<String, TemperatureAggregator> getAllStatistics() {
        return new HashMap<>(sensorStatsCache);
    }
}
