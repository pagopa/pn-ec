package it.pagopa.pn.ec.pdfraster.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OriginalRequestDto {
    @JsonProperty("iun")
    private String iun;

    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("requestPaId")
    private String requestPaId;

    @JsonProperty("clientRequestTimeStamp")
    private String clientRequestTimeStamp;

    @JsonProperty("productType")
    private String productType;

    @JsonProperty("attachments")
    @Valid
    private List<OriginalRequestAttachmentsDto> attachments = new ArrayList<>();

    @JsonProperty("printType")
    private String printType;

    @JsonProperty("receiverName")
    @lombok.ToString.Exclude
    private String receiverName;

    @JsonProperty("receiverNameRow2")
    @lombok.ToString.Exclude
    private String receiverNameRow2;

    @JsonProperty("receiverAddress")
    @lombok.ToString.Exclude
    private String receiverAddress;

    @JsonProperty("receiverAddressRow2")
    @lombok.ToString.Exclude
    private String receiverAddressRow2;

    @JsonProperty("receiverCap")
    @lombok.ToString.Exclude
    private String receiverCap;

    @JsonProperty("receiverCity")
    @lombok.ToString.Exclude
    private String receiverCity;

    @JsonProperty("receiverCity2")
    @lombok.ToString.Exclude
    private String receiverCity2;

    @JsonProperty("receiverPr")
    @lombok.ToString.Exclude
    private String receiverPr;

    @JsonProperty("receiverCountry")
    @lombok.ToString.Exclude
    private String receiverCountry;

    @JsonProperty("receiverFiscalCode")
    @lombok.ToString.Exclude
    private String receiverFiscalCode;

    @JsonProperty("senderName")
    @lombok.ToString.Exclude
    private String senderName;

    @JsonProperty("senderAddress")
    @lombok.ToString.Exclude
    private String senderAddress;

    @JsonProperty("senderCity")
    @lombok.ToString.Exclude
    private String senderCity;

    @JsonProperty("senderPr")
    @lombok.ToString.Exclude
    private String senderPr;

    @JsonProperty("senderDigitalAddress")
    @lombok.ToString.Exclude
    private String senderDigitalAddress;

    @JsonProperty("arName")
    @lombok.ToString.Exclude
    private String arName;

    @JsonProperty("arAddress")
    @lombok.ToString.Exclude
    private String arAddress;

    @JsonProperty("arCap")
    @lombok.ToString.Exclude
    private String arCap;

    @JsonProperty("arCity")
    @lombok.ToString.Exclude
    private String arCity;

    @JsonProperty("vas")
    @Valid
    private Map<String, String> vas = null;

}
