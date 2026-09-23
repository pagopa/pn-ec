package it.pagopa.pn.ec.configurationproperties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PnEcConfig.class)
@EnableConfigurationProperties(PnEcConfig.class)
@TestPropertySource(properties = {
        "pn.ec.dynamo.repository-manager.anagrafica-client-name=pn-EcAnagraficaClient",
        "pn.ec.dynamo.repository-manager.richieste-metadata-name=pn-EcRichiesteMetadata",
        "pn.ec.dynamo.repository-manager.richieste-personal-name=pn-EcRichiestePersonal",
        "pn.ec.dynamo.repository-manager.richieste-conversione-request-name=pn-EcRichiesteConversioneRequest",
        "pn.ec.dynamo.repository-manager.richieste-conversione-pdf-name=pn-EcRichiesteConversionePdf",
        "pn.ec.dynamo.repository-manager.scarti-consolidatore-name=pn-EcScartiConsolidatore"
})
class PnEcConfigDynamoTest {

    @Autowired
    private PnEcConfig pnEcConfig;

    @Test
    void shouldBindRepositoryManagerTableNames() {
        var repositoryManager = pnEcConfig.getDynamo().getRepositoryManager();
        assertThat(repositoryManager.getAnagraficaClientName()).isEqualTo("pn-EcAnagraficaClient");
        assertThat(repositoryManager.getRichiesteMetadataName()).isEqualTo("pn-EcRichiesteMetadata");
        assertThat(repositoryManager.getRichiestePersonalName()).isEqualTo("pn-EcRichiestePersonal");
        assertThat(repositoryManager.getRichiesteConversioneRequestName()).isEqualTo("pn-EcRichiesteConversioneRequest");
        assertThat(repositoryManager.getRichiesteConversionePdfName()).isEqualTo("pn-EcRichiesteConversionePdf");
        assertThat(repositoryManager.getScartiConsolidatoreName()).isEqualTo("pn-EcScartiConsolidatore");
    }
}
