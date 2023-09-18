package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class PaperRequestPersonal {

    @ToString.Exclude
    String receiverName;
    @ToString.Exclude
    String receiverNameRow2;
    @ToString.Exclude
    String receiverAddress;
    @ToString.Exclude
    String receiverAddressRow2;
    @ToString.Exclude
    String receiverCap;
    @ToString.Exclude
    String receiverCity;
    @ToString.Exclude
    String receiverCity2;
    @ToString.Exclude
    String receiverPr;
    @ToString.Exclude
    String receiverCountry;
    @ToString.Exclude
    String receiverFiscalCode;
    @ToString.Exclude
    String senderName;
    @ToString.Exclude
    String senderAddress;
    @ToString.Exclude
    String senderCity;
    @ToString.Exclude
    String senderPr;
    @ToString.Exclude
    String senderDigitalAddress;
    @ToString.Exclude
    String arName;
    @ToString.Exclude
    String arAddress;
    @ToString.Exclude
    String arCap;
    @ToString.Exclude
    String arCity;
    List<PaperEngageRequestAttachments> attachments;
}
