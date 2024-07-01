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
package fr.gouv.vitam.processing.management.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class ProcessManagementMainTest {

    private ProcessManagementMain application;
    private static JunitHelper junitHelper;
    private static int port;
    private static final String CONF = "processing.conf";

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8084);

    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Before
    public void setUp() throws Exception {
        instanceRule.stubFor(
            WireMock.get(urlMatching("/workspace/v1/containers/process/folders/1")).willReturn(
                aResponse()
                    .withStatus(200)
                    .withBody(JsonHandler.createArrayNode().toString())
                    .withHeader(GlobalDataRest.X_TENANT_ID, Integer.toString(1))
                    .withHeader("Content-Type", "application/json")
            )
        );
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
    }

    @AfterClass
    public static void shutdownAfterClass() {
        junitHelper.releasePort(port);
        VitamClientFactory.resetConnections();
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyArgsWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application = new ProcessManagementMain((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyArgsConfWhenConfigureApplicationOThenRaiseAnException() throws Exception {
        application = new ProcessManagementMain(null);
    }

    @Test(expected = IllegalStateException.class)
    public void givenFileNotFoundWhenConfigureApplicationThenRaiseAnException() throws Exception {
        application = new ProcessManagementMain("notFound.conf");
    }

    @Test
    public void givenFileExistsWhenConfigureApplicationThenRunServer() throws Exception {
        application = new ProcessManagementMain(CONF);
    }

    @Test
    public void givenFileExistsWhenStartupApplicationThenRunServer() throws Exception {
        SystemPropertyUtil.set(ProcessManagementMain.PARAMETER_JETTY_SERVER_PORT, Integer.toString(port));
        application = new ProcessManagementMain(CONF);
        application.start();
        application.stop();
    }
}
