package it.pagopa.pn.ec.repositorymanager.rest;

import it.pagopa.pn.ec.rest.v1.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientConfigurationControllerTest {

    @Autowired
    private WebTestClient webClient;

    private static final String BASE_PATH_WITH_PARAM = "/gestoreRepository/clients/{xPagopaExtchCxId}";
    private static final String BASE_PATH = "/gestoreRepository/clients";

    private static final String DEFAULT_ID = "AAA";
    private static ClientConfigurationDto clientConfigurationDto;

    @BeforeEach
    public void createClientConfigurationDto() {
        clientConfigurationDto = new ClientConfigurationDto();
        clientConfigurationDto.setxPagopaExtchCxId(DEFAULT_ID);
        clientConfigurationDto.setSqsArn("");
        clientConfigurationDto.setSqsName("");
    }

    //test.100.1
    @Test
    @Order(1)
    void insertClientTestSuccess() {
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
    @Order(2)
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
    @Order(3)
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
    @Order(4)
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
    @Order(5)
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
    @Order(6)
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
    @Order(7)
    void deleteClientTestSuccess() {
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build(DEFAULT_ID))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.103.2
    @Test
    @Order(8)
    void deleteClientTestFailed() {
        webClient.delete()
                 .uri(uriBuilder -> uriBuilder.path(BASE_PATH_WITH_PARAM).build("idNonPresente"))
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }
}
