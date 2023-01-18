package it.pagopa.pn.ec.service;

import it.pagopa.pn.ec.localstack.SQSLocalStackTestConfig;
import it.pagopa.pn.ec.service.impl.SmsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import static it.pagopa.pn.ec.testutils.constant.RestApiTestConstants.DEFAULT_ID_CLIENT_HEADER;
import static it.pagopa.pn.ec.testutils.factory.EcRequestObjectFactory.getDigitalCourtesySmsRequest;

@SpringBootTestWebEnv
@Import(SQSLocalStackTestConfig.class)
class SmsServiceTest {

    @Autowired
    private SmsService smsService;

    @Test
    void presaInCaricoSmsTest(){
        smsService.presaInCarico(DEFAULT_ID_CLIENT_HEADER, getDigitalCourtesySmsRequest());
    }
}
