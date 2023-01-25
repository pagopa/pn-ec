package it.pagopa.pn.ec.repositorymanager;

import it.pagopa.pn.ec.repositorymanager.dto.ClientConfigurationDto;
import it.pagopa.pn.ec.repositorymanager.dto.SenderPhysicalAddressDto;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientConfigurationTest {

    @Autowired
    private WebTestClient webClient;

    //test.100.1
    @Test
    @Order(1)
    void insertClientTestSuccess() {

        ClientConfigurationDto ccDtoI = new ClientConfigurationDto();
        SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();

        spaDto.setName("Mario");
        spaDto.setAddress("Via s nome 1");
        spaDto.setCap("00123");
        spaDto.setCity("Pisa");
        spaDto.setPr("PI");

        ccDtoI.setCxId("AAA");
        ccDtoI.setSqsArn("ABC");
        ccDtoI.setSqsName("MARIO ROSSI");
        ccDtoI.setPecReplyTo("mariorossi@pec.it");
        ccDtoI.setMailReplyTo("mariorossi@yahoo.it");

        ccDtoI.setSenderPhysicalAddress(spaDto);

        webClient.post()
                 .uri("http://localhost:8080/clients")
                 .accept(APPLICATION_JSON)
                 .contentType(APPLICATION_JSON)
                 .body(BodyInserters.fromValue(ccDtoI))
                 .exchange()
                 .expectStatus()
                 .isOk();
    }

    //test.100.2
    @Test
    @Order(2)
    void insertClientTestFailed() {

        ClientConfigurationDto ccDtoI = new ClientConfigurationDto();
        SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();

        spaDto.setName("Mirko");
        spaDto.setAddress("Via s nome 2");
        spaDto.setCap("00124");
        spaDto.setCity("Pisa");
        spaDto.setPr("PI");

        ccDtoI.setCxId("AAA");
        ccDtoI.setSqsArn("ABC");
        ccDtoI.setSqsName("MIRKO ROSSI");
        ccDtoI.setPecReplyTo("mirkorossi@pec.it");
        ccDtoI.setMailReplyTo("mirkorossi@yahoo.it");

        ccDtoI.setSenderPhysicalAddress(spaDto);

        webClient.post()
                .uri("http://localhost:8080/clients")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(ccDtoI))
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    //test.101.1
    @Test
    @Order(3)
    void getClientTestSuccess() {
        webClient.get()
                 .uri("http://localhost:8080/clients/AAA")
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
                 .uri("http://localhost:8080/clients/abab")
                 .accept(APPLICATION_JSON)
                 .exchange()
                 .expectStatus()
                 .isBadRequest();
    }

	//test.102.1
	@Test
    @Order(5)
	void testUpdateSuccess() {
        ClientConfigurationDto ccDtoI = new ClientConfigurationDto();
        SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();

        spaDto.setName("Ciro");
        spaDto.setAddress("Viale senza nome 1");
        spaDto.setCap("00555");
        spaDto.setCity("Cuneo");
        spaDto.setPr("CU");

        ccDtoI.setSqsArn("DEF");
        ccDtoI.setSqsName("Ciro ROSSI");
        ccDtoI.setPecReplyTo("Cirorossi@pec.it");
        ccDtoI.setMailReplyTo("Cirorossi@yahoo.it");

        ccDtoI.setSenderPhysicalAddress(spaDto);

        webClient.put()
                .uri("http://localhost:8080/clients/AAA")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(ccDtoI))
                .exchange()
                .expectStatus()
                .isOk();
	}

	//test.102.2
	@Test
    @Order(6)
	void testUpdateFailed() {
		ClientConfigurationDto ccDtoI = new ClientConfigurationDto();
        SenderPhysicalAddressDto spaDto = new SenderPhysicalAddressDto();

        spaDto.setName("Ciro");
        spaDto.setAddress("Via s nome 1");
        spaDto.setCap("00555");
        spaDto.setCity("Pisa");
        spaDto.setPr("Pi");

        ccDtoI.setSqsArn("DEF");
        ccDtoI.setSqsName("Ciro ROSSI");
        ccDtoI.setPecReplyTo("Cirorossi@pec.it");
        ccDtoI.setMailReplyTo("Cirorossi@yahoo.it");

        ccDtoI.setSenderPhysicalAddress(spaDto);

        webClient.put()
                .uri("http://localhost:8080/clients/www")
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_JSON)
                .body(BodyInserters.fromValue(ccDtoI))
                .exchange()
                .expectStatus()
                .isBadRequest();
	}

	//test.103.1
	@Test
    @Order(7)
	void deleteClientTestSuccess() {
        webClient.delete()
                .uri("http://localhost:8080/clients/AAA")
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
                .uri("http://localhost:8080/clients/abab")
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest();
	}

}
