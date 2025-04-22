package it.pagopa.pn.library.pec.configuration;

import com.namirial.pec.library.service.PnPecServiceImpl;
import it.pagopa.pn.ec.dummy.pec.conf.DummyPecSharedAutoConfiguration;
import it.pagopa.pn.ec.dummy.pec.service.DummyPecService;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.*;

@Configuration
@CustomLog
@Import(DummyPecSharedAutoConfiguration.class)
public class PnPecProvidersConf {

    private final Set<String> namirialPropertiesKeySet = Set.of("namirial.server.imap.address",
            "namirial.server.smtp.address",
            "namirial.server.imap.port",
            "namirial.server.smtp.port",
            "namirial.pool.imap.maxtotal",
            "namirial.pool.imap.maxidle",
            "namirial.pool.imap.minidle",
            "namirial.pool.smtp.maxtotal",
            "namirial.pool.smtp.maxidle",
            "namirial.pool.smtp.minidle",
            "namirial.server.cache",
            "namirial.server.cache.endpoint",
            "namirial.metric.duplicate.receipt.namespace",
            "namirial.metric.duplicate.receipt.name",
            "namirial.warning-to-notdelivered.logic");

    @Bean
    public PnPecServiceImpl namirialService(@Autowired Environment env) {
        namirialPropertiesKeySet.forEach(key -> {
            String property = env.getRequiredProperty(key);
            System.setProperty(key, property);
        });
        return new PnPecServiceImpl();
    }

    @Bean
    public DummyPecService dummyPecService() {
        return new DummyPecService();
    }

}
