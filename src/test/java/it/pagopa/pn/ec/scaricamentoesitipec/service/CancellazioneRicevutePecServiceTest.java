package it.pagopa.pn.ec.scaricamentoesitipec.service;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.rest.call.RestCallException;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.rest.v1.dto.*;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.CancellazioneRicevutePecDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.pec.exception.pecservice.DeleteMessageException;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.constant.Status.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTestWebEnv
class CancellazioneRicevutePecServiceTest {

    @Autowired
    private CancellazioneRicevutePecService cancellazioneRicevutePecService;
    @MockBean
    Acknowledgment acknowledgment;
    @MockBean
    GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    PnEcPecService pnPecService;

    private final String CLIENT_ID = "CLIENT_ID";
    private final String REQUEST_ID = "REQUEST_ID";
    private final String MESSAGE_ID = "MESSAGE_ID";
    private final String SENDER_MESSAGE_ID = "SENDER_MESSAGE_ID";

    private static Stream<Arguments> statusesSource() {
        return Stream.of(Arguments.of(NOT_ACCEPTED.getStatusTransactionTableCompliant(), LegalMessageSentDetails.EventCodeEnum.C002),
                Arguments.of(NOT_PEC.getStatusTransactionTableCompliant(), LegalMessageSentDetails.EventCodeEnum.C009));
    }

