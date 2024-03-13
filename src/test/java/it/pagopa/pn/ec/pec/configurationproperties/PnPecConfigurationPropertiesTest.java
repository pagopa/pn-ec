package it.pagopa.pn.ec.pec.configurationproperties;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

@SpringBootTestWebEnv
@CustomLog
public class PnPecConfigurationPropertiesTest {

    @Autowired
    PnPecConfigurationProperties pnPecConfigurationProperties;

    private final String ARUBA = "aruba";
    private final String OTHER = "other";
    private String PROVIDER_SWITCH_DEFAULT = "aruba";
    private final String DATE_R_ARUBA_W_ARUBA = "2022-12-02T00:00:00Z";
    private final String DATE_R_ARUBA_OTHER_W_OTHER = "2023-01-02T23:59:58Z";
    private final String DATE_R_OTHER_W_ARUBA = "2023-02-02T00:00:00Z";
    private final String DATE_DEFAULT="1970-01-01T00:00:00Z";
    private final String PN_PEC_PROVIDER_SWITCH = "pnPecProviderSwitch";

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_OTHER_W_ARUBA).getMillis());
        PROVIDER_SWITCH_DEFAULT = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH);
    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, PROVIDER_SWITCH_DEFAULT);
    }

    @AfterAll
    static void afterAll() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Nested
    class TestTipoRicevutaActualPropertyValueOk {

        @Test
        void testWithDateRArubaWAruba() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_W_ARUBA).getMillis());
            Assertions.assertEquals(ARUBA, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
            Assertions.assertEquals(List.of(ARUBA), pnPecConfigurationProperties.getPnPecProviderSwitchRead());
        }

        @Test
        void testWithDateROtherWAruba() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_OTHER_W_ARUBA).getMillis());
            Assertions.assertEquals(ARUBA, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
            Assertions.assertEquals(List.of(OTHER), pnPecConfigurationProperties.getPnPecProviderSwitchRead());
        }

        @Test
        void testWithDateRArubaOtherWOther() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_OTHER_W_OTHER).getMillis());
            Assertions.assertEquals(OTHER, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
            Assertions.assertEquals(List.of(ARUBA, OTHER), pnPecConfigurationProperties.getPnPecProviderSwitchRead());
        }
    }


    @Test
    void testTipoRicevutaKoInvalidValues() {
        String invalidDate = "1969-12-31T23:59:59Z";
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH, DATE_DEFAULT + ";" + ARUBA + "|" + OTHER);
        Assertions.assertThrows(RuntimeException.class, () -> pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
    }

}