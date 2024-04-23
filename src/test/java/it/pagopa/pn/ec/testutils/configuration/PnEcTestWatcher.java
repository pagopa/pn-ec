package it.pagopa.pn.ec.testutils.configuration;

import lombok.CustomLog;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import software.amazon.awssdk.metrics.internal.DefaultSdkMetric;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * PnEcTestWatcher is a custom implementation of the TestWatcher interface.
 * It is used to take actions before and after a test is executed.
 */
@CustomLog
public class PnEcTestWatcher implements TestWatcher {

    /**
     * This method is invoked after a test has been skipped due to being disabled.
     *
     * @param context provides information about the current test execution
     * @param reason  provides the reason why the test was disabled
     */
    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        clearDeclaredMetrics();
    }

    /**
     * This method is invoked after a test has completed successfully.
     *
     * @param context provides information about the current test execution
     */
    @Override
    public void testSuccessful(ExtensionContext context) {
        clearDeclaredMetrics();
    }

    /**
     * This method is invoked after a test has been aborted.
     *
     * @param context provides information about the current test execution
     * @param cause   provides the exception that caused the test to be aborted
     */
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        clearDeclaredMetrics();
    }

    /**
     * This method is invoked after a test has failed.
     *
     * @param context provides information about the current test execution
     * @param cause   provides the exception that caused the test to fail
     */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        clearDeclaredMetrics();
    }

    /**
     * This method clears the declared metrics.
     * It uses reflection to access the clearDeclaredMetrics method of the DefaultSdkMetric class.
     * If an error occurs during the process, it logs the error and throws a RuntimeException.
     */
    public void clearDeclaredMetrics() {
        try {
            // Use reflection to get the clearDeclaredMetrics method and invoke it.
            Class<?> cls = DefaultSdkMetric.class;
            Method method = cls.getDeclaredMethod("clearDeclaredMetrics");
            method.setAccessible(true); // Set the method access.
            method.invoke(null); // Invoked the static method.
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("Error while clearing declared metrics", e);
            throw new RuntimeException(e);
        }
    }
}