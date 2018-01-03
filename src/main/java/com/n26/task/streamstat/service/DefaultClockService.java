package com.n26.task.streamstat.service;

import org.springframework.stereotype.Service;

@Service
public class DefaultClockService implements ClockService {
    @Override
    public long getCurrentEpocMilli() {
        return System.currentTimeMillis();
    }
}
