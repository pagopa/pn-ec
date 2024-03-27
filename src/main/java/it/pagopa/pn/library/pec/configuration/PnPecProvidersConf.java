package it.pagopa.pn.library.pec.configuration;

import com.namirial.pec.library.service.PnPecServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PnPecProvidersConf {

    @Bean
    public PnPecServiceImpl namirialService() {
        return new PnPecServiceImpl();
    }

}
