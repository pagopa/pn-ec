package it.pagopa.pn.ec.commons.service;

import it.pagopa.pn.ec.commons.model.pojo.s3.S3Pointer;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

public interface S3Service {

    <T> Mono<T> getObjectAndConvert(String key, String bucketName, Class<T> classToConvert);

    <T> Mono<String> convertAndPutObject(String bucketName, T object);

    Mono<DeleteObjectResponse> deleteObject(String key, String bucketName);

}
