package it.pagopa.pn.ec.pec.configurationProperties;

import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTestWebEnv
@CustomLog
public class PnPecConfigurationPropertiesTest {
    @Autowired
    PnPecConfigurationProperties pnPecConfigurationProperties;

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2020-01-01").getMillis());
        pnPecConfigurationProperties=new PnPecConfigurationProperties();
    }

    @Test
    void testTipoRicevutaActualPropertyValueOk() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-12-31T23:59:58Z").getMillis());
        Assertions.assertEquals(false, pnPecConfigurationProperties.getTipoRicevutaBreve());
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2050-01-01T00:00:00Z").getMillis());
        Assertions.assertEquals(true, pnPecConfigurationProperties.getTipoRicevutaBreve());
    }

    @Test
    void testTipoRicevutaBreveSingleValueOk(){
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "true");
        Assertions.assertEquals(true, pnPecConfigurationProperties.getTipoRicevutaBreve());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "false");
        Assertions.assertEquals(false, pnPecConfigurationProperties.getTipoRicevutaBreve());
    }

    @Test
    void testTipoRicevutaKoUnparsableValue(){
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "true;test;false;");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getTipoRicevutaBreve());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "true;2021-01-01T23:59:59Z");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getTipoRicevutaBreve());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "true;2021-01-01T23:59:59Z;false;false");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getTipoRicevutaBreve());
    }

    @Test
    void testTipoRicevutaBreveDateOk() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "true;2023-01-01T23:59:59Z;false");
        Assertions.assertEquals(true, pnPecConfigurationProperties.getTipoRicevutaBreve());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "tipoRicevutaBreve", "true;2019-01-01T23:59:59Z;false");
        Assertions.assertEquals(false, pnPecConfigurationProperties.getTipoRicevutaBreve());
    }

}
