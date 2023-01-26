package it.pagopa.pn.ec.notificationtracker.rest.util;

public class StateMachineUtils {

    private StateMachineUtils() {
        throw new IllegalStateException("EndpointUtils is a utility class");
    }

    public static final String STATI_MACCHINA_ENDPOINT = "/statemachinemanager/validate/";

    public static String getStatiMacchinaEndpoint() {
        return String.format(STATI_MACCHINA_ENDPOINT);
    }
}
