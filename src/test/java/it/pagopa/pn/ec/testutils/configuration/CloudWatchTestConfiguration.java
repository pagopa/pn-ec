package it.pagopa.pn.ec.testutils.configuration;

import lombok.CustomLog;
import org.springframework.boot.test.context.TestConfiguration;
import software.amazon.awssdk.metrics.internal.DefaultSdkMetric;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@TestConfiguration
@CustomLog
public class CloudWatchTestConfiguration {

    @PostConstruct
    public void initCloudWatchPublisher() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Use reflection to get the clearDeclaredMetrics method and invoke it.
        Class<?> cls = DefaultSdkMetric.class;
        Method method = cls.getDeclaredMethod("clearDeclaredMetrics");
        method.setAccessible(true); // Set the method access.
        method.invoke(null); // Invoked the static method.
    }

}
