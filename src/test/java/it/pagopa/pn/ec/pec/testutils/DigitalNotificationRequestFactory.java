package it.pagopa.pn.ec.pec.testutils;

import it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.ChannelEnum.PEC;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.MessageContentTypeEnum.PLAIN;
import static it.pagopa.pn.ec.rest.v1.dto.DigitalNotificationRequest.QosEnum.INTERACTIVE;
import static it.pagopa.pn.ec.testutils.constant.EcCommonRestApiConstant.DEFAULT_REQUEST_IDX;

public class DigitalNotificationRequestFactory {

    public static DigitalNotificationRequest createPecRequest(){
        String defaultStringInit = "string";

        List<String> attList = new ArrayList<>();

        attList.add(defaultStringInit);

        Map<String,String> tags = new HashMap<>();

        var digitalNotificationRequestFactory= new DigitalNotificationRequest();
        digitalNotificationRequestFactory.setRequestId(DEFAULT_REQUEST_IDX);
        digitalNotificationRequestFactory.setCorrelationId(defaultStringInit);
        digitalNotificationRequestFactory.setEventType(defaultStringInit);
        digitalNotificationRequestFactory.setQos(INTERACTIVE);
        digitalNotificationRequestFactory.setTags(tags);
        digitalNotificationRequestFactory.setClientRequestTimeStamp(OffsetDateTime.now());
        digitalNotificationRequestFactory.setReceiverDigitalAddress(defaultStringInit);
        digitalNotificationRequestFactory.setMessageText(defaultStringInit);
        digitalNotificationRequestFactory.setSenderDigitalAddress(defaultStringInit);
        digitalNotificationRequestFactory.channel(PEC);
        digitalNotificationRequestFactory.setSubjectText(defaultStringInit);
        digitalNotificationRequestFactory.setMessageContentType(PLAIN);
        digitalNotificationRequestFactory.setAttachmentsUrls(attList);
        return digitalNotificationRequestFactory;
    }
}
