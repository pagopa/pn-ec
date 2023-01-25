
package it.pagopa.pn.template.notificationtracker.service;


import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.*;

import java.util.ArrayList;
import java.util.List;

public class PutEvents {

//    EventBridgeClient eventBrClient = EventBridgeClient.builder().build();

    public static void putEBEvents(EventBridgeClient eventBrClient, String resourceArn ) {

        try {
            // Populate a List with the resource ARN values.
            List<String> resources = new ArrayList<>();
            resources.add(resourceArn);

            PutEventsRequestEntry reqEntry = PutEventsRequestEntry.builder()
                .resources(resources)
                .source("aws.sms")
                .detailType("Object Created")
                .detail("{ \"key1\": \"value1\", \"key2\": \"value2\" }")
                .build();

            PutEventsRequest eventsRequest = PutEventsRequest.builder()
                .entries(reqEntry)
                .build();

            PutEventsResponse result = eventBrClient.putEvents(eventsRequest);
            for (PutEventsResultEntry resultEntry : result.entries()) {
                if (resultEntry.eventId() != null) {
                    System.out.println("Event Id: " + resultEntry.eventId());
                } else {
                    System.out.println("Injection failed with Error Code: " + resultEntry.errorCode());
                }
            }

        } catch (EventBridgeException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
