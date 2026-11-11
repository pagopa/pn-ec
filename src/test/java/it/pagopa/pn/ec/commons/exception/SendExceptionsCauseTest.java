package it.pagopa.pn.ec.commons.exception;

import it.pagopa.pn.ec.commons.exception.ses.SesSendException;
import it.pagopa.pn.ec.commons.exception.sns.SnsSendException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendExceptionsCauseTest {

    private static final RuntimeException CAUSE = new RuntimeException("original provider failure");

    @Test
    void snsSendException_withCause_keepsCause() {
        SnsSendException exception = new SnsSendException(CAUSE);

        assertSame(CAUSE, exception.getCause());
    }

    @Test
    void snsSendException_withCause_messageIsUnchangedAndDoesNotContainCause() {
        SnsSendException exception = new SnsSendException(CAUSE);

        assertEquals("An error occurred while sending via SNS", exception.getMessage());
    }

    @Test
    void snsMaxRetriesExceededException_withCause_keepsCause() {
        SnsSendException.SnsMaxRetriesExceededException exception = new SnsSendException.SnsMaxRetriesExceededException(CAUSE);

        assertSame(CAUSE, exception.getCause());
    }

    @Test
    void snsMaxRetriesExceededException_withCause_messageIsUnchangedAndDoesNotContainCause() {
        SnsSendException.SnsMaxRetriesExceededException exception = new SnsSendException.SnsMaxRetriesExceededException(CAUSE);

        assertEquals("SNS max retries exceeded", exception.getMessage());
    }

    @Test
    void sesSendException_withCause_keepsCause() {
        SesSendException exception = new SesSendException(CAUSE);

        assertSame(CAUSE, exception.getCause());
    }

    @Test
    void sesSendException_withCause_messageIsUnchangedAndDoesNotContainCause() {
        SesSendException exception = new SesSendException(CAUSE);

        assertEquals("An error occurred while sending via SES", exception.getMessage());
    }

    @Test
    void sesMaxRetriesExceededException_withCause_keepsCause() {
        SesSendException.SesMaxRetriesExceededException exception = new SesSendException.SesMaxRetriesExceededException(CAUSE);

        assertSame(CAUSE, exception.getCause());
    }

    @Test
    void sesMaxRetriesExceededException_withCause_messageIsUnchangedAndDoesNotContainCause() {
        SesSendException.SesMaxRetriesExceededException exception = new SesSendException.SesMaxRetriesExceededException(CAUSE);

        assertEquals("SES max retries exceeded", exception.getMessage());
    }

    @Test
    void maxRetriesExceededException_withCause_keepsCause() {
        MaxRetriesExceededException exception = new MaxRetriesExceededException(CAUSE);

        assertSame(CAUSE, exception.getCause());
    }

    @Test
    void maxRetriesExceededException_withCause_messageIsUnchangedAndDoesNotContainCause() {
        MaxRetriesExceededException exception = new MaxRetriesExceededException(CAUSE);

        assertEquals("Max retries exceeded", exception.getMessage());
    }

    @Test
    void existingConstructors_keepDefaultMessagesAndNoCause() {
        assertEquals("An error occurred while sending via SNS", new SnsSendException().getMessage());
        assertEquals("SNS max retries exceeded", new SnsSendException.SnsMaxRetriesExceededException().getMessage());
        assertEquals("An error occurred while sending via SES", new SesSendException().getMessage());
        assertEquals("SES max retries exceeded", new SesSendException.SesMaxRetriesExceededException().getMessage());
        assertEquals("Max retries exceeded", new MaxRetriesExceededException().getMessage());
        assertNull(new MaxRetriesExceededException().getCause());
    }

}
