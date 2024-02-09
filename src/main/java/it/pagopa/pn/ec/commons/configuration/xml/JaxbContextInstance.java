package it.pagopa.pn.ec.commons.configuration.xml;

import it.pagopa.pn.library.pec.pojo.PnPostacert;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaxbContextInstance {

    @Bean
    public JAXBContext getJaxbContextUnmarshaller() throws JAXBException {
        return JAXBContext.newInstance(PnPostacert.class);
    }
}
