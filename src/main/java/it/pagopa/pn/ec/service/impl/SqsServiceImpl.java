package it.pagopa.pn.ec.service.impl;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import it.pagopa.pn.ec.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    private final QueueMessagingTemplate messagingTemplate;

    public SqsServiceImpl(QueueMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void send(String queueName, String messagePayload) {
        Message<String> msg = MessageBuilder.withPayload(messagePayload)
                                            .build();

        messagingTemplate.convertAndSend(queueName, msg);
    }
}
