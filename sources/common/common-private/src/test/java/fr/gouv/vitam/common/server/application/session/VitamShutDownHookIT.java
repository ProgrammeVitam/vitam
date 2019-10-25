/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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

package fr.gouv.vitam.common.server.application.session;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status;

import static org.junit.Assert.fail;

/**
 * Tests for the requestId propagation between servers.
 */
@Ignore("TODO Dette add ShutDownHook to resteasy server")
public class VitamShutDownHookIT {

    private static LocalhostClientFactory server1ClientFactory;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // Configure local client factory
        server1ClientFactory = new LocalhostClientFactory(1, "server1");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        server1ClientFactory.shutdown();
        VitamClientFactory.resetConnections();
    }

    @Test
    public void testLifeCycleShutdown() throws Exception {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        try (LocalhostClientFactory.LocalhostClient client = server1ClientFactory.getClient()) {
            headers.add(GlobalDataRest.X_REQUEST_ID, "header-1");
            Assert.assertEquals(Status.OK.getStatusCode(),
                client.doRequestAndGetStatus("testReturnImmediately", headers));

            Thread thread = VitamThreadFactory.getInstance().newThread(() -> {
                try {
                    headers.putSingle(GlobalDataRest.X_REQUEST_ID, "header-2");
                    int response = client.doRequestAndGetStatus("testWaitFiveSecond", headers);
                    Assert.assertEquals(Status.OK.getStatusCode(), response);
                } catch (Exception e) {
                    fail("should not fail");
                }
            });
            Thread thread2 = VitamThreadFactory.getInstance().newThread(() -> {
                try {
                    // TODO: 09/01/19 STOP SERVER
                } catch (Exception e) {
                    fail("should not fail");
                }
            });
            thread.start();
            Thread.sleep(500);
            thread2.start();
            // Hack over hack
            Thread.sleep(500);
            Assert.assertEquals(Status.GONE.getStatusCode(),
                client.doRequestAndGetStatus("testReturnImmediately", headers));
            thread.join();
            thread2.join();
        }
    }

}
