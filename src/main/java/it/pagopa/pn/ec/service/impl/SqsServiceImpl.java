package it.pagopa.pn.ec.service.impl;

import it.pagopa.pn.ec.service.SqsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SqsServiceImpl implements SqsService {

    @Override
    public <T> void send(String queueName, T queuePayload) {

    }
}
