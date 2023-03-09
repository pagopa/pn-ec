package it.pagopa.pn.ec.sms.retry;

import io.awspring.cloud.messaging.listener.Acknowledgment;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import it.pagopa.pn.ec.commons.exception.sqs.SqsPublishException;
import it.pagopa.pn.ec.commons.model.dto.NotificationTrackerQueueDto;
import it.pagopa.pn.ec.commons.service.SnsService;
import it.pagopa.pn.ec.commons.service.impl.SqsServiceImpl;
import it.pagopa.pn.ec.sms.configurationproperties.SmsSqsQueueName;
import it.pagopa.pn.ec.sms.model.pojo.SmsPresaInCaricoInfo;
import it.pagopa.pn.ec.sms.service.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static it.pagopa.pn.ec.sms.testutils.DigitalCourtesySmsRequestFactory.createSmsRequest;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_ID_CLIENT_HEADER_VALUE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@SpringBootTestWebEnv
class RetrySmsTest {

    @Autowired
    private SmsService smsService;

    @Autowired
    private SmsSqsQueueName smsSqsQueueName;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @SpyBean
    private SqsServiceImpl sqsService;

    @SpyBean
    private SnsService snsService;

    @Mock
    private Acknowledgment acknowledgment;

    private static final SmsPresaInCaricoInfo SMS_PRESA_IN_CARICO_INFO =
            new SmsPresaInCaricoInfo(DEFAULT_REQUEST_IDX, DEFAULT_ID_CLIENT_HEADER_VALUE, createSmsRequest());

    @Test
    void ErrorQueueRetrieveMessageOk() {

        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.just(PublishResponse.builder().build()));

        // Invio di un messaggio alla coda di errore
        when(sqsService.send(eq(smsSqsQueueName.errorName()), any(SmsPresaInCaricoInfo.class))).thenReturn(Mono.error(
                new SqsPublishException(smsSqsQueueName.errorName())));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO, acknowledgment);

        // Verifica della ricezione del messaggio nella coda di errore
        verify(sqsService, times(1)).send(eq(smsSqsQueueName.errorName()), any(SmsPresaInCaricoInfo.class));
        verifyNoMoreInteractions(snsService);
        verifyNoMoreInteractions(sqsService);

    }

    @Test
    void snsShortRetryOk() {

        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.error(new SnsSendException()))
                .thenReturn(Mono.just(PublishResponse.builder().build()));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO, acknowledgment);

        verify(snsService, times(2)).send(anyString(), anyString());
        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
    }

    @Test
    void snsShortRetryKo() {

        // TODO: Eliminare il mock una volta sistemato l'ambiente Localstack
        when(snsService.send(anyString(), anyString())).thenReturn(Mono.just(PublishResponse.builder().build()));

        when(sqsService.send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class))).thenReturn(Mono.error(
                new SqsPublishException(notificationTrackerSqsName.statoSmsName())));

        smsService.lavorazioneRichiesta(SMS_PRESA_IN_CARICO_INFO, acknowledgment);

        verify(sqsService, times(1)).send(eq(notificationTrackerSqsName.statoSmsName()), any(NotificationTrackerQueueDto.class));
        verify(sqsService, times(1)).send(eq(smsSqsQueueName.errorName()), any(SmsPresaInCaricoInfo.class));
    }

}

