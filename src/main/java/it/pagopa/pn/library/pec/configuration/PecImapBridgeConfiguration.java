package it.pagopa.pn.library.pec.configuration;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import lombok.CustomLog;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.HttpConduitConfig;
import org.apache.cxf.transport.http.HttpConduitFeature;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static it.pagopa.pn.ec.commons.utils.LogUtils.INITIALIZING_ARUBA_PROXY_CLIENT;
import static org.apache.cxf.helpers.HttpHeaderHelper.CHUNKED;

@Configuration
@CustomLog
public class PecImapBridgeConfiguration {

    @Value("${aruba.server.address}")
    private String arubaServerAddress;
    @Bean
    public PecImapBridge pecImapBridgeClient() {
        var endpointName = PecImapBridge_Service.PecImapBridgeSOAP;
        var serviceName = PecImapBridge_Service.SERVICE;
        log.debug(INITIALIZING_ARUBA_PROXY_CLIENT, "pn-library.pec", arubaServerAddress, endpointName, serviceName);
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(PecImapBridge.class);
        factory.setAddress(arubaServerAddress);
        factory.setEndpointName(PecImapBridge_Service.PecImapBridgeSOAP);
        factory.setServiceName(PecImapBridge_Service.SERVICE);

        //Custom character escape handler to avoid characters escaping inside CDataTag
//        CharacterEscapeHandler characterEscapeHandler = (ch, start, length, isAttVal, out) -> out.write(ch);
//        JAXBDataBinding jaxbDataBinding = new JAXBDataBinding();
//        Map<String, Object> marshallerProperties = new HashMap<>();
//        marshallerProperties.put("com.sun.xml.bind.characterEscapeHandler", characterEscapeHandler);
//        jaxbDataBinding.setMarshallerProperties(marshallerProperties);
//        factory.setDataBinding(jaxbDataBinding);

        //Custom feature to disable chunked transfer encoding
        HttpConduitFeature feature = new HttpConduitFeature();
        HttpConduitConfig httpConduitConfig = new HttpConduitConfig();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        httpConduitConfig.setClientPolicy(httpClientPolicy);
        feature.setConduitConfig(httpConduitConfig);
        factory.setFeatures(List.of(feature));

        return factory.create(PecImapBridge.class);
    }
}
