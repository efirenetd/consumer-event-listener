package org.efire.net.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.efire.net.entity.LibraryEvent;
import org.efire.net.exceptions.LibraryEventException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LibraryEventRecoveryCallback implements RecoveryCallback {

    private FailedRecordHandler failedRecordHandler;

    public LibraryEventRecoveryCallback(FailedRecordHandler failedRecordHandler) {
        this.failedRecordHandler = failedRecordHandler;
    }

    @Override
    public Object recover(RetryContext retryContext) throws Exception {
        Throwable cause = retryContext.getLastThrowable().getCause();
        if (cause instanceof RecoverableDataAccessException) {
            log.info("Put Recoverable logic here...");
            ConsumerRecord<Integer, String> failedConsumerRecord = (ConsumerRecord<Integer, String>) retryContext.getAttribute("record");
            failedRecordHandler.handleRecord(failedConsumerRecord);
        } else if (cause instanceof LibraryEventException) {
            LibraryEvent failedMessage = ((LibraryEventException) cause).getLibraryEvent();
            log.info("Failed message {} ", failedMessage.toString());
        } else {
            log.info("Put Non-recoverable logic here...");
            throw new RuntimeException(retryContext.getLastThrowable().getMessage());
        }
        return null;
    }
}
