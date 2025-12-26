package com.sensordata.util;

import java.util.*;

/**
 * Thread-safe bounded collection using ArrayDeque with max size limit.
 * Automatically removes oldest elements when capacity is exceeded.
 *
 * Решает проблему: Неограниченный рост памяти в sensorHistoryCache
 */
public class SlidingWindow<T> extends AbstractCollection<T> {
    private final ArrayDeque<T> deque;
    private final int maxSize;
    private final Object lock = new Object();

    /**
     * Creates a SlidingWindow with specified max capacity
     * @param maxSize maximum number of elements to keep
     */
    public SlidingWindow(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive");
        }
        this.maxSize = maxSize;
        this.deque = new ArrayDeque<>(Math.min(maxSize, 16));
    }

    /**
     * Adds an element, removing the oldest if capacity exceeded
     */
    @Override
    public boolean add(T element) {
        synchronized (lock) {
            if (deque.size() >= maxSize) {
                deque.removeFirst();
            }
            deque.addLast(element);
            return true;
        }
    }

    /**
     * Returns iterator over elements (oldest to newest)
     */
    @Override
    public Iterator<T> iterator() {
        synchronized (lock) {
            // Create a copy to avoid concurrent modification issues
            return new ArrayList<>(deque).iterator();
        }
    }

    /**
     * Returns number of elements
     */
    @Override
    public int size() {
        synchronized (lock) {
            return deque.size();
        }
    }

    /**
     * Returns max capacity
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Gets element at index (0 = oldest, size-1 = newest)
     */
    public T get(int index) {
        synchronized (lock) {
            if (index < 0 || index >= deque.size()) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + deque.size());
            }
            return ((ArrayDeque<T>) deque).stream()
                    .skip(index)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Clears all elements
     */
    @Override
    public void clear() {
        synchronized (lock) {
            deque.clear();
        }
    }

    /**
     * Returns copy of all elements as list
     */
    public List<T> toList() {
        synchronized (lock) {
            return new ArrayList<>(deque);
        }
    }

    /**
     * Checks if element is present
     */
    @Override
    public boolean contains(Object o) {
        synchronized (lock) {
            return deque.contains(o);
        }
    }
}
