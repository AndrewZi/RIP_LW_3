package com.sensordata.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("sensor_data")
public class SensorData {
    @Id
    private Long id;

    private Long sensorId;

    private Long timestamp;

    private Double temperature;

    private Double humidity;

    private Double pressure;

    private Double value;

    private Boolean anomaly;
}
