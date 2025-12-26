package com.sensordata.controller;

import com.sensordata.dto.SensorDataDto;
import com.sensordata.service.SensorClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api/client")
@RequiredArgsConstructor
@Validated
public class SensorClientController {
    private final SensorClientService sensorClientService;

    @GetMapping(value = "/sensors", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<SensorDataDto> getSensors(
            @RequestParam(required = false) Long sensorId,
            @RequestParam(required = false) Integer limit) {

        log.info("Client received request for sensors: sensorId={}, limit={}", sensorId, limit);

        if (sensorId != null) {
            return sensorClientService.getSensorStream(sensorId, limit);
        } else {
            return sensorClientService.getMultipleSensorStream(null, limit);
        }
    }

    @GetMapping(value = "/sensors/multi", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<SensorDataDto> getMultipleSensors(
            @RequestParam(required = false) Integer sensorCount,
            @RequestParam(required = false) Integer limit) {

        log.info("Client received request for multiple sensors: sensorCount={}, limit={}", sensorCount, limit);
        return sensorClientService.getMultipleSensorStream(sensorCount, limit);
    }
}
