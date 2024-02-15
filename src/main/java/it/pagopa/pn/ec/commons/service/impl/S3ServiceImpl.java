package it.pagopa.pn.ec.commons.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.ec.commons.configurationproperties.s3.S3Properties;
import it.pagopa.pn.ec.commons.service.S3Service;
import lombok.CustomLog;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import software.amazon.awssdk.core.BytesWrapper;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.stream.Stream;

import static it.pagopa.pn.ec.commons.utils.LogUtils.*;

@Service
@CustomLog
public class S3ServiceImpl implements S3Service {

    private final S3AsyncClient s3AsyncClient;
    private final S3Properties s3Properties;
    private final RetryBackoffSpec s3RetryStrategy;
    private final ObjectMapper objectMapper;

    public S3ServiceImpl(S3AsyncClient s3AsyncClient, S3Properties s3Properties, ObjectMapper objectMapper) {
        this.s3AsyncClient = s3AsyncClient;
        this.s3Properties = s3Properties;
        this.objectMapper = objectMapper;
        this.s3RetryStrategy = Retry.backoff(s3Properties.maxAttempts(), Duration.ofSeconds(s3Properties.minBackoff()))
                .filter(S3Exception.class::isInstance)
                .doBeforeRetry(retrySignal -> log.debug(SHORT_RETRY_ATTEMPT, retrySignal.totalRetries(), retrySignal.failure(), retrySignal.failure().getMessage()))
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure());
    }

    @Override
    public <T> Mono<T> getObjectAndConvert(String key, String bucketName, Class<T> classToConvert) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, GET_OBJECT_AND_CONVERT, Stream.of(key, bucketName, classToConvert).toList());
        return Mono.fromCompletionStage(s3AsyncClient.getObject(builder -> builder.key(key).bucket(bucketName),
                        AsyncResponseTransformer.toBytes()))
                .map(BytesWrapper::asInputStream)
                .map(inputStream -> convertToClass(inputStream, classToConvert))
                .doOnNext(convertedObject -> log.info(CLIENT_METHOD_RETURN, GET_OBJECT_AND_CONVERT, key))
                .retryWhen(s3RetryStrategy);
    }

    @Override
    public <T> Mono<String> convertAndPutObject(String bucketName, T object) {
        log.debug(CLIENT_METHOD_INVOCATION_WITH_ARGS, CONVERT_AND_PUT_OBJECT, Stream.of(bucketName, object).toList());
        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(object))
                .zipWhen(fileBytes -> Mono.just(new String(Base64.encodeBase64(DigestUtils.md5(fileBytes)))))
                .flatMap(tuple -> {
                    String contentMD5 = tuple.getT2();
                    String fileKey = contentMD5 + ".eml";
                    return Mono.fromCompletionStage(s3AsyncClient.putObject(builder -> builder.key(fileKey)
                                            .contentMD5(contentMD5)
                                            .bucket(bucketName),
                                    AsyncRequestBody.fromBytes(tuple.getT1())))
                            .thenReturn(fileKey);
                })
                .doOnNext(fileKey -> log.info(CLIENT_METHOD_RETURN, CONVERT_AND_PUT_OBJECT, fileKey))
                .retryWhen(s3RetryStrategy)
                .doOnError(throwable -> log.warn(CLIENT_METHOD_RETURN_WITH_ERROR, CONVERT_AND_PUT_OBJECT, throwable, throwable.getMessage()));
    }

    @SneakyThrows(IOException.class)
    private <T> T convertToClass(InputStream inputStream, Class<T> classToConvert) {
        return objectMapper.readValue(inputStream, classToConvert);
    }
}
