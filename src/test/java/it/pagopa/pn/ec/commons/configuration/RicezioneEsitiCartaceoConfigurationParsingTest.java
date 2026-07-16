package it.pagopa.pn.ec.commons.configuration;

import it.pagopa.pn.ec.commons.constant.DuplicatesCheckMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RicezioneEsitiCartaceoConfigurationParsingTest {

    private RicezioneEsitiCartaceoConfiguration buildConfig(String duplicatesCheck) {
        RicezioneEsitiCartaceoConfiguration config = new RicezioneEsitiCartaceoConfiguration();
        ReflectionTestUtils.setField(config, "duplicatesCheck", duplicatesCheck);
        ReflectionTestUtils.setField(config, "duplicatedEventErrorCode", "400.02");
        config.init();
        return config;
    }

    @Test
    void init_emptyDuplicatesCheck_emptyMapAndNotConfiguredLookup(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig("");
        Assertions.assertTrue(config.getDuplicatesCheckModeByProduct().isEmpty());
        Assertions.assertEquals(DuplicatesCheckMode.NOT_CONFIGURED, config.getDuplicatesCheckMode("X"));
    }

    @Test
    void init_blankDuplicatesCheck_emptyMapAndNotConfiguredLookup(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig("   ");
        Assertions.assertTrue(config.getDuplicatesCheckModeByProduct().isEmpty());
        Assertions.assertEquals(DuplicatesCheckMode.NOT_CONFIGURED, config.getDuplicatesCheckMode("X"));
    }

    @Test
    void init_productWithTrailingColonAndNoMode_returnsBlocking(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig("AR:");
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("AR"));
    }

    @Test
    void init_tokensWithSurroundingSpaces_trimsProductTypeAndMode(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig(" AR : NONBLOCKING ; RS ");
        Assertions.assertEquals(DuplicatesCheckMode.NONBLOCKING, config.getDuplicatesCheckMode("AR"));
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("RS"));
        Assertions.assertArrayEquals(new String[]{"AR", "RS"}, config.getProductTypesToCheck());
    }

    @Test
    void init_nonblockingSuffixLowercase_isCaseInsensitive(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig("AR:nonblocking");
        Assertions.assertEquals(DuplicatesCheckMode.NONBLOCKING, config.getDuplicatesCheckMode("AR"));
    }

    @Test
    void init_duplicatedProductType_lastTokenWins(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig("AR:NONBLOCKING;AR");
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("AR"));
    }

    @Test
    void init_emptyTokensBetweenSeparators_areIgnored(){
        RicezioneEsitiCartaceoConfiguration config = buildConfig("AR;;RS");
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("AR"));
        Assertions.assertEquals(DuplicatesCheckMode.BLOCKING, config.getDuplicatesCheckMode("RS"));
        Assertions.assertEquals(2, config.getDuplicatesCheckModeByProduct().size());
    }

}
