package it.pagopa.pn.ec.commons.service.impl;

import it.pagopa.pn.ec.commons.exception.XmlParserException;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pec.daticert.DaticertDate;
import it.pec.daticert.Postacert;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class DatiCertServiceImpl implements DaticertService {

    private final Unmarshaller unmarshaller;

    public DatiCertServiceImpl(Unmarshaller unmarshaller) {
        this.unmarshaller = unmarshaller;
    }

    @Override
    public Postacert getPostacertFromByteArray(byte[] bytes) {
        try {
            return (Postacert) unmarshaller.unmarshal(new ByteArrayInputStream(bytes));
        } catch (JAXBException e) {
            log.error(e.getMessage(), e);
            throw new XmlParserException("JAXBException during input stream unmarshalling");
        }
    }

    public static OffsetDateTime createTimestampFromDaticertDate(DaticertDate daticertDate) {
        var ora = daticertDate.getOra();
        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var localDate = LocalDate.parse(daticertDate.getGiorno(), formatter);
        var offset = ZoneOffset.of(daticertDate.getZona());
        return OffsetDateTime.of(localDate, LocalTime.of(ora.getHour(), ora.getMinute(), ora.getSecond()), offset);
    }
}
