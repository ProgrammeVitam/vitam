/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyClientFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InaTapeProxyClientFactoryTest {

    @Test
    public void testConstructorWithConfiguration() {
        SecureClientConfiguration configuration = new SecureClientConfigurationImpl("localhost", 8080, false);
        InaTapeProxyClientFactory factory = new InaTapeProxyClientFactory(configuration);
        assertThat(factory).isNotNull();
        assertThat(factory.getInaTapeProxyClient()).isNotNull();
        assertThat(factory.getInaTapeProxyClient().getApiClient().getBasePath()).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testGetInstance() {
        InaTapeProxyClientFactory instance = InaTapeProxyClientFactory.getInstance();
        assertThat(instance).isNotNull();
        assertThat(instance).isSameAs(InaTapeProxyClientFactory.getInstance());
    }

    @Test
    public void testChangeModeWithConfiguration() {
        SecureClientConfiguration configuration = new SecureClientConfigurationImpl("localhost", 8080, false);
        InaTapeProxyClientFactory.changeMode(configuration);
        InaTapeProxyClientFactory instance = InaTapeProxyClientFactory.getInstance();
        assertThat(instance).isNotNull();
        assertThat(instance.getInaTapeProxyClient()).isNotNull();
        assertThat(instance.getInaTapeProxyClient().getApiClient().getBasePath()).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testChangeServerPort() {
        SecureClientConfiguration configuration = new SecureClientConfigurationImpl("localhost", 8080, false);
        InaTapeProxyClientFactory factory = new InaTapeProxyClientFactory(configuration);
        assertThat(factory.getInaTapeProxyClient().getApiClient().getBasePath()).isEqualTo("http://localhost:8080");
        factory.changeServerPort(9090);
        assertThat(factory).isNotNull();
        assertThat(factory.getInaTapeProxyClient()).isNotNull();
        assertThat(factory.getInaTapeProxyClient().getApiClient().getBasePath()).isEqualTo("http://localhost:9090");
    }

    @Test
    public void testInitialisationWithNullConfiguration() {
        InaTapeProxyClientFactory factory = new InaTapeProxyClientFactory(null);
        assertThat(factory).isNotNull();
    }
}
