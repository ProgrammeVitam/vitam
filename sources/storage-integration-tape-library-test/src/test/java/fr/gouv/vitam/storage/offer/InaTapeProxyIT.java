/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.offer;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyApi;
import fr.gouv.vitam.storage.cold.client.InaTapeProxyClientFactory;
import fr.gouv.vitam.storage.cold.server.InaTapeProxyLauncher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWithCustomExecutor
public class InaTapeProxyIT {

    static final String CONFIGURATION_DIRECTORY = "ina-tape-proxy";

    static final String SERVER_CONFIGURATION_FILENAME = "ina-tape-proxy-web.conf";
    static final String SERVER_CONFIGURATION_PATH = String.join(
        "/",
        CONFIGURATION_DIRECTORY,
        SERVER_CONFIGURATION_FILENAME
    );

    static final String CLIENT_CONFIGURATION_FILENAME = "ina-tape-proxy-client.conf";
    static final String CLIENT_CONFIGURATION_PATH = String.join(
        "/",
        CONFIGURATION_DIRECTORY,
        CLIENT_CONFIGURATION_FILENAME
    );

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @BeforeClass
    public static void setup() throws FileNotFoundException {
        final String configurationDirectory = PropertiesUtils.getResourceFile(
            CONFIGURATION_DIRECTORY
        ).getAbsolutePath();
        SystemPropertyUtil.set("vitam.conf.folder", configurationDirectory);
        VitamConfiguration.getConfiguration().setConfig(configurationDirectory);

        final File SERVER_CONFIGURATION_FILE = PropertiesUtils.getResourceFile(SERVER_CONFIGURATION_PATH);
        final String[] args = { SERVER_CONFIGURATION_FILE.getAbsolutePath() };

        InaTapeProxyLauncher.start(args);
    }

    @AfterClass
    public static void tearDown() {
        InaTapeProxyLauncher.stop();
    }

    @Before
    public void init() {
        VitamThreadUtils.getVitamSession().setTenantId(0);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newGUID());
    }

    @Test
    public void shouldCallColdStorageProxy() throws IOException {
        InaTapeProxyApi client = getColdStorageClient();

        assertThatThrownBy(client::getDriveStatus)
            .isInstanceOf(fr.gouv.vitam.storage.cold.client.invoker.ApiException.class)
            .satisfies(
                ex -> assertThat(((fr.gouv.vitam.storage.cold.client.invoker.ApiException) ex).getCode()).isEqualTo(500)
            );
    }

    private InaTapeProxyApi getColdStorageClient() throws IOException {
        File configurationFile = PropertiesUtils.getResourceFile(CLIENT_CONFIGURATION_PATH);
        ClientConfiguration configuration = PropertiesUtils.readYaml(
            configurationFile,
            SecureClientConfigurationImpl.class
        );
        return new InaTapeProxyClientFactory(configuration).getInaTapeProxyClient();
    }
}