    @Test
    void cancellazioneRicevuteOk() {
        CancellazioneRicevutePecDto cancellazioneRicevutePecDto = buildCancellazioneRicevutePecDto(LegalMessageSentDetails.EventCodeEnum.C003);

        DigitalProgressStatusDto digitalProgressStatusDto1 = new DigitalProgressStatusDto();
        digitalProgressStatusDto1.setStatusCode(LegalMessageSentDetails.EventCodeEnum.C001.getValue());
        digitalProgressStatusDto1.setStatus(ACCEPTED.getStatusTransactionTableCompliant());
        digitalProgressStatusDto1.setGeneratedMessage(new GeneratedMessageDto().id(SENDER_MESSAGE_ID));

        DigitalProgressStatusDto digitalProgressStatusDto2 = new DigitalProgressStatusDto();
        digitalProgressStatusDto2.setStatusCode(LegalMessageSentDetails.EventCodeEnum.C003.getValue());
        digitalProgressStatusDto2.setStatus(DELIVERED.getStatusTransactionTableCompliant());
        digitalProgressStatusDto2.setGeneratedMessage(new GeneratedMessageDto().id(MESSAGE_ID));

        EventsDto eventsDto1 = new EventsDto();
        eventsDto1.setDigProgrStatus(digitalProgressStatusDto1);

        EventsDto eventsDto2 = new EventsDto();
        eventsDto2.setDigProgrStatus(digitalProgressStatusDto2);

        RequestDto requestDto = buildRequestDto(eventsDto1, eventsDto2);

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_ID)).thenReturn(Mono.just(requestDto));
        when(pnPecService.deleteMessage(MESSAGE_ID, SENDER_MESSAGE_ID)).thenReturn(Mono.just("").then());

        var testMono = cancellazioneRicevutePecService.cancellazioneRicevutePec(cancellazioneRicevutePecDto, REQUEST_ID, acknowledgment);
        StepVerifier.create(testMono).verifyComplete();
        verify(pnPecService).deleteMessage(MESSAGE_ID, SENDER_MESSAGE_ID);
    }

    @ParameterizedTest
    @MethodSource("statusesSource")
    void cancellazioneRicevuteNotAcceptedAndNotPecOk(String status, LegalMessageSentDetails.EventCodeEnum eventCode)
    {
        CancellazioneRicevutePecDto cancellazioneRicevutePecDto = buildCancellazioneRicevutePecDto(eventCode);

        DigitalProgressStatusDto digitalProgressStatusDto2 = new DigitalProgressStatusDto();
        digitalProgressStatusDto2.setStatusCode(eventCode.getValue());
        digitalProgressStatusDto2.setStatus(status);
        digitalProgressStatusDto2.setGeneratedMessage(new GeneratedMessageDto().id(MESSAGE_ID));

        EventsDto eventsDto = new EventsDto();
        eventsDto.setDigProgrStatus(digitalProgressStatusDto2);

        RequestDto requestDto = buildRequestDto(eventsDto);

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_ID)).thenReturn(Mono.just(requestDto));
        when(pnPecService.deleteMessage(MESSAGE_ID, MESSAGE_ID)).thenReturn(Mono.just("").then());

        var testMono = cancellazioneRicevutePecService.cancellazioneRicevutePec(cancellazioneRicevutePecDto, REQUEST_ID, acknowledgment);
        StepVerifier.create(testMono).verifyComplete();
        verify(pnPecService).deleteMessage(MESSAGE_ID, MESSAGE_ID);
    }

    @Test
    void cancellazioneRicevuteGestoreRepositoryKo() {
        CancellazioneRicevutePecDto cancellazioneRicevutePecDto = buildCancellazioneRicevutePecDto(LegalMessageSentDetails.EventCodeEnum.C001);

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_ID)).thenReturn(Mono.error(new RestCallException.ResourceNotFoundException()));

        var testMono = cancellazioneRicevutePecService.cancellazioneRicevutePec(cancellazioneRicevutePecDto, REQUEST_ID, acknowledgment);
        StepVerifier.create(testMono).expectError(RestCallException.ResourceNotFoundException.class).verify();
    }

    @Test
    void cancellazioneRicevuteDeleteMessageKo() {
        CancellazioneRicevutePecDto cancellazioneRicevutePecDto = buildCancellazioneRicevutePecDto(LegalMessageSentDetails.EventCodeEnum.C003);

        DigitalProgressStatusDto digitalProgressStatusDto1 = new DigitalProgressStatusDto();
        digitalProgressStatusDto1.setStatusCode(LegalMessageSentDetails.EventCodeEnum.C001.getValue());
        digitalProgressStatusDto1.setStatus(ACCEPTED.getStatusTransactionTableCompliant());
        digitalProgressStatusDto1.setGeneratedMessage(new GeneratedMessageDto().id(MESSAGE_ID));

        DigitalProgressStatusDto digitalProgressStatusDto2 = new DigitalProgressStatusDto();
        digitalProgressStatusDto2.setStatusCode(LegalMessageSentDetails.EventCodeEnum.C003.getValue());
        digitalProgressStatusDto2.setStatus(DELIVERED.getStatusTransactionTableCompliant());
        digitalProgressStatusDto2.setGeneratedMessage(new GeneratedMessageDto().id(MESSAGE_ID));

        EventsDto eventsDto1 = new EventsDto();
        eventsDto1.setDigProgrStatus(digitalProgressStatusDto1);

        EventsDto eventsDto2 = new EventsDto();
        eventsDto2.setDigProgrStatus(digitalProgressStatusDto2);

        RequestDto requestDto = buildRequestDto(eventsDto1, eventsDto2);

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_ID)).thenReturn(Mono.just(requestDto));
        when(pnPecService.deleteMessage(MESSAGE_ID, MESSAGE_ID)).thenReturn(Mono.error(new DeleteMessageException(MESSAGE_ID)));

        var testMono = cancellazioneRicevutePecService.cancellazioneRicevutePec(cancellazioneRicevutePecDto, REQUEST_ID, acknowledgment);
        StepVerifier.create(testMono).expectError(DeleteMessageException.class).verify();
    }

    @Test
    void cancellazioneRicevuteNotValidForDelete() {
        CancellazioneRicevutePecDto cancellazioneRicevutePecDto = buildCancellazioneRicevutePecDto(LegalMessageSentDetails.EventCodeEnum.C003);

        DigitalProgressStatusDto digitalProgressStatusDto = new DigitalProgressStatusDto();
        digitalProgressStatusDto.setStatusCode(LegalMessageSentDetails.EventCodeEnum.C003.getValue());
        digitalProgressStatusDto.setStatus(DELIVERED.getStatusTransactionTableCompliant());
        digitalProgressStatusDto.setGeneratedMessage(new GeneratedMessageDto().id(MESSAGE_ID));
        EventsDto eventsDto = new EventsDto();
        eventsDto.setDigProgrStatus(digitalProgressStatusDto);

        RequestDto requestDto = buildRequestDto(eventsDto);

        when(gestoreRepositoryCall.getRichiesta(CLIENT_ID, REQUEST_ID)).thenReturn(Mono.just(requestDto));
        when(pnPecService.deleteMessage(MESSAGE_ID, MESSAGE_ID)).thenReturn(Mono.just("").then());

        var testMono = cancellazioneRicevutePecService.cancellazioneRicevutePec(cancellazioneRicevutePecDto, REQUEST_ID, acknowledgment);
        StepVerifier.create(testMono).verifyComplete();
    }

    private CancellazioneRicevutePecDto buildCancellazioneRicevutePecDto(LegalMessageSentDetails.EventCodeEnum eventCode) {
        LegalMessageSentDetails legalMessageSentDetails = new LegalMessageSentDetails();
        legalMessageSentDetails.setEventCode(eventCode);
        legalMessageSentDetails.setGeneratedMessage(new DigitalMessageReference().id("messageID"));
        legalMessageSentDetails.setRequestId(REQUEST_ID);

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(legalMessageSentDetails);
        singleStatusUpdate.setClientId(CLIENT_ID);

        CancellazioneRicevutePecDto cancellazioneRicevutePecDto = new CancellazioneRicevutePecDto();
        cancellazioneRicevutePecDto.setSingleStatusUpdate(singleStatusUpdate);

        return cancellazioneRicevutePecDto;
    }

    private RequestDto buildRequestDto(EventsDto... events) {
        RequestMetadataDto requestMetadataDto = new RequestMetadataDto();
        requestMetadataDto.setEventsList(List.of(events));

        RequestDto requestDto = new RequestDto();
        requestDto.setRequestIdx(REQUEST_ID);
        requestDto.setRequestMetadata(requestMetadataDto);

        return requestDto;
    }

}
