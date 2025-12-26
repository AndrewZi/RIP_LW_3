package com.sensordata.controller;

import com.sensordata.dto.SensorDataDto;
import com.sensordata.service.SensorStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorServerController {
    private final SensorStreamService sensorStreamService;

    @GetMapping(value = "/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<SensorDataDto> streamSensorData(
            @RequestParam(required = false) Long sensorId,
            @RequestParam(required = false) Integer limit) {

        log.info("Received stream request for sensorId={}, limit={}", sensorId, limit);

        if (sensorId != null) {
            return sensorStreamService.streamSensorData(sensorId, limit);
        } else {
            return sensorStreamService.streamMultipleSensors(null, limit);
        }
    }

    @GetMapping(value = "/stream/multi", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<SensorDataDto> streamMultipleSensors(
            @RequestParam(required = false) Integer sensorCount,
            @RequestParam(required = false) Integer limit) {

        log.info("Received multi-sensor stream request for sensorCount={}, limit={}", sensorCount, limit);
        return sensorStreamService.streamMultipleSensors(sensorCount, limit);
    }
}
