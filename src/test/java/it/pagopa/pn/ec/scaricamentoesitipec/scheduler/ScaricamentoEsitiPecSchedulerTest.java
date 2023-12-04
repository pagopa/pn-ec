package it.pagopa.pn.ec.scaricamentoesitipec.scheduler;

import it.pagopa.pn.ec.commons.configurationproperties.TransactionProcessConfigurationProperties;
import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.DaticertService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.library.pec.service.ArubaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTestWebEnv
class ScaricamentoEsitiPecSchedulerTest {

    @MockBean
    private ArubaService arubaService;

    @Autowired
    private DaticertService daticertService;

    @MockBean
    private GestoreRepositoryCall gestoreRepositoryCall;

    @Autowired
    private SqsService sqsService;

    @Autowired
    private NotificationTrackerSqsName notificationTrackerSqsName;

    @Autowired
    private TransactionProcessConfigurationProperties transactionProcessConfigurationProperties;
    @Autowired
    private ScaricamentoEsitiPecScheduler scaricamentoEsitiPecScheduler;

    @Test
    void scaricamentoEsitiPecOk() {

    }

}
