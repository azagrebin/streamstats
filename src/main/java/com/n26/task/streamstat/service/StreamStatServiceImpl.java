package com.n26.task.streamstat.service;

import com.google.common.base.Preconditions;
import com.n26.task.streamstat.model.Statistics;
import com.n26.task.streamstat.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class StreamStatServiceImpl implements StreamStatService {
    private final ClockService clock;
    private final DelayQueue<ExpiringTransaction> transactions = new DelayQueue<>();
    private final ConcurrentSkipListSet<Transaction> minMaxSet =
            new ConcurrentSkipListSet<>(StreamStatServiceImpl::compareTransactions);
    private final AtomicReference<AvgStat> avgStatRef = new AtomicReference<>(new AvgStat(0L, 0L));
    private final long windowMilli;

    @Autowired
    public StreamStatServiceImpl(ClockService clock,
                                 @Value("${streamstat.window:1}") long window,
                                 @Value("${streamstat.window.unit:MINUTES}") TimeUnit windowUnit) {
        this.clock = clock;
        Preconditions.checkArgument(window > 0, "window must greater than zero");
        long windowMilli = TimeUnit.MILLISECONDS.convert(window, windowUnit);
        Preconditions.checkArgument(clock.getCurrentEpocMilli() > windowMilli,
                "window cannot be bigger than current epoc timestamp");
        this.windowMilli = windowMilli;
    }

    // O(log(n)) where n is number of elements in window
    @Override
    public boolean addTransaction(Transaction transaction) {
        boolean insideCurrentWindow = clock.getCurrentEpocMilli() - windowMilli <= transaction.getTimestamp();
        if (insideCurrentWindow) {
            minMaxSet.add(transaction);
            transactions.add(new ExpiringTransaction(transaction));
            avgStatRef.updateAndGet(avgStat -> avgStat.add(transaction.getAmount()));
        }
        return insideCurrentWindow;
    }

    // O(1)
    @Override
    public Statistics getStatistics() {
        AvgStat avgStat = avgStatRef.get();
        double min = minMaxSet.isEmpty() ? 0.0 : minMaxSet.first().getAmount();
        double max = minMaxSet.isEmpty() ? 0.0 : minMaxSet.last().getAmount();
        return Statistics.builder()
                .withSum(avgStat.sum)
                .withCount(avgStat.count)
                .withAvg(avgStat.avg())
                .withMin(min)
                .withMax(max)
                .build();

    }

    @Override
    public Transaction removeOldestTransaction() throws InterruptedException {
        Transaction transaction = transactions.take().getTransaction();
        removeTransaction(transaction);
        return transaction;
    }

    // O(k * log(n)) where n is number of elements in window and k is number of old elements
    @Override
    public long removeOldTransactions() {
        long lastRemovedTimestamp = clock.getCurrentEpocMilli() - windowMilli;
        while (true) {
            ExpiringTransaction expiringTransaction = transactions.poll();
            if (expiringTransaction == null) {
                break;
            }
            Transaction transaction = expiringTransaction.getTransaction();
            lastRemovedTimestamp = Math.max(lastRemovedTimestamp, transaction.getTimestamp());
            removeTransaction(transaction);
        }
        return lastRemovedTimestamp;
    }

    private void removeTransaction(Transaction transaction) {
        minMaxSet.remove(transaction);
        avgStatRef.updateAndGet(avgStat -> avgStat.remove(transaction.getAmount()));
    }

    private static int compareTransactions(Transaction t1, Transaction t2) {
        int compareAmount = Double.compare(t1.getAmount(), t2.getAmount());
        int compareTs = Long.compare(t1.getTimestamp(), t2.getTimestamp());
        return compareAmount == 0 ? compareTs : compareAmount;
    }

    private static class AvgStat {
        final double sum;
        final long count;

        AvgStat(double sum, long count) {
            this.sum = sum;
            this.count = count;
        }

        double avg() {
            return count == 0L ? 0L : sum / ((double) count);
        }

        AvgStat add(double next) {
            return new AvgStat(sum + next, count + 1);
        }

        AvgStat remove(double old) {
            return new AvgStat(sum - old, count - 1);
        }
    }

    private class ExpiringTransaction implements Delayed {
        private final Transaction transaction;

        ExpiringTransaction(Transaction transaction) {
            this.transaction = transaction;
        }

        Transaction getTransaction() {
            return transaction;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long age = transaction.getTimestamp() + windowMilli - clock.getCurrentEpocMilli();
            return age <= 0L ? 0L : unit.convert(age, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
