package it.pagopa.pnec.notificationTracker.controller;

import it.pagopa.pnec.notificationTracker.model.ResponseModel;
import it.pagopa.pnec.notificationTracker.service.NotificationtrackerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.*;

class NotificationtrackerControllerTest {
    @Mock
    NotificationtrackerService service;
    @Mock
    Logger log;
    @InjectMocks
    NotificationtrackerController notificationtrackerController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetStato() {
        Mono<ResponseEntity<Void>> result = notificationtrackerController.getStato("processId", "currStatus", "clientId", "nextStatus");
        ResponseModel resp = new ResponseModel();
        resp.setAllowed(true);
        Assertions.assertEquals(resp, result);
    }

    @Test
    void testPutNewStato() {
        Mono<ResponseEntity<Void>> result = notificationtrackerController.putNewStato("processId", "currStatus", "clientId", "nextStatus");
        ResponseModel resp = new ResponseModel();
        Assertions.assertEquals(resp, result);
    }
}
