/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.ingest.external.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client2.AbstractMockClient;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;

@SuppressWarnings("rawtypes")
public class IngestExternalClientRestTest extends VitamJerseyTest {

    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/ingest-ext/v1";
    protected IngestExternalClientRest client;
    private static final String MOCK_INPUTSTREAM_CONTENT = "VITAM-Ingest External Client Rest Mock InputStream";
    private static final String FAKE_X_REQUEST_ID = GUIDFactory.newRequestIdGUID(0).getId();
    private static final String MOCK_RESPONSE_STREAM = "VITAM-Ingest External Client Rest Mock Response";


    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    @SuppressWarnings("unchecked")
    public IngestExternalClientRestTest() {
        super(IngestExternalClientFactory.getInstance());
    }

    @Override
    public void beforeTest() {
        client = (IngestExternalClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse startVitamApplication(int reservedPort) throws IllegalStateException {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        return new StartApplicationResponse<AbstractApplication>()
            .setServerPort(application.getVitamServer().getPort())
            .setApplication(application);
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new MockResource(mock));
        }
    }

    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

    @Path("/ingest-ext/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("upload")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_XML)
        public Response upload(InputStream stream) {
            return expectedResponse.post();
        }
    }

    @Test
    public void givenInputstreamWhenUploadThenReturnOK()
        throws IngestExternalException, XMLStreamException, IOException {

        InputStream mockResponseInputStream = IOUtils.toInputStream(MOCK_RESPONSE_STREAM);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, FAKE_X_REQUEST_ID);

        Response fakeResponse = new AbstractMockClient.FakeInboundResponse(Status.OK,
            mockResponseInputStream,
            MediaType.APPLICATION_OCTET_STREAM_TYPE, headers);
        when(mock.post()).thenReturn(fakeResponse);


        final InputStream streamToUpload = IOUtils.toInputStream(MOCK_INPUTSTREAM_CONTENT);
        InputStream fakeUploadResponseInputStream = client.upload(streamToUpload).readEntity(InputStream.class);
        assertNotNull(fakeUploadResponseInputStream);

        try {
            assertTrue(IOUtils.contentEquals(fakeUploadResponseInputStream,
                IOUtils.toInputStream(MOCK_RESPONSE_STREAM)));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void givenErrorWhenUploadThenReturnBadRequestErrorWithBody() throws Exception {
        InputStream mockResponseInputStream = IOUtils.toInputStream(MOCK_RESPONSE_STREAM);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_REQUEST_ID, FAKE_X_REQUEST_ID);

        Response fakeResponse = new AbstractMockClient.FakeInboundResponse(Status.BAD_REQUEST,
            mockResponseInputStream,
            MediaType.APPLICATION_OCTET_STREAM_TYPE, headers);
        when(mock.post()).thenReturn(fakeResponse);


        final InputStream streamToUpload = IOUtils.toInputStream(MOCK_INPUTSTREAM_CONTENT);
        InputStream fakeUploadResponseInputStream = client.upload(streamToUpload).readEntity(InputStream.class);
        assertNotNull(fakeUploadResponseInputStream);

        try {
            assertTrue(IOUtils.contentEquals(fakeUploadResponseInputStream,
                IOUtils.toInputStream(MOCK_RESPONSE_STREAM)));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
