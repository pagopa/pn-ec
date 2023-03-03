package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.GestoreRepositoryEndpointProperties;
import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.model.entity.ClientConfiguration;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class ClientConfigurationControllerTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private GestoreRepositoryEndpointProperties gestoreRepositoryEndpointProperties;

    private static final String DEFAULT_ID = "AAA";
    private static ClientConfigurationDto clientConfigurationDto;

    private static DynamoDbTable<ClientConfiguration> dynamoDbTable;

    private static void insertClientConfiguration(String cxId) {
        var clientConfiguration = new ClientConfiguration();
        clientConfiguration.setCxId(cxId);
        dynamoDbTable.putItem(builder -> builder.item(clientConfiguration));
    }

    @BeforeAll
    public static void insertDefaultClientConfiguration(@Autowired DynamoDbEnhancedClient dynamoDbTestEnhancedClient,
                                                        @Autowired RepositoryManagerDynamoTableName gestoreRepositoryDynamoDbTableName) {
        dynamoDbTable = dynamoDbTestEnhancedClient.table(gestoreRepositoryDynamoDbTableName.anagraficaClientName(),
                                                         TableSchema.fromBean(ClientConfiguration.class));
        insertClientConfiguration(DEFAULT_ID);
    }

    @BeforeEach
    public void createClientConfigurationDto() {
        clientConfigurationDto = new ClientConfigurationDto();
        clientConfigurationDto.setxPagopaExtchCxId(DEFAULT_ID);
        clientConfigurationDto.setSqsArn("");
        clientConfigurationDto.setSqsName("");
    }

    //test.100.1
    @Test
    void insertClientTestSuccess() {
        clientConfigurationDto.setxPagopaExtchCxId("newId");
        webClient.post()
                 .uri(gestoreRepositoryEndpointProperties.postClientConfiguration())
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(clientConfigurationDto))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.100.2
    @Test
    void insertClientTestFailed() {
        webClient.post()
                 .uri(gestoreRepositoryEndpointProperties.postClientConfiguration())
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(clientConfigurationDto))
                 .exchange()
                 .expectStatus()
                 .isEqualTo(HttpStatus.CONFLICT);
    }

    //test.101.1
    @Test
    void getClientTestSuccess() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getClientConfiguration()).build(DEFAULT_ID))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isOk()
                 .expectBody(ClientConfigurationDto.class);
    }

    //test.101.2
    @Test
    void getClientTestFailed() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.getClientConfiguration()).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    //test.102.1
    @Test
    void testUpdateSuccess() {
        webClient.put()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.putClientConfiguration()).build(DEFAULT_ID))
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(clientConfigurationDto))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.102.2
    @Test
    void testUpdateFailed() {
        webClient.put()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.putClientConfiguration()).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(clientConfigurationDto))
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }

    //test.103.1
    @Test
    void deleteClientTestSuccess() {
        String cxId = "idToDelete";
        insertClientConfiguration(cxId);
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.deleteClientConfiguration()).build(cxId))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isEqualTo(HttpStatus.NO_CONTENT);
    }

    //test.103.2
    @Test
    void deleteClientTestFailed() {
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(gestoreRepositoryEndpointProperties.deleteClientConfiguration()).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isNotFound();
    }
}
