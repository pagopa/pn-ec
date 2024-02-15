package it.pagopa.pn.ec.commons.service;

import reactor.core.publisher.Mono;

public interface S3Service {

    <T> Mono<T> getObjectAndConvert(String key, String bucketName, Class<T> classToConvert);

    <T> Mono<String> convertAndPutObject(String bucketName, T object);

}
