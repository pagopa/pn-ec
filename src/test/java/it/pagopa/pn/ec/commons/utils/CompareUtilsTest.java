package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.SENT;

@SpringBootTestWebEnv
class CompareUtilsTest {

    @Test
    void isSameEventDigitalOk() {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto()
                .status(SENT.getStatusTransactionTableCompliant())
                .generatedMessage(new GeneratedMessageDto().id("id").system("system"))
                .eventTimestamp(now);
        EventsDto eventsDto = new EventsDto().digProgrStatus(digitalProgressStatusDto);

        boolean isSameEvent = CompareUtils.isSameEvent(List.of(eventsDto), digitalProgressStatusDto, SENT.getStatusTransactionTableCompliant());
        Assertions.assertTrue(isSameEvent);
    }

    @Test
    void isSameEventPaperOk() {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto()
                .status(SENT.getStatusTransactionTableCompliant())
                .statusDateTime(now);

        boolean isSameEvent = CompareUtils.isSameEvent(paperProgressStatusDto, paperProgressStatusDto, SENT.getStatusTransactionTableCompliant());
        Assertions.assertTrue(isSameEvent);
    }

    @Test
    void isSameEventConsolidatoreOk() {
        OffsetDateTime now = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        String id = "id";
        String uri = "uri";
        String documentType = "documentType";
        String sha256 = "sha256";

        AttachmentsProgressEventDto attachments = new AttachmentsProgressEventDto()
                .id(id)
                .date(now)
                .uri(uri)
                .documentType(documentType)
                .sha256(sha256);

        ConsolidatoreIngressPaperProgressStatusEventAttachments consAttachments = new ConsolidatoreIngressPaperProgressStatusEventAttachments()
                .id(id)
                .date(now)
                .uri(uri)
                .documentType(documentType)
                .sha256(sha256);

        String name = "name";
        String nameRow2 = "nameRow2";
        String city = "city";
        String city2 = "city2";
        String country = "country";
        String address = "address";
        String addressRow2 = "addressRow2";
        String cap = "cap";
        String pr = "pr";

        DiscoveredAddressDto discoveredAddress = new DiscoveredAddressDto()
                .name(name)
                .nameRow2(nameRow2)
                .city(city)
                .city2(city2)
                .country(country)
                .address(address)
                .addressRow2(addressRow2)
                .cap(cap)
                .pr(pr);

        ConsolidatoreIngressPaperProgressStatusEventDiscoveredAddress consDiscoveredAddress = new ConsolidatoreIngressPaperProgressStatusEventDiscoveredAddress()
                .name(name)
                .nameRow2(nameRow2)
                .city(city)
                .city2(city2)
                .country(country)
                .address(address)
                .addressRow2(addressRow2)
                .cap(cap)
                .pr(pr);

        String statusCode = "P000";
        String statusDescription = "desc";
        String registeredLetterCode = "code";
        String productType = "type";
        String iun = "iun";
        String deliveryFailureCause = "cause";
        String courier = "recapitista";

        PaperProgressStatusDto paperProgressStatusDto = new PaperProgressStatusDto()
                .status(SENT.getStatusTransactionTableCompliant())
                .statusDateTime(now)
                .registeredLetterCode(registeredLetterCode)
                .productType(productType)
                .iun(iun)
                .deliveryFailureCause(deliveryFailureCause)
                .statusCode(statusCode)
                .statusDescription(statusDescription)
                .attachments(List.of(attachments))
                .discoveredAddress(discoveredAddress)
                .courier(courier);

        ConsolidatoreIngressPaperProgressStatusEvent consEvent = new ConsolidatoreIngressPaperProgressStatusEvent()
                .statusDateTime(now)
                .registeredLetterCode(registeredLetterCode)
                .productType(productType)
                .iun(iun)
                .deliveryFailureCause(deliveryFailureCause)
                .statusCode(statusCode)
                .statusDescription(statusDescription)
                .attachments(List.of(consAttachments))
                .discoveredAddress(consDiscoveredAddress)
                .courier(courier);

        boolean isSameEvent = CompareUtils.isSameEvent(paperProgressStatusDto, consEvent);
        Assertions.assertTrue(isSameEvent);
    }

}
