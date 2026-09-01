package it.pagopa.pn.ec.pec.service.impl;

import it.pagopa.pn.ec.commons.configurationproperties.sqs.NotificationTrackerSqsName;
import it.pagopa.pn.ec.commons.rest.call.download.DownloadCall;
import it.pagopa.pn.ec.commons.rest.call.ec.gestorerepository.GestoreRepositoryCall;
import it.pagopa.pn.ec.commons.service.AuthService;
import it.pagopa.pn.ec.commons.service.SqsService;
import it.pagopa.pn.ec.commons.service.impl.AttachmentServiceImpl;
import it.pagopa.pn.ec.pec.configurationproperties.PecSqsQueueName;
import it.pagopa.pn.ec.pec.configurationproperties.PnPecConfigurationProperties;
import it.pagopa.pn.library.exceptions.PnSpapiPermanentErrorException;
import it.pagopa.pn.library.exceptions.PnSpapiTemporaryErrorException;
import it.pagopa.pn.library.pec.service.PnEcPecService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PecServiceIsAddressExceptionTest {

    @Mock
    private AuthService authService;
    @Mock
    private PnEcPecService pnPecService;
    @Mock
    private GestoreRepositoryCall gestoreRepositoryCall;
    @Mock
    private SqsService sqsService;
    @Mock
    private AttachmentServiceImpl attachmentService;
    @Mock
    private DownloadCall downloadCall;
    @Mock
    private NotificationTrackerSqsName notificationTrackerSqsName;
    @Mock
    private PecSqsQueueName pecSqsQueueName;
    @Mock
    private PnPecConfigurationProperties pnPecProps;

    private Predicate<Throwable> isAddressException;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        var pecService = new PecService(authService, pnPecService, gestoreRepositoryCall, sqsService, attachmentService, downloadCall, notificationTrackerSqsName, pecSqsQueueName, 1, pnPecProps);
        Field isAddressExceptionField = PecService.class.getDeclaredField("isAddressException");
        isAddressExceptionField.setAccessible(true);
        isAddressException = (Predicate<Throwable>) isAddressExceptionField.get(pecService);
    }

    @Test
    void isAddressException_AddressExceptionKo() {
        assertTrue(isAddressException.test(new PnSpapiPermanentErrorException("class jakarta.mail.internet.AddressException Local address starts with dot")));
    }

    @Test
    void isAddressException_InvalidAddressesKo() {
        assertTrue(isAddressException.test(new PnSpapiPermanentErrorException("class jakarta.mail.SendFailedException Invalid Addresses")));
    }

    @Test
    void isAddressException_OtherSendFailedExceptionKo() {
        assertFalse(isAddressException.test(new PnSpapiPermanentErrorException("class jakarta.mail.SendFailedException Some other reason")));
    }

    @Test
    void isAddressException_TemporaryErrorKo() {
        assertFalse(isAddressException.test(new PnSpapiTemporaryErrorException("qualsiasi messaggio")));
    }
}
