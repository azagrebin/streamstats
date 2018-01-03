package com.n26.task.streamstat.controller;

import com.n26.task.streamstat.model.Statistics;
import com.n26.task.streamstat.model.Transaction;
import com.n26.task.streamstat.service.StreamStatService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class StreamStatController {
    private static Logger logger = LoggerFactory.getLogger(StreamStatController.class);

    private static final ResponseEntity CREATED = ResponseEntity.status(HttpStatus.CREATED).build();
    private static final ResponseEntity NO_CONTENT = ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    private final StreamStatService streamStatService;

    @Autowired
    public StreamStatController(StreamStatService streamStatService) {
        this.streamStatService = streamStatService;
    }

    @RequestMapping(path = "/transactions", method = RequestMethod.POST, consumes = "application/json")
    @ApiOperation(
            value = "Adds new transaction to the current window"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "transaction is added to current window"),
            @ApiResponse(code = 204, message = "transaction is dropped because it is out of current window")
    })
    public ResponseEntity<?> addTransaction(@RequestBody Transaction transaction) {
        streamStatService.removeOldTransactions();
        boolean added = streamStatService.addTransaction(transaction);
        if (added) {
            logger.debug("Add {}", transaction);
        } else {
            logger.debug("{} is out of current window, not added", transaction);
        }
        return added ? CREATED : NO_CONTENT;
    }

    @RequestMapping(path = "/statistics", method = RequestMethod.GET, produces = "application/json")
    public Statistics getStatistics() {
        // this operation is amortised among subsequent API calls, it is optional for max accuracy
        streamStatService.removeOldTransactions();

        Statistics statistics = streamStatService.getStatistics();
        logger.debug("Get {}", statistics);
        return statistics;
    }
}
