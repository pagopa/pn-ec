package it.pagopa.pn.ec.repositorymanager.configurationproperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dynamo.table.repository-manager")
public record RepositoryManagerDynamoTableName(String anagraficaClientArn, String anagraficaClientName, String richiesteArn,
                                               String richiesteName) {}
