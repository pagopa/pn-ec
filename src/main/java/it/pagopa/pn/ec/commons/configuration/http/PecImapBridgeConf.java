package it.pagopa.pn.ec.commons.configuration.http;

import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import lombok.CustomLog;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static it.pagopa.pn.ec.commons.utils.LogUtils.INITIALIZING_ARUBA_PROXY_CLIENT;

@Configuration
@CustomLog
public class PecImapBridgeConf {

    @Value("${aruba.server.address}")
    private String arubaServerAddress;

    @Bean
    public PecImapBridge pecImapBridge() {
        var endpointName = PecImapBridge_Service.PecImapBridgeSOAP;
        var serviceName = PecImapBridge_Service.SERVICE;
        log.debug(INITIALIZING_ARUBA_PROXY_CLIENT, "pn-ec", arubaServerAddress, endpointName, serviceName);
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(PecImapBridge.class);
        factory.setAddress(arubaServerAddress);
        factory.setEndpointName(endpointName);
        factory.setServiceName(serviceName);
        return factory.create(PecImapBridge.class);
    }
}
