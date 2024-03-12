package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.commons.utils.EmailUtils;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.library.pec.exception.pecservice.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.model.pojo.ArubaSecretValue;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.pojo.PnListOfMessages;
import it.pagopa.pn.library.pec.service.ArubaService;
import it.pagopa.pn.library.pec.exception.aruba.ArubaCallException;
import it.pec.bridgews.*;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;


@Component
@CustomLog
public class ArubaServiceImpl implements ArubaService {


    private final PecImapBridge pecImapBridgeClient;

    private final ArubaSecretValue arubaSecretValue;


    private final CloudWatchPecMetrics cloudWatchPecMetrics;

    private final String arubaProviderNamespace;

    private static final int MESSAGE_NOT_FOUND_ERR_CODE = 99;

    public static final String ARUBA_PATTERN_STRING = "@pec.aruba.it";

    @Autowired
    public ArubaServiceImpl(PecImapBridge pecImapBridgeClient,
                            ArubaSecretValue arubaSecretValue,
                            CloudWatchPecMetrics cloudWatchPecMetrics,
                            @Value("${library.pec.cloudwatch.namespace.aruba}") String arubaProviderNamespace) {
        this.pecImapBridgeClient = pecImapBridgeClient;
        this.arubaSecretValue = arubaSecretValue;
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.arubaProviderNamespace = arubaProviderNamespace;
    }

    @Override
    public Mono<Integer> getMessageCount() {
        GetMessageCount getMessageCount = new GetMessageCount();
        getMessageCount.setUser(arubaSecretValue.getPecUsername());
        getMessageCount.setPass(arubaSecretValue.getPecPassword());
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGE_COUNT, getMessageCount);
        return Mono.create(sink -> pecImapBridgeClient.getMessageCountAsync(getMessageCount, res -> {
                    try {
                        var result = res.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception e) {
                        MDCUtils.enrichWithMDC(null, mdcContextMap);
                        endSoapRequest(sink, e);
                    }
                })).cast(GetMessageCountResponse.class)
                .map(GetMessageCountResponse::getCount)
                .flatMap(count -> cloudWatchPecMetrics.publishMessageCount(Long.valueOf(count), arubaProviderNamespace).thenReturn(count))
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGE_COUNT, result))
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));
    }

    public Mono<Void> deleteMessage(String messageID) {
        DeleteMail deleteMail = new DeleteMail();
        deleteMail.setUser(arubaSecretValue.getPecUsername());
        deleteMail.setPass(arubaSecretValue.getPecPassword());
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        deleteMail.setMailid(messageID);
        deleteMail.setIsuid(2);
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_DELETE_MAIL, deleteMail);
        return Mono.create(sink -> pecImapBridgeClient.deleteMailAsync(deleteMail, res -> {
                    try {
                        var result = res.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (ArubaCallException arubaCallException) {
                        MDCUtils.enrichWithMDC(null, mdcContextMap);
                        if (arubaCallException.getErrorCode() == MESSAGE_NOT_FOUND_ERR_CODE) {
                            log.debug(ARUBA_MESSAGE_MISSING, deleteMail.getMailid());
                            sink.success();
                        } else endSoapRequest(sink, arubaCallException);
                    } catch (Exception e) {
                        MDCUtils.enrichWithMDC(null, mdcContextMap);
                        endSoapRequest(sink, e);
                    }
                })).cast(DeleteMailResponse.class)
                .then()
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_DELETE_MAIL, result))
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));

    }

    @Override
    public Mono<String> sendMail(byte[] message) {
        SendMail sendMail = new SendMail();
        sendMail.setData(EmailUtils.getMimeMessageInCDATATag(message));
        sendMail.setUser(arubaSecretValue.getPecUsername());
        sendMail.setPass(arubaSecretValue.getPecPassword());
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_SEND_MAIL, sendMail);
        return Mono.create(sink -> pecImapBridgeClient.sendMailAsync(sendMail, outputFuture -> {
            try {
                var result = outputFuture.get();
                checkErrors(result.getErrcode(), result.getErrstr());
                sink.success(result);
            } catch (Exception throwable) {
                MDCUtils.enrichWithMDC(null, mdcContextMap);
                endSoapRequest(sink, throwable);
            }
        }))
                .cast(SendMailResponse.class)
                .map(sendMailResponse -> {
                    String msgId = sendMailResponse.getErrstr();
                    //Remove the last 2 char '\r\n'
                    return msgId.substring(0, msgId.length() - 2);
                })
                .cast(String.class)
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_SEND_MAIL, result))
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));

    }

    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        GetMessages getMessages = new GetMessages();
        getMessages.setUnseen(1);
        getMessages.setLimit(limit);
        getMessages.setOuttype(2);
        getMessages.setUser(arubaSecretValue.getPecUsername());
        getMessages.setPass(arubaSecretValue.getPecPassword());
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGES, getMessages);
        return Mono.create(sink -> pecImapBridgeClient.getMessagesAsync(getMessages, outputFuture -> {
                    try {
                        var result = outputFuture.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception throwable) {
                        MDCUtils.enrichWithMDC(null, mdcContextMap);
                        endSoapRequest(sink, throwable);
                    }
                })).cast(GetMessagesResponse.class)
                .map(getMessagesResponse -> {
                    PnGetMessagesResponse pnGetMessagesResponse = new PnGetMessagesResponse();
                    List<byte[]> messages = getMessagesResponse.getArrayOfMessages() == null ?
                            List.of() : getMessagesResponse.getArrayOfMessages().getItem();

                    pnGetMessagesResponse.setPnListOfMessages(new PnListOfMessages(messages));
                    return pnGetMessagesResponse;
                })
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGES, result))
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));

    }


   public  Mono<Void> markMessageAsRead(String messageID) {
        GetMessageID getMessageID = new GetMessageID();
        getMessageID.setUser(arubaSecretValue.getPecUsername());
        getMessageID.setPass(arubaSecretValue.getPecPassword());
        getMessageID.setMailid(messageID);
        getMessageID.setIsuid(2);
        getMessageID.setMarkseen(1);
        var mdcContextMap = MDCUtils.retrieveMDCContextMap();
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, ARUBA_GET_MESSAGE_ID, getMessageID);
        return Mono.create(sink -> pecImapBridgeClient.getMessageIDAsync(getMessageID, outputFuture -> {
                    try {
                        var result = outputFuture.get();
                        checkErrors(result.getErrcode(), result.getErrstr());
                        sink.success(result);
                    } catch (Exception throwable) {
                        MDCUtils.enrichWithMDC(null, mdcContextMap);
                        endSoapRequest(sink, throwable);
                    }
                })).cast(GetMessageIDResponse.class)
                .then()
                .doOnSuccess(result -> log.info(CLIENT_METHOD_RETURN, ARUBA_GET_MESSAGE_ID, result))
                .onErrorResume(throwable -> Mono.error(new PnSpapiTemporaryErrorException(throwable.getMessage(), throwable)));

   }

    private void checkErrors(Integer errorCode, String errorStr) {
        if (!errorCode.equals(0))
            throw new ArubaCallException(errorStr, errorCode);
    }

    private void endSoapRequest(MonoSink<Object> sink, Throwable throwable) {
        log.error(throwable.getMessage());
        sink.error(throwable);
        Thread.currentThread().interrupt();
    }


    public static boolean isAruba(String messageID) {
        return messageID.trim().toLowerCase().endsWith(ARUBA_PATTERN_STRING);
    }


}
