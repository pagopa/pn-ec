package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.ec.scaricamentoesitipec.configurationproperties.ScaricamentoEsitiPecProperties;
import it.pagopa.pn.ec.scaricamentoesitipec.model.pojo.RicezioneEsitiPecDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pec.bridgews.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static it.pagopa.pn.ec.pec.utils.MessageIdUtils.encodeMessageId;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.ACCETTAZIONE;
import static it.pagopa.pn.ec.scaricamentoesitipec.constant.PostacertTypes.POSTA_CERTIFICATA;
import static it.pagopa.pn.ec.scaricamentoesitipec.utils.PecUtils.generatePec;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTestWebEnv
class ScaricamentoEsitiPecSchedulerTest {

    @Autowired
    private DaticertService daticertService;
    @SpyBean
    private SqsService sqsService;
    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @Autowired
    private ScaricamentoEsitiPecScheduler scaricamentoEsitiPecScheduler;
    @Autowired
    private ArubaSecretValue arubaSecretValue;
    @Autowired
    private ScaricamentoEsitiPecProperties scaricamentoEsitiPecProperties;
    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;
    @MockBean
    private ArubaCall arubaCall;

    private static final String CLIENT_ID = "CLIENT_ID";
    private static final String PEC_REQUEST_IDX = "PEC_REQUEST_IDX";

    private final AtomicBoolean HAS_MESSAGES=new AtomicBoolean();

    @BeforeEach
    public void beforeEach() {
        when(arubaCall.getMessageId(any(GetMessageID.class))).thenReturn(Mono.just(new GetMessageIDResponse()));
        HAS_MESSAGES.set(true);
    }

    @BeforeAll
    public static void beforeAll() {
    }

    @Test
    void scaricamentoEsitiPecOk() throws MessagingException, IOException {

        //GIVEN
        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();
        getMessagesResponse.setArrayOfMessages(new MesArrayOfMessages());
        String msgId = "-" + encodeMessageId(CLIENT_ID, PEC_REQUEST_IDX) + "-";
        var outputStream = generatePec(ACCETTAZIONE, msgId, arubaSecretValue.getPecUsername(), "certificato", true);
        getMessagesResponse.getArrayOfMessages().getItem().add(outputStream.toByteArray());

        //WHEN
        when(arubaCall.getMessages(any(GetMessages.class))).thenReturn(Mono.just(getMessagesResponse));

        //THEN
        var testMono = scaricamentoEsitiPecScheduler.scaricamentoEsitiPec(HAS_MESSAGES);
        StepVerifier.create(testMono).expectNextCount(1).thenConsumeWhile(getMessageIDResponse -> true).verifyComplete();

        verify(sqsService, times(1)).send(eq(scaricamentoEsitiPecProperties.sqsQueueName()), anyString(), any(RicezioneEsitiPecDto.class));
        verify(arubaCall, times(1)).getMessageId(any(GetMessageID.class));
    }

    @Test
    void scaricamentoEsitiPecNoMessages() {

        when(arubaCall.getMessages(any(GetMessages.class))).thenReturn(Mono.just(new GetMessagesResponse()));

        var testMono = scaricamentoEsitiPecScheduler.scaricamentoEsitiPec(HAS_MESSAGES);
        StepVerifier.create(testMono).verifyComplete();

        verify(sqsService, times(0)).send(eq(scaricamentoEsitiPecProperties.sqsQueueName()), anyString(), any(RicezioneEsitiPecDto.class));
    }

    @Test
    void scaricamentoEsitiPecNoDaticert() throws MessagingException, IOException {

        //GIVEN
        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();
        getMessagesResponse.setArrayOfMessages(new MesArrayOfMessages());
        String msgId = "-" + encodeMessageId(CLIENT_ID, PEC_REQUEST_IDX) + "-";
        var outputStream = generatePec(ACCETTAZIONE, msgId, arubaSecretValue.getPecUsername(), "certificato", false);
        getMessagesResponse.getArrayOfMessages().getItem().add(outputStream.toByteArray());

        when(arubaCall.getMessages(any(GetMessages.class))).thenReturn(Mono.just(getMessagesResponse));

        var testMono = scaricamentoEsitiPecScheduler.scaricamentoEsitiPec(HAS_MESSAGES);
        StepVerifier.create(testMono).expectNextCount(1).thenConsumeWhile(getMessageIDResponse -> true).verifyComplete();


        verify(sqsService, times(0)).send(eq(scaricamentoEsitiPecProperties.sqsQueueName()), anyString(), any(RicezioneEsitiPecDto.class));
        verify(arubaCall, times(1)).getMessageId(any(GetMessageID.class));
    }



    @ParameterizedTest
    @ValueSource(strings = {ACCETTAZIONE, POSTA_CERTIFICATA})
    void scaricamentoEsitiPecDiscarded(String tipo) throws MessagingException, IOException {

        //GIVEN
        GetMessagesResponse getMessagesResponse = new GetMessagesResponse();
        getMessagesResponse.setArrayOfMessages(new MesArrayOfMessages());
        var outputStream = generatePec(tipo, "-msgId@domain.it-", arubaSecretValue.getPecUsername(), "certificato", true);
        getMessagesResponse.getArrayOfMessages().getItem().add(outputStream.toByteArray());

        when(arubaCall.getMessages(any(GetMessages.class))).thenReturn(Mono.just(getMessagesResponse));

        var testMono = scaricamentoEsitiPecScheduler.scaricamentoEsitiPec(HAS_MESSAGES);
        StepVerifier.create(testMono).expectNextCount(1).thenConsumeWhile(getMessageIDResponse -> true).verifyComplete();


        verify(sqsService, times(0)).send(eq(scaricamentoEsitiPecProperties.sqsQueueName()), anyString(), any(RicezioneEsitiPecDto.class));
        verify(arubaCall, times(1)).getMessageId(any(GetMessageID.class));
    }

}
