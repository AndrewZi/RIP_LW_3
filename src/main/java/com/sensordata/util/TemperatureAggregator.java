package com.sensordata.util;

/**
 * Efficient aggregator for temperature statistics.
 * Stores only 4 values instead of List<Double> with all historical values.
 *
 * Решает проблему: Полная история для статистики (List<Double> вместо агрегированных метрик)
 */
public class TemperatureAggregator {
    private double sum;
    private double min;
    private double max;
    private long count;

    /**
     * Creates aggregator with initial value
     */
    public TemperatureAggregator(double initialValue) {
        this.sum = initialValue;
        this.min = initialValue;
        this.max = initialValue;
        this.count = 1;
    }

    /**
     * Creates empty aggregator
     */
    public TemperatureAggregator() {
        this.sum = 0;
        this.min = Double.MAX_VALUE;
        this.max = Double.MIN_VALUE;
        this.count = 0;
    }

    /**
     * Adds a value to aggregation
     */
    public void add(double value) {
        sum += value;
        min = Math.min(min, value);
        max = Math.max(max, value);
        count++;
    }

    /**
     * Returns average temperature
     */
    public double getAverage() {
        return count == 0 ? 0 : sum / count;
    }

    /**
     * Returns minimum temperature
     */
    public double getMin() {
        return count == 0 ? 0 : min;
    }

    /**
     * Returns maximum temperature
     */
    public double getMax() {
        return count == 0 ? 0 : max;
    }

    /**
     * Returns sum of all values
     */
    public double getSum() {
        return sum;
    }

    /**
     * Returns count of measurements
     */
    public long getCount() {
        return count;
    }

    /**
     * Resets all statistics
     */
    public void reset() {
        sum = 0;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
        count = 0;
    }

    /**
     * Merges another aggregator into this one
     */
    public void merge(TemperatureAggregator other) {
        if (other.count == 0) return;
        sum += other.sum;
        min = Math.min(min, other.min);
        max = Math.max(max, other.max);
        count += other.count;
    }

    @Override
    public String toString() {
        return String.format("TemperatureAggregator{avg=%.2f, min=%.2f, max=%.2f, count=%d}",
                getAverage(), getMin(), getMax(), count);
    }
}
