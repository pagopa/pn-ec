package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.repositorymanager.configurationproperties.RepositoryManagerDynamoTableName;
import it.pagopa.pn.ec.repositorymanager.entity.ClientConfiguration;
import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import it.pagopa.pn.ec.testutils.configuration.DynamoTestConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@Import(DynamoTestConfiguration.class)
class ClientConfigurationControllerTest {

    @Autowired
    private WebTestClient webClient;

    private static final String BASE_PATH = "/gestoreRepository/clients";
    private static final String BASE_PATH_WITH_PARAM = String.format("%s/{xPagopaExtchCxId}", BASE_PATH);

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
                 .uri(BASE_PATH)
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
                 .uri(BASE_PATH)
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(clientConfigurationDto))
                 .exchange()
                 .expectStatus()
                 .isForbidden();
    }

    //test.101.1
    @Test
    void getClientTestSuccess() {
        webClient.get()
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(DEFAULT_ID))
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
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }

    //test.102.1
    @Test
    void testUpdateSuccess() {
        webClient.put()
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(DEFAULT_ID))
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
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(clientConfigurationDto))
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }

    //test.103.1
    @Test
    void deleteClientTestSuccess() {
        String cxId = "idToDelete";
        insertClientConfiguration(cxId);
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(cxId))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.103.2
    @Test
    void deleteClientTestFailed() {
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }
}
