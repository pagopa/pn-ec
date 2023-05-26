package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.commons.model.pojo.MonoResultWrapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class ReactorUtils {

    private ReactorUtils() {
        throw new IllegalStateException("ReactorUtils is a utility class");
    }

    public static <T> Function<Mono<T>, Flux<MonoResultWrapper<T>>> pullFromMonoUntilIsEmpty() {
        return tMono -> tMono.map(MonoResultWrapper::new)
                             .defaultIfEmpty(new MonoResultWrapper<>(null))
                             .repeat()
                             .takeWhile(MonoResultWrapper::isNotEmpty);
    }

    public static <T> Function<Flux<T>, Flux<MonoResultWrapper<T>>> pullFromFluxUntilIsEmpty() {
        return tFlux -> tFlux.map(MonoResultWrapper::new)
                .defaultIfEmpty(new MonoResultWrapper<>(null))
                .repeat()
                .takeWhile(MonoResultWrapper::isNotEmpty);
    }

}
