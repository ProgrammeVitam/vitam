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
package fr.gouv.vitam.access.internal.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * AccessResourceTest class
 */
public class AccessResourceTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessResourceTest.class);
    private static final String ACCESS_RESOURCE_URI = "access/v1";

    private static final String ACCESS_CONF = "access-test.conf";
    private static AccessInternalMain application;
    private static final String ID = "identifier8";

    private static JunitHelper junitHelper;
    private static int serverPort;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();
        application = new AccessInternalMain(ACCESS_CONF);
        application.start();
        RestAssured.port = serverPort;
        RestAssured.basePath = ACCESS_RESOURCE_URI;

        LOGGER.debug("Beginning tests");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        LOGGER.debug("Ending tests");
        try {
            application.stop();
            junitHelper.releasePort(serverPort);
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
        }
        VitamClientFactory.resetConnections();
    }

    @Test
    public final void test() throws Exception {
        final AccessInternalClient client = AccessInternalClientFactory.getInstance().getClient();
        final String selectQuery =
            "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
        final JsonNode queryJson = JsonHandler.getFromString(selectQuery);
        assertNotNull(client.selectUnits(queryJson));
        assertNotNull(client.selectUnitbyId(queryJson, ID));
    }
}
