package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.XmlParserException;
import it.pagopa.pn.ec.commons.model.pojo.pec.PnPostacert;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pec.daticert.Data;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Service
@Slf4j
public class DatiCertServiceImpl implements DaticertService {

    private final JAXBContext jaxbContext;

    public DatiCertServiceImpl(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    @Override
    public PnPostacert getPostacertFromByteArray(byte[] bytes) {
        try {

//          This method could be called in a non-thread safe context; is good to create for each unmarshall operation a jakarta.xml.bind
//          .JAXBContext.Unmarshaller object instead of a Spring Bean ?
//          Yes the jakarta.xml.bind.JAXBContext is thread safe but jakarta.xml.bind.JAXBContext.Unmarshaller no
//          https://javaee.github.io/jaxb-v2/doc/user-guide/ch06.html#d0e6879
//          https://stackoverflow.com/questions/7400422/jaxb-creating-context-and-marshallers-cost

            return (PnPostacert) jaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(bytes));
        } catch (JAXBException e) {
            throw new XmlParserException("JAXBException during input stream unmarshalling");
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
}
