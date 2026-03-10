package it.pagopa.pn.ec.availabilitymanager.model.dto;

import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ToString
public class AvailabilityManagerDetailDto {

    private String key;
    private String versionId;
    private String documentType;
    private String documentStatus;
    private String contentType;
    private String checksum;
    private LocalDateTime retentionUntil;
    private Map<String, List<String>> tags;
    private String clientShortCode;

    public AvailabilityManagerDetailDto() {
    }

    public AvailabilityManagerDetailDto(String checksum) {
        this.checksum = checksum;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(String documentStatus) {
        this.documentStatus = documentStatus;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LocalDateTime getRetentionUntil() {
        return retentionUntil;
    }

    public void setRetentionUntil(LocalDateTime retentionUntil) {
        this.retentionUntil = retentionUntil;
    }

    public Map<String, List<String>> getTags() {
        return tags;
    }

    public void setTags(Map<String, List<String>> tags) {
        this.tags = tags;
    }

    public String getClientShortCode() {
        return clientShortCode;
    }

    public void setClientShortCode(String clientShortCode) {
        this.clientShortCode = clientShortCode;
    }
}
