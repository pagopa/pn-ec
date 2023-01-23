package it.pagopa.pn.ec.commons.service.impl;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import it.pagopa.pn.ec.commons.exception.SqsException;
import it.pagopa.pn.ec.commons.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    private final QueueMessagingTemplate queueMessagingTemplate;

    public SqsServiceImpl(QueueMessagingTemplate queueMessagingTemplate) {
        this.queueMessagingTemplate = queueMessagingTemplate;
    }

    @Override
    public <T> void send(String queueName, T queuePayload) {
        log.info("Send to {} queue with payload ↓\n{}", queueName, queuePayload);
        try {
            queueMessagingTemplate.convertAndSend(queueName, queuePayload);
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new SqsException.SqsPublishException(queueName);
        }
    }
}
