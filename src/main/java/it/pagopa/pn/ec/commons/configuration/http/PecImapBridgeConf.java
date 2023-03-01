package it.pagopa.pn.ec.commons.configuration.http;

import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PecImapBridgeConf {

    @Bean
    public PecImapBridge pecImapBridge(){
        return new PecImapBridge_Service().getPecImapBridgeSOAP();
    }
}
