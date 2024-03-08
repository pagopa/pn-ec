package it.pagopa.pn.ec.pec.configurationProperties;

import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTestWebEnv
@CustomLog
public class PnPecConfigurationPropertiesTest {

    @Autowired
    PnPecConfigurationProperties pnPecConfigurationProperties;
    private String PROVIDER_SWITCH_DEFAULT = "aruba";

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2020-01-01").getMillis());
        PROVIDER_SWITCH_DEFAULT = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, "pnPecProviderSwitch");
    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitch", PROVIDER_SWITCH_DEFAULT);
    }

    @AfterAll
    static void afterAll() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    void testTipoRicevutaActualPropertyValueOk() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2020-12-31T23:59:58Z").getMillis());
        Assertions.assertEquals("other", pnPecConfigurationProperties.getPnPecProviderSwitch());
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2023-01-01T00:00:00Z").getMillis());
        Assertions.assertEquals("aruba", pnPecConfigurationProperties.getPnPecProviderSwitch());
    }

    @Test
    void testpnPecProviderSwitchSingleValueOk() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitch", "aruba");
        Assertions.assertEquals("aruba", pnPecConfigurationProperties.getPnPecProviderSwitch());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitch", "other");
        Assertions.assertEquals("other", pnPecConfigurationProperties.getPnPecProviderSwitch());
    }

    @Test
    void testTipoRicevutaKoUnparsableValue() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitch", "aruba;aruba;other;");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitch());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitch", "aruba;2021-01-01T23:59:59Z");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitch());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, "pnPecProviderSwitch", "aruba;2021-01-01T23:59:59Z;false;false");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitch());
    }

}