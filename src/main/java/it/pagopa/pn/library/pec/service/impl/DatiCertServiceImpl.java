package it.pagopa.pn.library.pec.service.impl;

import it.pagopa.pn.ec.commons.exception.XmlParserException;
import it.pagopa.pn.library.pec.exception.daticert.DaticertServiceException;
import it.pagopa.pn.library.pec.model.IPostacert;
import it.pagopa.pn.library.pec.service.DaticertService;
import it.pagopa.pn.library.pec.model.pojo.Data;
import it.pagopa.pn.library.pec.model.pojo.Postacert;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static it.pagopa.pn.library.pec.utils.PnPecUtils.*;

@Service
@CustomLog
public class DatiCertServiceImpl implements DaticertService {

    private final JAXBContext jaxbContext;

    @Value("${library.pec.aruba.postacert.path}")
    String arubaPostacertClassType;
    @Value("${library.pec.pn.postacert.path}")
    String pnPostacertClassType;

    public DatiCertServiceImpl(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    @Override
    public IPostacert getPostacertFromByteArray(byte[] bytes, String providerName) {
        try {
//          This method could be called in a non-thread safe context; is good to create for each unmarshall operation a jakarta.xml.bind
//          .JAXBContext.Unmarshaller object instead of a Spring Bean ?
//          Yes the jakarta.xml.bind.JAXBContext is thread safe but jakarta.xml.bind.JAXBContext.Unmarshaller no
//          https://javaee.github.io/jaxb-v2/doc/user-guide/ch06.html#d0e6879
//          https://stackoverflow.com/questions/7400422/jaxb-creating-context-and-marshallers-cost
            Class<?> dynamicClass = Class.forName(getPostacertClassType(providerName)).asSubclass(Postacert.class);
            return (IPostacert) dynamicClass.getDeclaredConstructor(Postacert.class).newInstance((Postacert) jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(bytes)));
        } catch (JAXBException e) {

            throw new XmlParserException("JAXBException during input stream unmarshalling");
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new DaticertServiceException("Exception during custom Postacert object instantiation : " + e);
        }
    }

    public static OffsetDateTime createTimestampFromDaticertDate(Data daticertDate) {
//      daticertDate.getOra() -> hh:mm:ss
//      daticertDate.getGiorno() -> dd/MM/yyyy
//      daticertDate.getZona() -> +0200
        var hhMmSs = Arrays.stream(daticertDate.getOra().split(":")).map(Integer::valueOf).toList();
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var localDate = LocalDate.parse(daticertDate.getGiorno(), formatter);
        var offset = ZoneOffset.of(daticertDate.getZona());
        var localTime = LocalTime.of(hhMmSs.get(0), hhMmSs.get(1), hhMmSs.get(2));
        var localDateTime = LocalDateTime.of(localDate, localTime);
        return OffsetDateTime.of(localDateTime, offset);
    }

    private String getPostacertClassType(String providerName) {
        String postacertClassType = switch (providerName) {
            case ARUBA_PROVIDER -> arubaPostacertClassType;
            case NAMIRIAL_PROVIDER, DUMMY_PROVIDER -> pnPostacertClassType;
            default -> throw new IllegalArgumentException(String.format("There is no postacert type for '%s' provider", providerName));
        };
        log.debug("PostacertClassType : {}", postacertClassType);
        return postacertClassType;
    }
}
