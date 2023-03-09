package it.pagopa.pn.ec.repositorymanager.model.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@Data
@DynamoDbBean
public class PaperRequestPersonal {

    List<PaperEngageRequestAttachments> attachments;
    String receiverName;
    String receiverNameRow2;
    String receiverAddress;
    String receiverAddressRow2;
    String receiverCap;
    String receiverCity;
    String receiverCity2;
    String receiverPr;
    String receiverCountry;
    String receiverFiscalCode;
    String senderName;
    String senderAddress;
    String senderCity;
    String senderPr;
    String senderDigitalAddress;
    String arName;
    String arAddress;
    String arCap;
    String arCity;
}
