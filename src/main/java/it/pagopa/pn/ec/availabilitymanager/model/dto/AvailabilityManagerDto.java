package it.pagopa.pn.ec.availabilitymanager.model.dto;

import java.time.LocalDateTime;

public class AvailabilityManagerDto {
    private String version;
    private String id;
    private String detailType;
    private String source;
    private String account;
    private LocalDateTime time;
    private String region;
    private AvailabilityManagerDetailDto detail;

    public AvailabilityManagerDto() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDetailType() {
        return detailType;
    }

    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public AvailabilityManagerDetailDto getDetail() {
        return detail;
    }

    public void setDetail(AvailabilityManagerDetailDto detail) {
        this.detail = detail;
    }
}
