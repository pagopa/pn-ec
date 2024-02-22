package it.pagopa.pn.library.pec.configuration;

import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import lombok.CustomLog;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transport.http.HttpConduitFeature;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;
import java.util.List;

import static it.pagopa.pn.ec.commons.utils.LogUtils.INITIALIZING_ARUBA_PROXY_CLIENT;

@Configuration
@CustomLog
public class PecImapBridgeConfiguration {

    @Value("${aruba.server.address}")
    private String arubaServerAddress;

    @Bean
    public PecImapBridge pecImapBridgeClient() {
        QName endpointName = PecImapBridge_Service.PecImapBridgeSOAP;
        QName serviceName = PecImapBridge_Service.SERVICE;
        log.debug(INITIALIZING_ARUBA_PROXY_CLIENT, "pn-library.pec", arubaServerAddress, endpointName, serviceName);
        return initializeFactory(endpointName, serviceName, List.of(disabledChunkingFeature())).create(PecImapBridge.class);
    }

    private JaxWsProxyFactoryBean initializeFactory(QName endpointName, QName serviceName, List<Feature> features) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(PecImapBridge.class);
        factory.setAddress(arubaServerAddress);
        factory.setEndpointName(endpointName);
        factory.setServiceName(serviceName);
        factory.setFeatures(features);
        return factory;
    }

    //Custom feature to disable chunking transfer encoding
    private Feature disabledChunkingFeature() {
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);

        HttpConduitConfig httpConduitConfig = new HttpConduitConfig();
        httpConduitConfig.setClientPolicy(httpClientPolicy);

        HttpConduitFeature feature = new HttpConduitFeature();
        feature.setConduitConfig(httpConduitConfig);

        return feature;
    }

}
