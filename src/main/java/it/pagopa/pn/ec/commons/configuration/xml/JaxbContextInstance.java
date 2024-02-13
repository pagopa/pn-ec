package it.pagopa.pn.ec.commons.configuration.xml;

import it.pagopa.pn.library.pec.pojo.PnPostacert;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaxbContextInstance {

    @Bean
    public JAXBContext getJaxbContextUnmarshaller() throws JAXBException {
        return JAXBContext.newInstance(PnPostacert.class);
    }
}
