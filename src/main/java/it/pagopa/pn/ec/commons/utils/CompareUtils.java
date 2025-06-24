package it.pagopa.pn.ec.commons.utils;

import it.pagopa.pn.ec.rest.v1.dto.*;

import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.SECONDS;

public class CompareUtils {

    private CompareUtils() {
        throw new IllegalStateException("CompareUtils is utility class");
    }

    public static boolean isSameEvent(List<EventsDto> lastEvents, DigitalProgressStatusDto newEvent, String nextStatus) {
        return lastEvents.stream().map(EventsDto::getDigProgrStatus).anyMatch(lastEvent -> lastEvent.getEventTimestamp().equals(newEvent.getEventTimestamp().truncatedTo(SECONDS))
                && lastEvent.getStatus().equals(nextStatus) && lastEvent.getGeneratedMessage() != null
                && lastEvent.getGeneratedMessage().getId().equals(newEvent.getGeneratedMessage().getId())
                && lastEvent.getGeneratedMessage().getSystem().equals(newEvent.getGeneratedMessage().getSystem()));
    }

    public static boolean isSameEvent(PaperProgressStatusDto lastEvent, PaperProgressStatusDto newEvent, String nextStatus) {
        return lastEvent.getStatus().equals(nextStatus) && lastEvent.getStatusDateTime().equals(newEvent.getStatusDateTime().truncatedTo(SECONDS));
    }

    public static boolean isSameEvent(PaperProgressStatusDto paperProgressStatusEvent, ConsolidatoreIngressPaperProgressStatusEvent consolidatoreIngressPaperProgressStatusEvent) {
       return  Objects.equals(paperProgressStatusEvent.getRegisteredLetterCode(),consolidatoreIngressPaperProgressStatusEvent.getRegisteredLetterCode())
                && Objects.equals(paperProgressStatusEvent.getProductType(), consolidatoreIngressPaperProgressStatusEvent.getProductType())
                && Objects.equals(paperProgressStatusEvent.getIun(), consolidatoreIngressPaperProgressStatusEvent.getIun())
                && Objects.equals(paperProgressStatusEvent.getStatusCode(), consolidatoreIngressPaperProgressStatusEvent.getStatusCode())
                && Objects.equals(paperProgressStatusEvent.getStatusDescription(), consolidatoreIngressPaperProgressStatusEvent.getStatusDescription())
                && paperProgressStatusEvent.getStatusDateTime().truncatedTo(SECONDS).isEqual(consolidatoreIngressPaperProgressStatusEvent.getStatusDateTime().truncatedTo(SECONDS))
                && Objects.equals(paperProgressStatusEvent.getDeliveryFailureCause(), consolidatoreIngressPaperProgressStatusEvent.getDeliveryFailureCause())
                && isSameAttachments(paperProgressStatusEvent.getAttachments(), consolidatoreIngressPaperProgressStatusEvent.getAttachments())
                && isSameAddress(paperProgressStatusEvent.getDiscoveredAddress(), consolidatoreIngressPaperProgressStatusEvent.getDiscoveredAddress());

    }

    private static boolean isSameAttachments(List<AttachmentsProgressEventDto> paperProgressAttachmentList, List<ConsolidatoreIngressPaperProgressStatusEventAttachments> consolidatoreIngressPaperProgressStatusEventAttachmentsList) {

        if(paperProgressAttachmentList == null) {
            paperProgressAttachmentList = List.of();
        }

        if(consolidatoreIngressPaperProgressStatusEventAttachmentsList == null) {
            consolidatoreIngressPaperProgressStatusEventAttachmentsList = List.of();
        }

        if(consolidatoreIngressPaperProgressStatusEventAttachmentsList.isEmpty() && paperProgressAttachmentList.isEmpty()) {
            return true;
        }

        if (paperProgressAttachmentList.isEmpty() || consolidatoreIngressPaperProgressStatusEventAttachmentsList.isEmpty()) {
            return false;
        }

        if (paperProgressAttachmentList.size() != consolidatoreIngressPaperProgressStatusEventAttachmentsList.size()){
            return false;
        } else {

            for (int i = 0; i<paperProgressAttachmentList.size(); i++) {
                AttachmentsProgressEventDto paperProgress = paperProgressAttachmentList.get(i);
                ConsolidatoreIngressPaperProgressStatusEventAttachments consolidatoreAttachment = consolidatoreIngressPaperProgressStatusEventAttachmentsList.get(i);

                if(! (Objects.equals(paperProgress.getDate(), consolidatoreAttachment.getDate()) &&
                        Objects.equals(paperProgress.getId(), consolidatoreAttachment.getId()) &&
                        Objects.equals(paperProgress.getDocumentType(), consolidatoreAttachment.getDocumentType()) &&
                        Objects.equals(paperProgress.getSha256(), consolidatoreAttachment.getSha256()))) {
                    return false;
                }
            } return true;
        }
    }

    private static boolean isSameAddress (DiscoveredAddressDto discoveredAddress, ConsolidatoreIngressPaperProgressStatusEventDiscoveredAddress consolidatoreIngressPaperProgressStatusEventDiscoveredAddress) {
        if (discoveredAddress == null && consolidatoreIngressPaperProgressStatusEventDiscoveredAddress == null) {
            return true;
        }
        if (discoveredAddress == null || consolidatoreIngressPaperProgressStatusEventDiscoveredAddress==null) {
            return false;
        }
        return  Objects.equals(discoveredAddress.getName(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getName())
                && Objects.equals(discoveredAddress.getNameRow2(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getNameRow2())
                && Objects.equals(discoveredAddress.getAddress(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getAddress())
                && Objects.equals(discoveredAddress.getAddressRow2(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getAddressRow2())
                && Objects.equals(discoveredAddress.getCap(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getCap())
                && Objects.equals(discoveredAddress.getCity(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getCity())
                && Objects.equals(discoveredAddress.getCity2(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getCity2())
                && Objects.equals(discoveredAddress.getPr(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getPr())
                && Objects.equals(discoveredAddress.getCountry(), consolidatoreIngressPaperProgressStatusEventDiscoveredAddress.getCountry());
    }
}
