package it.pagopa.pn.ec.commons.rest;

import it.pagopa.pn.ec.commons.configurationproperties.endpoint.internal.ec.ExternalChannelEndpointProperties;
import it.pagopa.pn.ec.testutils.annotation.SpringBootTestWebEnv;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTestWebEnv
@AutoConfigureWebTestClient
class ApiControllerTest {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ExternalChannelEndpointProperties extChannelEndpoint;

	@Test
	void getStatusTest() {
		webClient.get().uri(extChannelEndpoint.containerBaseUrl() + "/").accept(APPLICATION_JSON).exchange()
				.expectStatus().isOk();
	}
}
