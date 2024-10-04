package it.pagopa.pn.ec.commons.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "transaction.process")
public record TransactionProcessConfigurationProperties(String startStatus, String sms, String smsStartStatus, String email,
                                                        String emailStartStatus, String pec, String pecStartStatus, String paper,
                                                        String paperStarterStatus, String sercq, String sercqStartStatus) {}
