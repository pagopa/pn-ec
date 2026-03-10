package it.pagopa.pn.ec.commons.configuration.scheduler;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "pn.ec.feature.flag.cartaceo.consolidatore",
        havingValue = "true",
        matchIfMissing = false
)
public class ShedLockRunner {

    @SchedulerLock(name="lavorazioneRichiestaBatch",
            lockAtMostFor = "${pn.ec.shedlock.lockAtMostFor}",
            lockAtLeastFor = "${pn.ec.shedlock.lockAtLeastFor}")
    public void runWithLock(String lockName, Runnable task) {
        log.info("Acquisito lock: {}", lockName);
        task.run();
        log.info("Rilasciato lock: {}", lockName);
    }
}
