package it.pagopa.pn.ec.pec.service.impl;


import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.impl.ArubaServiceImpl;
import it.pec.bridgews.*;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;
import lombok.CustomLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SpringBootTestWebEnv
@CustomLog
class ArubaServiceImplTest {

    @MockBean
    @Qualifier("pecImapBridgeClient")
    private PecImapBridge pecImapBridge;
    @SpyBean
    private ArubaServiceImpl arubaServiceImpl;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void deleteMessageTest_Ok() {

        when(pecImapBridge.deleteMailAsync(any(DeleteMail.class), any())).thenAnswer(invocation -> {
            AsyncHandler<DeleteMailResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<DeleteMailResponse> response = mock(Response.class);
            when(response.get()).thenReturn(new DeleteMailResponse());
            handler.handleResponse(response);
            return CompletableFuture.completedFuture(null);
        });

        String messageID = "test@pec.aruba.it";

        StepVerifier.create(arubaServiceImpl.deleteMessage(messageID)).verifyComplete();

        verify(pecImapBridge, times(1)).deleteMailAsync(any(), any());
    }

    @Test
    void deleteMessageTest_Ko() {
        String messageID = "test@pec.aruba.it";

        when(pecImapBridge.deleteMailAsync(any(DeleteMail.class), any())).thenAnswer(invocation -> {
            AsyncHandler<DeleteMailResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<DeleteMailResponse> response = mock(Response.class);

            when(response.get()).thenReturn(new DeleteMailResponse() {{
                setErrcode(1);
                setErrstr("error");
            }});
            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        StepVerifier.create(arubaServiceImpl.deleteMessage(messageID)).verifyError();
    }

    @Test
    void getMessageCountTest_Ok() {

        when(pecImapBridge.getMessageCountAsync(any(GetMessageCount.class), any())).thenAnswer(invocation -> {
            AsyncHandler<GetMessageCountResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<GetMessageCountResponse> response = mock(Response.class);

            GetMessageCountResponse gmc = new GetMessageCountResponse();
            gmc.setCount(5);

            when(response.get()).thenReturn(gmc);
            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        StepVerifier.create(arubaServiceImpl.getMessageCount()).expectNext(5).verifyComplete();

        verify(pecImapBridge, times(1)).getMessageCountAsync(any(), any());
    }

    @Test
    void getMessageCountTest_MatchKo() {

        when(pecImapBridge.getMessageCountAsync(any(GetMessageCount.class), any())).thenAnswer(invocation -> {
            AsyncHandler<GetMessageCountResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<GetMessageCountResponse> response = mock(Response.class);

            when(response.get()).thenReturn(new GetMessageCountResponse() {{
                setErrcode(1);
                setErrstr("error");
            }});
            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        StepVerifier.create(arubaServiceImpl.getMessageCount()).verifyError();
    }

    @Test
    void sendMailTest_Ok() {

        when(pecImapBridge.sendMailAsync(any(SendMail.class), any())).thenAnswer(invocation -> {
            AsyncHandler<SendMailResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<SendMailResponse> response = mock(Response.class);

            SendMailResponse smr = new SendMailResponse();
            smr.setErrstr("seterrorstr");

            when(response.get()).thenReturn(smr);
            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        Mono<String> monoResponse = arubaServiceImpl.sendMail("message".getBytes());

        StepVerifier.create(monoResponse).expectNext("seterrors").verifyComplete();

        verify(pecImapBridge, times(1)).sendMailAsync(any(), any());
    }

    @Test
    void sendMailTest_Ko() {

        when(pecImapBridge.sendMailAsync(any(SendMail.class), any())).thenAnswer(invocation -> {
            AsyncHandler<SendMailResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<SendMailResponse> response = mock(Response.class);

            when(response.get()).thenReturn(new SendMailResponse() {{
                setErrcode(1);
                setErrstr("error");
            }});
            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        Mono<String> monoResponse = arubaServiceImpl.sendMail("message".getBytes());

        StepVerifier.create(monoResponse).verifyError();
    }

    @Test
    void getUnreadMessagesTest_Ok() {

        GetMessagesResponse gmr = new GetMessagesResponse();
        MesArrayOfMessages messagesArray = new MesArrayOfMessages();
        messagesArray.getItem().add("message1".getBytes());
        messagesArray.getItem().add("message2".getBytes());
        messagesArray.getItem().add("message3".getBytes());
        gmr.setArrayOfMessages(messagesArray);

        when(pecImapBridge.getMessagesAsync(any(GetMessages.class), any())).thenAnswer(invocation -> {
            AsyncHandler<GetMessagesResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<GetMessagesResponse> response = mock(Response.class);

            when(response.get()).thenReturn(gmr);
            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        Mono<PnGetMessagesResponse> monoResponse = arubaServiceImpl.getUnreadMessages(3);

        StepVerifier.create(monoResponse).expectNextMatches(response -> response.getPnListOfMessages().getMessages().containsAll(messagesArray.getItem())).verifyComplete();

        verify(pecImapBridge, times(1)).getMessagesAsync(any(), any());
    }

    @Test
    void getUnreadMessagesTest_Ko() {

        when(pecImapBridge.getMessagesAsync(any(GetMessages.class), any())).thenAnswer(invocation -> {
            AsyncHandler<GetMessagesResponse> handler = invocation.getArgument(1, AsyncHandler.class);
            Response<GetMessagesResponse> response = mock(Response.class);

            when(response.get()).thenReturn(new GetMessagesResponse() {{
                setErrcode(1);
                setErrstr("error");
            }});

            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        Mono<PnGetMessagesResponse> monoResponse = arubaServiceImpl.getUnreadMessages(3);

        StepVerifier.create(monoResponse).verifyError();
    }

    @Test
    void markMessageAsReadTest_Ok() {

        when(pecImapBridge.getMessageIDAsync(any(GetMessageID.class), any())).thenAnswer(invocation -> {
            AsyncHandler<GetMessageIDResponse> handler = invocation.getArgument(1, AsyncHandler.class);

            Response<GetMessageIDResponse> response = mock(Response.class);
            when(response.get()).thenReturn(new GetMessageIDResponse());

            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        String messageID = "test@pec.aruba.it";

        StepVerifier.create(arubaServiceImpl.markMessageAsRead(messageID)).verifyComplete();

        verify(pecImapBridge, times(1)).getMessageIDAsync(any(), any());
    }

    @Test
    void markMessageAsReadTest_Ko() {

        when(pecImapBridge.getMessageIDAsync(any(GetMessageID.class), any())).thenAnswer(invocation -> {
            AsyncHandler<GetMessageIDResponse> handler = invocation.getArgument(1, AsyncHandler.class);

            Response<GetMessageIDResponse> response = mock(Response.class);
            when(response.get()).thenReturn(new GetMessageIDResponse() {{
                setErrcode(1);
                setErrstr("error");
            }});

            handler.handleResponse(response);

            return CompletableFuture.completedFuture(null);
        });

        String messageID = "test@pec.aruba.it";

        StepVerifier.create(arubaServiceImpl.markMessageAsRead(messageID)).verifyError();
    }
}