package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.rest.call.aruba.ArubaCall;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class ScaricamentoEsitiPecScheduler {

    private final ArubaCall arubaCall;

    public ScaricamentoEsitiPecScheduler(ArubaCall arubaCall) {
        this.arubaCall = arubaCall;
    }

    @Scheduled(fixedDelay = 1000)
    void scaricamentoEsitiPec(){

    }
}
