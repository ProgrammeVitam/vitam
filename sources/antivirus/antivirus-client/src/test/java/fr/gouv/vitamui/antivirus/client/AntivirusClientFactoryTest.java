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

package fr.gouv.vitamui.antivirus.client;

import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AntivirusClientFactory}.
 */
public class AntivirusClientFactoryTest {

    @Test
    public void testConstructorWithConfiguration() {
        SecureClientConfiguration configuration = new SecureClientConfigurationImpl("localhost", 8080, false);
        AntivirusClientFactory factory = new AntivirusClientFactory(configuration);
        assertThat(factory).isNotNull();
        assertThat(factory.getAntivirusApi()).isNotNull();
        assertThat(factory.getApiClient().getBasePath()).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testGetInstance() {
        AntivirusClientFactory instance = AntivirusClientFactory.getInstance();
        assertThat(instance).isNotNull();
        assertThat(instance).isSameAs(AntivirusClientFactory.getInstance());
    }

    @Test
    public void testChangeModeWithConfiguration() {
        SecureClientConfiguration configuration = new SecureClientConfigurationImpl("localhost", 8080, false);
        AntivirusClientFactory.changeMode(configuration);
        AntivirusClientFactory instance = AntivirusClientFactory.getInstance();
        assertThat(instance).isNotNull();
        assertThat(instance.getAntivirusApi()).isNotNull();
        assertThat(instance.getApiClient().getBasePath()).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testChangeServerPort() {
        SecureClientConfiguration configuration = new SecureClientConfigurationImpl("localhost", 8080, false);
        AntivirusClientFactory factory = new AntivirusClientFactory(configuration);
        assertThat(factory.getApiClient().getBasePath()).isEqualTo("http://localhost:8080");
        factory.changeServerPort(9090);
        assertThat(factory).isNotNull();
        assertThat(factory.getAntivirusApi()).isNotNull();
        assertThat(factory.getApiClient().getBasePath()).isEqualTo("http://localhost:9090");
    }

    @Test
    public void testInitialisationWithNullConfiguration() {
        AntivirusClientFactory factory = new AntivirusClientFactory(null);
        assertThat(factory).isNotNull();
    }
}
