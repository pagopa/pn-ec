package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.scaricamentoesitipec.utils.CloudWatchPecMetrics;
import it.pagopa.pn.library.pec.pojo.PnGetMessagesResponse;
import it.pagopa.pn.library.pec.service.AlternativeProviderService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@CustomLog
public class AlternativeProviderServiceImpl implements AlternativeProviderService {

    private final CloudWatchPecMetrics cloudWatchPecMetrics;
    private final String alternativeProviderNamespace;

    @Autowired
    public AlternativeProviderServiceImpl(CloudWatchPecMetrics cloudWatchPecMetrics, @Value("${library.pec.cloudwatch.namespace.alternative}") String alternativeProviderNamespace) {
        this.cloudWatchPecMetrics = cloudWatchPecMetrics;
        this.alternativeProviderNamespace = alternativeProviderNamespace;
    }

    @Override
    public Mono<String> sendMail(byte[] message) {
        return Mono.just("");
    }

    @Override
    public Mono<PnGetMessagesResponse> getUnreadMessages(int limit) {
        return Mono.just(new PnGetMessagesResponse());
    }

    @Override
    public Mono<Void> markMessageAsRead(String messageID) { return Mono.empty(); }

    @Override
    public Mono<Integer> getMessageCount() {
        return Mono.just(0)
                .flatMap(count -> cloudWatchPecMetrics.publishMessageCount(Long.valueOf(count), alternativeProviderNamespace).thenReturn(count));
    }

    @Override
    public Mono<Void> deleteMessage(String messageID) {
        return Mono.empty();
    }


}