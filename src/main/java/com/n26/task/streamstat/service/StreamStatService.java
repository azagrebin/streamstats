package com.n26.task.streamstat.service;

import com.n26.task.streamstat.model.Statistics;
import com.n26.task.streamstat.model.Transaction;

public interface StreamStatService {
    boolean addTransaction(Transaction transaction);
    Statistics getStatistics();
    Transaction removeOldestTransaction() throws InterruptedException;
    long removeOldTransactions();
}
