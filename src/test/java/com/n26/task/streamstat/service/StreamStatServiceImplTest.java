package com.n26.task.streamstat.service;

import com.n26.task.streamstat.model.Statistics;
import com.n26.task.streamstat.model.Transaction;
import com.n26.task.streamstat.service.util.ControllableRunnable;
import com.n26.task.streamstat.service.util.RunnableControl;
import org.junit.Test;

import java.util.Comparator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class StreamStatServiceImplTest {
    private static final int PRODUCERS_NUM = 5;
    private static final long WINDOW_SIZE_SEC = 1;
    private static final long WINDOW_SIZE_MILLI = TimeUnit.MILLISECONDS.convert(WINDOW_SIZE_SEC, TimeUnit.SECONDS);
    private static final int TEST_ATTEMPS = 3;
    private static final int PRODUCE_MAX_PERIOD_MILLI = 100;
    private static final Random random = new Random();
    private final ConcurrentSkipListSet<Transaction> transactions = new ConcurrentSkipListSet<>(
            Comparator.comparingLong(Transaction::getTimestamp)
                    .thenComparing(Transaction::getAmount));
    private StreamStatService streamStatService;

    @Test
    public void test() throws Exception {
        streamStatService = new StreamStatServiceImpl(
                new DefaultClockService(), WINDOW_SIZE_SEC, TimeUnit.SECONDS);

        transactions.clear();
        RunnableControl control = new RunnableControl(PRODUCERS_NUM);
        ExecutorService executorService = Executors.newFixedThreadPool(PRODUCERS_NUM);
        IntStream.range(0, PRODUCERS_NUM).forEach(p ->
            executorService.submit(
                    new ControllableRunnable(this::injectRandomTransaction, control, PRODUCE_MAX_PERIOD_MILLI))
        );

        for (int i = 0; i < TEST_ATTEMPS; i++) {
            Thread.sleep(WINDOW_SIZE_MILLI * 2);
            control.pause();
            long oldestTimestamp = streamStatService.removeOldTransactions();
            long actualWindow = System.currentTimeMillis() - oldestTimestamp;
            assertTrue(
                    "Actual window is much larger than expected with accuracy",
                    actualWindow < WINDOW_SIZE_MILLI + PRODUCE_MAX_PERIOD_MILLI);
            assertThat("Statistics check failed",
                    streamStatService.getStatistics(),
                    is(equalTo(calcExpectedStatistics(oldestTimestamp))));
            control.resume();
        }

        control.stop();
        executorService.shutdown();
        executorService.awaitTermination(WINDOW_SIZE_SEC, TimeUnit.SECONDS);
    }

    private void injectRandomTransaction() {
        double amount = random.nextDouble() * 100;
        long timeSkewMilli = random.nextInt((int)(WINDOW_SIZE_MILLI));
        long timestamp = System.currentTimeMillis() - timeSkewMilli;
        Transaction transaction = new Transaction(amount, timestamp);
        if (streamStatService.addTransaction(transaction)) {
            transactions.add(transaction);
        }
    }

    private Statistics calcExpectedStatistics(long oldestTimestamp) {
        Transaction oldestTransaction = new Transaction(0.0, oldestTimestamp + 1);
        Set<Transaction> windowTransactions = transactions.tailSet(oldestTransaction);
        double sum = windowTransactions.stream().mapToDouble(Transaction::getAmount).sum();
        long count = windowTransactions.stream().mapToDouble(Transaction::getAmount).count();
        double min = windowTransactions.stream().mapToDouble(Transaction::getAmount).min().orElse(0.0);
        double max = windowTransactions.stream().mapToDouble(Transaction::getAmount).max().orElse(0.0);
        return Statistics.builder()
                .withSum(sum)
                .withCount(count)
                .withAvg(sum / (double) count)
                .withMin(min)
                .withMax(max)
                .build();
    }
}