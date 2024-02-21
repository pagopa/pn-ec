package it.pagopa.pn.library.pec.configuration;

import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import it.pec.bridgews.PecImapBridge;
import it.pec.bridgews.PecImapBridge_Service;
import lombok.CustomLog;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.ec.commons.utils.LogUtils.INITIALIZING_ARUBA_PROXY_CLIENT;

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
        CharacterEscapeHandler characterEscapeHandler = (ch, start, length, isAttVal, out) -> out.write(ch);
        JAXBDataBinding jaxbDataBinding = new JAXBDataBinding();
        Map<String, Object> marshallerProperties = new HashMap<>();
        marshallerProperties.put("com.sun.xml.bind.characterEscapeHandler", characterEscapeHandler);
        jaxbDataBinding.setMarshallerProperties(marshallerProperties);
        factory.setDataBinding(jaxbDataBinding);

        return factory.create(PecImapBridge.class);
    }
}
