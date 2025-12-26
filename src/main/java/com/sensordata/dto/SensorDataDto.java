package com.sensordata.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorDataDto {
    @JsonProperty("sensor_id")
    private Long sensorId;

    @JsonProperty("timestamp")
    private Long timestamp;

    @JsonProperty("temperature")
    private Double temperature;

    @JsonProperty("humidity")
    private Double humidity;

    @JsonProperty("pressure")
    private Double pressure;

    @JsonProperty("value")
    private Double value;

    @JsonProperty("anomaly")
    private Boolean anomaly;
}
