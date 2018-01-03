package com.n26.task.streamstat.service;

import com.n26.task.streamstat.controller.StreamStatController;
import com.n26.task.streamstat.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

// background garbage collection for expired transactions (optional optimisation)
@Service
public class CleanupOldTransService {
    private static Logger logger = LoggerFactory.getLogger(StreamStatController.class);

    private final StreamStatService streamStatService;
    private final ClockService clock;
    private final int workersNum;
    private final ExecutorService executorService;

    @Autowired
    public CleanupOldTransService(StreamStatService streamStatService,
                                  ClockService clock,
                                  @Value("${streamstat.cleanup.workers.num:0}") int workersNum) {
        this.streamStatService = streamStatService;
        this.clock = clock;
        this.workersNum = workersNum == 0 ? Runtime.getRuntime().availableProcessors() : workersNum;
        executorService = this.workersNum > 0 ? Executors.newFixedThreadPool(this.workersNum) : null;
    }

    @PostConstruct
    public void start() {
        if (executorService != null) {
            IntStream.range(0, workersNum).forEach(i -> executorService.submit(this::removeOldTransactions));
        }
    }

    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private void removeOldTransactions() {
        while (true) {
            try {
                Transaction transaction = streamStatService.removeOldestTransaction();
                logger.debug("Remove old transaction {} after {} ms",
                        transaction, clock.getCurrentEpocMilli() - transaction.getTimestamp());
            } catch (InterruptedException e) {
                return;
            }
        }
    }
}
