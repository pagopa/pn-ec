package it.pagopa.pn.ec.scaricamentoesitipec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@Slf4j
public class ScaricamentoEsitiPec {

    @Scheduled(fixedDelay = 1000)
    public void scaricamentoEsitiPec() {
        // TODO implement scaricamentoEsitiPec
    }
}
