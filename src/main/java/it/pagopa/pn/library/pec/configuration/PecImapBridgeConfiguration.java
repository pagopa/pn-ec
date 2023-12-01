package it.pagopa.pn.library.pec.configuration;

import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
public class PecImapBridgeConfiguration {

    @Bean
    public PecImapBridge pecImapBridgeClient() throws IOException {
        return new PecImapBridge_Service(new ClassPathResource("pec/PecImapBridgeBWS1.5.wsdl").getURL()).getPecImapBridgeSOAP();
    }
}
