package it.pagopa.pn.ec.commons.configuration.http;

import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class PecImapBridgeConf {

    @Bean
    public PecImapBridge pecImapBridge() throws IOException {
        return new PecImapBridge_Service(new ClassPathResource("pec/PecImapBridgeBWS1.5.wsdl").getURL()).getPecImapBridgeSOAP();
    }
}
