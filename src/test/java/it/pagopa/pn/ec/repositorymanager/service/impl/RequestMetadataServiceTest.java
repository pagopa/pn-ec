package it.pagopa.pn.ec.repositorymanager.service.impl;

import it.pagopa.pn.ec.commons.exception.RepositoryManagerException;
import it.pagopa.pn.ec.repositorymanager.model.entity.DigitalProgressStatus;
import it.pagopa.pn.ec.repositorymanager.model.entity.Events;
import it.pagopa.pn.ec.repositorymanager.model.entity.GeneratedMessage;
import it.pagopa.pn.ec.repositorymanager.model.entity.PaperProgressStatus;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTestWebEnv
class RequestMetadataServiceTest {

    @Autowired
    private RequestMetadataServiceImpl requestMetadataService;

    @Test
    void eventsCheckDigitalKo() {
        var now = OffsetDateTime.now();

        //GeneratedMessage uguali.
        //Per l'implementazione custom dell'equals, i due oggetti risulteranno uguali.
        var generatedMessage1 = GeneratedMessage.builder().system("system").id("id").location("location1").build();
        var generatedMessage2 = GeneratedMessage.builder().system("system").id("id").location("location2").build();

        //Costruisco due eventi che hanno stesso status e generatedMessage, ma il resto dei campi diversi
        //Per l'implementazione custom dell'equals, i due oggetti risulteranno uguali.
        var status1 = DigitalProgressStatus.builder().status("STATUS").eventTimestamp(now).eventDetails("eventDetails1").eventCode("eventCode1").generatedMessage(generatedMessage1).build();
        var event1 = Events.builder().digProgrStatus(status1).build();
        var status2 = DigitalProgressStatus.builder().status("STATUS").eventTimestamp(now).eventDetails("eventDetails2").eventCode("eventCode2").generatedMessage(generatedMessage2).build();
        var event2 = Events.builder().digProgrStatus(status2).build();

        var eventsList = List.of(event2);

        Assertions.assertEquals(event1, event2);
        Assertions.assertThrows(RepositoryManagerException.EventAlreadyExistsException.class, () -> requestMetadataService.eventsCheck(event1, eventsList, "requestId"));

    }

    @Test
    void eventsCheckDigitalOk() {
        var now = OffsetDateTime.now();

        //GeneratedMessage uguali.
        GeneratedMessage generatedMessage1 = GeneratedMessage.builder().system("system").id("id").location("location1").build();
        GeneratedMessage generatedMessage2 = GeneratedMessage.builder().system("system").id("id").location("location2").build();

        //Costruisco due eventi che hanno uno status differente.
        //Per l'implementazione custom dell'equals, i due oggetti risulteranno diversi.
        DigitalProgressStatus status1 = DigitalProgressStatus.builder().status("STATUS1").eventTimestamp(now).eventDetails("eventDetails1").eventCode("eventCode1").generatedMessage(generatedMessage1).build();
        Events event1 = Events.builder().digProgrStatus(status1).build();

        DigitalProgressStatus status2 = DigitalProgressStatus.builder().status("STATUS2").eventTimestamp(now).eventDetails("eventDetails2").eventCode("eventCode2").generatedMessage(generatedMessage2).build();
        Events event2 = Events.builder().digProgrStatus(status2).build();

        var eventsList = List.of(event2);

        Assertions.assertNotEquals(event1, event2);
        Assertions.assertDoesNotThrow(() -> requestMetadataService.eventsCheck(event1, eventsList, "requestId"));

    }

    @Test
    void eventsCheckPaperOk() {
        var now = OffsetDateTime.now();

        //Eventi uguali
        var status1 = PaperProgressStatus.builder().status("STATUS1").statusDateTime(now).iun("iun1").build();
        var event1 = Events.builder().paperProgrStatus(status1).build();
        var status2 = PaperProgressStatus.builder().status("STATUS2").statusDateTime(now).iun("iun2").build();
        var event2 = Events.builder().paperProgrStatus(status2).build();

        var eventsList = List.of(event2);

        Assertions.assertNotEquals(event1, event2);
        Assertions.assertDoesNotThrow(() -> requestMetadataService.eventsCheck(event1, eventsList, "requestId"));
    }

}
