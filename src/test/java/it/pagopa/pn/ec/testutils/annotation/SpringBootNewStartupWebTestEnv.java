package it.pagopa.pn.ec.testutils.annotation;

import it.pagopa.pn.ec.testutils.localstack.LocalStackNewStartupStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(LocalStackNewStartupStrategy.class)

public @interface SpringBootNewStartupWebTestEnv {
}
