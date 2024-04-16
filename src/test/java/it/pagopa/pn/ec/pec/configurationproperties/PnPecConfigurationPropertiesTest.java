package it.pagopa.pn.ec.pec.configurationproperties;

import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import lombok.CustomLog;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTestWebEnv
@CustomLog
public class PnPecConfigurationPropertiesTest {

    @Autowired
    PnPecConfigurationProperties pnPecConfigurationProperties;

    private final String ARUBA = "aruba";
    private final String NAMIRIAL = "namirial";
    private String PROVIDER_SWITCH_DEFAULT_READ = "aruba";
    private String PROVIDER_SWITCH_DEFAULT_WRITE = "aruba";
    private final String DATE_R_ARUBA_W_ARUBA = "2022-12-02T00:00:00Z";
    private final String DATE_R_ARUBA_NAM_W_NAM = "2023-01-02T23:59:58Z";
    private final String DATE_R_NAM_W_ARUBA = "2023-02-02T00:00:00Z";
    private final String DATE_DEFAULT="1970-01-01T00:00:00Z";
    private final String PN_PEC_PROVIDER_SWITCH_WRITE = "pnPecProviderSwitchWrite";
    private final String PN_PEC_PROVIDER_SWITCH_READ = "pnPecProviderSwitchRead";

    @BeforeEach
    void setUp() {
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_NAM_W_ARUBA).getMillis());
        PROVIDER_SWITCH_DEFAULT_READ = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH_READ);
        PROVIDER_SWITCH_DEFAULT_WRITE = (String) ReflectionTestUtils.getField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH_WRITE);


    }

    @AfterEach
    void afterEach() {
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH_READ, PROVIDER_SWITCH_DEFAULT_READ);
        ReflectionTestUtils.setField(pnPecConfigurationProperties, PN_PEC_PROVIDER_SWITCH_WRITE, PROVIDER_SWITCH_DEFAULT_WRITE);
    }

    @AfterAll
    static void afterAll() {
        DateTimeUtils.setCurrentMillisSystem();
    }

        @Test
        void testWithDateRArubaWAruba() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_W_ARUBA).getMillis());
            Assertions.assertEquals(ARUBA, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
            assertThat(pnPecConfigurationProperties.getPnPecProviderSwitchRead(), Matchers.containsInAnyOrder(ARUBA));
        }

        @Test
        void testWithDateRNamirialWAruba() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_NAM_W_ARUBA).getMillis());
            Assertions.assertEquals(ARUBA, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
            assertThat(pnPecConfigurationProperties.getPnPecProviderSwitchRead(), Matchers.containsInAnyOrder(NAMIRIAL));
        }

        @Test
        void testWithDateRArubaNamirialWNamirial() {
            DateTimeUtils.setCurrentMillisFixed(DateTime.parse(DATE_R_ARUBA_NAM_W_NAM).getMillis());
            Assertions.assertEquals(NAMIRIAL, pnPecConfigurationProperties.getPnPecProviderSwitchWrite());
            assertThat(pnPecConfigurationProperties.getPnPecProviderSwitchRead(), Matchers.containsInAnyOrder(ARUBA, NAMIRIAL));
        }

}