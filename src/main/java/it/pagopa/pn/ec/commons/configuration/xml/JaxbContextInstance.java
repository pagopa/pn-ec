package it.pagopa.pn.ec.commons.configuration.xml;

import it.pec.daticert.Postacert;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaxbContextInstance {

    @Bean
    public Unmarshaller getJaxbContextUnmarshaller() throws JAXBException {
        return JAXBContext.newInstance(Postacert.class).createUnmarshaller();
    }
}
