package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.model.dto.MacchinaStatiDecodeResponseDto;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.rest.call.machinestate.CallMacchinaStati;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static it.pagopa.pn.ec.commons.constant.Status.BOOKED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
public class StatusPullServiceTest {

    @Autowired
    private StatusPullService statusPullService;
    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @MockBean
    private AuthService authService;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private CallMacchinaStati callMacchinaStati;
    private static final String SMS_REQUEST_IDX = "SMS_REQUEST_IDX";
    private static final String EMAIL_REQUEST_IDX = "EMAIL_REQUEST_IDX";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";
    private static final String PAPER_REQUEST_IDX = "PAPER_REQUEST_IDX";
    private static final String CLIENT_ID = "CLIENT_ID";

    @BeforeEach
    public void initialize() {
        when(authService.clientAuth(anyString())).thenReturn(Mono.just(new ClientConfigurationInternalDto()));
    }

    private RequestDto smsRequest() {
        var requestPersonal = new RequestPersonalDto().digitalRequestPersonal(new DigitalRequestPersonalDto());
        var requestMetadata = new RequestMetadataDto().digitalRequestMetadata(new DigitalRequestMetadataDto().channel(DigitalRequestMetadataDto.ChannelEnum.SMS));
        return new RequestDto().requestIdx(SMS_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).requestPersonal(requestPersonal).requestMetadata(requestMetadata);
    }

    private RequestDto pecRequest() {
        var requestPersonal = new RequestPersonalDto().digitalRequestPersonal(new DigitalRequestPersonalDto());
        var requestMetadata = new RequestMetadataDto().digitalRequestMetadata(new DigitalRequestMetadataDto().channel(DigitalRequestMetadataDto.ChannelEnum.PEC));
        return new RequestDto().requestIdx(PEC_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).requestPersonal(requestPersonal).requestMetadata(requestMetadata);
    }

    private RequestDto paperRequest() {
        var requestPersonal = new RequestPersonalDto().paperRequestPersonal(new PaperRequestPersonalDto());
        var requestMetadata = new RequestMetadataDto().paperRequestMetadata(new PaperRequestMetadataDto());
        return new RequestDto().requestIdx(PEC_REQUEST_IDX).xPagopaExtchCxId(CLIENT_ID).requestPersonal(requestPersonal).requestMetadata(requestMetadata);
    }

    private EventsDto digitalBookedEvent() {
        return new EventsDto().digProgrStatus(new DigitalProgressStatusDto()
                .status(BOOKED.getStatusTransactionTableCompliant())
                .eventTimestamp(OffsetDateTime.now())
                .generatedMessage(new GeneratedMessageDto().id("id").location("location").system("system")));
    }

    private EventsDto paperBookedEvent() {
        return new EventsDto().paperProgrStatus(new PaperProgressStatusDto()
                .status(BOOKED.getStatusTransactionTableCompliant())
                .statusDateTime(OffsetDateTime.now())
                .discoveredAddress(new DiscoveredAddressDto())
                .attachments(List.of(new AttachmentsProgressEventDto())));
    }

    @Test
    void digitalPullServiceOk() {

        RequestDto request = smsRequest();
        request.getRequestMetadata().setEventsList(List.of(digitalBookedEvent()));

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(SMS_REQUEST_IDX))).thenReturn(Mono.just(request));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiDecodeResponseDto(CourtesyMessageProgressEvent.EventCodeEnum.M003.getValue(), ProgressEventCategory.OK.getValue())));


        Mono<CourtesyMessageProgressEvent> testMono = statusPullService.digitalPullService(SMS_REQUEST_IDX, CLIENT_ID, transactionProcessConfigurationProperties.sms());
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void digitalPullServiceEmptyEventsList() {

        RequestDto request = smsRequest();

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(SMS_REQUEST_IDX))).thenReturn(Mono.just(request));

        Mono<CourtesyMessageProgressEvent> testMono = statusPullService.digitalPullService(SMS_REQUEST_IDX, CLIENT_ID, transactionProcessConfigurationProperties.sms());
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void pecPullServiceOk() {

        RequestDto request = pecRequest();
        request.getRequestMetadata().setEventsList(List.of(digitalBookedEvent()));

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(PEC_REQUEST_IDX))).thenReturn(Mono.just(request));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiDecodeResponseDto(LegalMessageSentDetails.EventCodeEnum.C000.getValue(), ProgressEventCategory.OK.getValue())));


        Mono<LegalMessageSentDetails> testMono = statusPullService.pecPullService(PEC_REQUEST_IDX, CLIENT_ID);
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void pecPullServiceEmptyEventsList() {

        RequestDto request = pecRequest();

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(PEC_REQUEST_IDX))).thenReturn(Mono.just(request));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiDecodeResponseDto(LegalMessageSentDetails.EventCodeEnum.C000.getValue(), ProgressEventCategory.OK.getValue())));


        Mono<LegalMessageSentDetails> testMono = statusPullService.pecPullService(PEC_REQUEST_IDX, CLIENT_ID);
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void paperPullServiceOk() {

        RequestDto request = paperRequest();
        request.getRequestMetadata().setEventsList(List.of(paperBookedEvent()));

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(PAPER_REQUEST_IDX))).thenReturn(Mono.just(request));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiDecodeResponseDto("logicStatus", "externalStatus")));


        Mono<PaperProgressStatusEvent> testMono = statusPullService.paperPullService(PAPER_REQUEST_IDX, CLIENT_ID);
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

    @Test
    void paperPullServiceEmptyEventsList() {

        RequestDto request = paperRequest();

        when(gestoreRepositoryCall.getRichiesta(eq(CLIENT_ID), eq(PAPER_REQUEST_IDX))).thenReturn(Mono.just(request));
        when(callMacchinaStati.statusDecode(anyString(), anyString(), anyString())).thenReturn(Mono.just(new MacchinaStatiDecodeResponseDto("logicStatus", "externalStatus")));

        Mono<PaperProgressStatusEvent> testMono = statusPullService.paperPullService(PAPER_REQUEST_IDX, CLIENT_ID);
        StepVerifier.create(testMono).expectNextCount(1).verifyComplete();
    }

}
