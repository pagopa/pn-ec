package it.pagopa.pn.ec.sms.retry;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.StatusToDeleteException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.model.pojo.sqs.SqsMessageWrapper;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.rest.v1.dto.DigitalProgressStatusDto;
import it.pagopa.pn.ec.rest.v1.dto.PatchDto;
import it.pagopa.pn.ec.rest.v1.dto.RequestDto;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.List;

import static io.awspring.cloud.messaging.core.QueueMessageUtils.createMessage;
import static it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto.createNotificationTrackerQueueDtoDigital;
import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class SmsRetryTest {


    @Autowired
    SnsService snsServiceMock = mock(SnsService.class);

    @Autowired
    SnsService snsService;

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SnsService snsServiMessage;

    @Autowired
    SmsService smsService;

    SmsService smsServiceMock = mock(SmsService.class);


    GestoreRepositoryCall gestoreRepositoryCall = mock(GestoreRepositoryCall.class);



    Message message = Message.builder().build();

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO = SmsPresaInCaricoInfo.builder()
            .requestIdx(DEFAULT_REQUEST_IDX)
            .xPagopaExtchCxId(
                    DEFAULT_ID_CLIENT_HEADER_VALUE)
            .digitalCourtesySmsRequest(createSmsRequest())
            .build();

    private final SqsMessageWrapper<SmsPresaInCaricoInfo> sqsPresaInCaricoInfo = new SqsMessageWrapper<>(message, SMS_PRESA_IN_CARICO_INFO);

    @Test
    void testGestioneRetrySmsScheduler_NoMessages() {
        // mock SQSService per restituire un Mono vuoto quando viene chiamato getOneMessage
        SqsServiceImpl mockSqsService = mock(SqsServiceImpl.class);
        when(mockSqsService.getOneMessage(eq(smsSqsQueueName.errorName()), eq(SmsPresaInCaricoInfo.class)))
                .thenReturn(Mono.empty());

        // chiamare il metodo sotto test
        smsService.gestioneRetrySmsScheduler();

        // verificare che non sia stata eseguita alcuna operazione sul mock SQSService
        verify(mockSqsService, never()).deleteMessageFromQueue(eq(message), anyString());
    }

    @Test
    void gestioneRetrySms_RetryOk() {

        RequestDto requestDto = new RequestDto();
        requestDto.setStatusRequest("statusTest");
        requestDto.setRequestIdx("idTest");


        when(gestoreRepositoryCall.getRichiesta(eq(requestDto.getRequestIdx()))).thenReturn(Mono.just(requestDto));
        when(snsServiceMock.send(eq(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getReceiverDigitalAddress()),
                eq(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getMessageText())))
                .thenReturn(Mono.just(PublishResponse.builder().build()));

        gestoreRepositoryCall.getRichiesta(requestDto.getRequestIdx());
        smsService.gestioneRetrySms(SMS_PRESA_IN_CARICO_INFO, message);
        snsService.send(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(), SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getMessageText());

        verify(gestoreRepositoryCall, times(1)).getRichiesta(requestDto.getRequestIdx());
        verify(snsServiceMock, times(1)).send(SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getReceiverDigitalAddress(), SMS_PRESA_IN_CARICO_INFO.getDigitalCourtesySmsRequest().getMessageText());

    }


}