package it.pagopa.pn.ec.pec.configurationproperties;

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

    private final String ARUBA = "aruba";
    private final String ALTERNATIVE = "other";
    private  String PROVIDER_SWITCH_DEFAULT = "aruba";
    private final String DATE_ARUBA= "2050-01-01T00:00:00Z";
    private final String DATE_ALTERNATIVE= "2020-12-31T23:59:58Z";
    private final String DATE_DEFAULT= "2020-01-01T00:00:00Z";
    private final String PN_PEC_PROVIDER_SWITCH = "pnPecProviderSwitch";

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_DEFAULT).getMillis());
        PROVIDER_SWITCH_DEFAULT = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH);
    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties,PN_PEC_PROVIDER_SWITCH,PROVIDER_SWITCH_DEFAULT);
    }

    @AfterAll
    static void afterAll() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    void testTipoRicevutaActualPropertyValueOk() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_ALTERNATIVE).getMillis());
        Assertions.assertEquals(ALTERNATIVE, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_ARUBA).getMillis());
        Assertions.assertEquals(ARUBA, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
    }

    @Test
    void testpnPecProviderSwitchSingleValueOk() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, ARUBA);
        Assertions.assertEquals(ARUBA, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, ALTERNATIVE);
        Assertions.assertEquals(ALTERNATIVE, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
    }

    @Test
    void testTipoRicevutaKoUnparsableValue() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, ARUBA+";"+ARUBA+";"+ALTERNATIVE);
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, ARUBA+";"+DATE_ALTERNATIVE);
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, ARUBA+";"+DATE_ALTERNATIVE+";false;false");
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
    }

}