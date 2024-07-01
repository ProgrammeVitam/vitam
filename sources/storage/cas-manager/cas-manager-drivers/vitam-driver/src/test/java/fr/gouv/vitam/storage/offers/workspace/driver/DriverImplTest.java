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
package fr.gouv.vitam.storage.offers.workspace.driver;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DriverImplTest extends ResteasyTestApplication {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverImplTest.class);

    protected static final String HOSTNAME = "localhost";
    private static final String DRIVER_NAME = "DefaultOfferDriver";
    private static StorageOffer offer = new StorageOffer();

    protected static final ExpectedResults mock = mock(ExpectedResults.class);

    static TestVitamClientFactory factory = new TestVitamClientFactory(1, "/offer/v1", mock(Client.class));

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        DriverImplTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path("/offer/v1")
    public static class MockResource {

        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullUrlThenRaiseAnException() throws Exception {
        DriverImpl.getInstance().connect(null);
    }

    @Test(expected = StorageDriverException.class)
    public void givenCorrectUrlThenConnectResponseKO() throws Exception {
        try {
            offer.setBaseUrl("http://" + HOSTNAME + ":" + vitamServerTestRunner.getBusinessPort());
            offer.setId("default");
            when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
            try (Driver driver = DriverImpl.getInstance()) {
                driver.addOffer(offer, null);
                try (Connection connection = driver.connect(offer.getId())) {
                    connection.getStorageCapacity(0);
                }
            }
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        }
    }

    @Test
    public void givenCorrectUrlThenConnectResponseNoContent() throws Exception {
        offer.setBaseUrl("http://" + HOSTNAME + ":" + vitamServerTestRunner.getBusinessPort());
        offer.setId("default2");
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        try (Driver driver = DriverImpl.getInstance()) {
            driver.addOffer(offer, null);
            try (Connection connection = driver.connect(offer.getId())) {
                assertNotNull(connection);
            }
        }
    }

    @Test
    public void getNameOK() {
        assertEquals(DRIVER_NAME, DriverImpl.getInstance().getName());
    }

    @Test
    public void isStorageOfferAvailableOK() throws Exception {
        assertEquals(false, DriverImpl.getInstance().isStorageOfferAvailable(null));
    }

    @Test
    public void getMajorVersionOK() throws Exception {
        assertEquals(0, DriverImpl.getInstance().getMajorVersion());
    }

    @Test
    public void getMinorVersionOK() throws Exception {
        assertEquals(0, DriverImpl.getInstance().getMinorVersion());
    }
}
