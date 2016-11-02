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
package fr.gouv.vitam.ingest.internal.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.ingest.internal.model.UploadResponseDTO;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

@SuppressWarnings("rawtypes")
public class IngestInternalClientRestTest extends VitamJerseyTest {

    private static final String PATH = "/ingest/v1";

    private IngestInternalClientRest client;
    private UploadResponseDTO uploadResponseDTO;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    @SuppressWarnings("unchecked")
    public IngestInternalClientRestTest() {
        super(IngestInternalClientFactory.getInstance());
    }

    @Override
    public void beforeTest() {
        client = (IngestInternalClientRest) getClient();
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new MockRessource(mock));
            resourceConfig.register(MultiPartFeature.class);
        }
    }

    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

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

    @Path(PATH)
    public static class MockRessource {

        private final ExpectedResults expectedResponse;

        public MockRessource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Path("/upload")
        @POST
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_JSON)
        public Response upload(@FormDataParam("file") InputStream stream,
            @FormDataParam("file") FormDataContentDisposition header) {
            return expectedResponse.post();
        }
    }

    @Test
    public void givenStartedServerWhenUploadSipThenReturnOK() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(FileUtil.readInputStream(inputStreamATR)).build());
        final InputStream inputStream =

            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        final Response response = client.upload(ingestGuid, operationList, inputStream, CommonMediaType.ZIP);
        inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        assertEquals(response.readEntity(String.class), FileUtil.readInputStream(inputStreamATR));

    }

    @Test
    public void givenVirusWhenUploadSipThenReturnKO() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.KO,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        InputStream inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        when(mock.post()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).entity(FileUtil.readInputStream(inputStreamATR)).build());
        final Response response = client.upload(ingestGuid, operationList, null, CommonMediaType.ZIP);
        assertEquals(500, response.getStatus());
        inputStreamATR = PropertiesUtils.getResourceAsStream("ATR_example.xml");
        assertEquals(response.readEntity(String.class), FileUtil.readInputStream(inputStreamATR));

    }

    @Test
    public void givenServerErrorWhenPostSipThenRaiseAnException() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);

        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(uploadResponseDTO).build());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_bordereau_avec_objet_OK.zip");
        final Response response = client.upload(ingestGuid, operationList, inputStream, CommonMediaType.ZIP);
        assertEquals(500, response.getStatus());
        assertNotNull(response.readEntity(String.class));
    }

    @Test
    public void givenStartedServerWhenUploadSipNonZipThenReturnKO() throws Exception {

        final List<LogbookOperationParameters> operationList = new ArrayList<>();

        final GUID ingestGuid = GUIDFactory.newGUID();
        final GUID conatinerGuid = GUIDFactory.newGUID();
        final LogbookOperationParameters externalOperationParameters1 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.STARTED,
                "Start Ingest external",
                conatinerGuid);

        final LogbookOperationParameters externalOperationParameters2 =
            LogbookParametersFactory.newLogbookOperationParameters(
                ingestGuid,
                "Ingest external",
                conatinerGuid,
                LogbookTypeProcess.INGEST,
                StatusCode.OK,
                "End Ingest external",
                conatinerGuid);
        operationList.add(externalOperationParameters1);
        operationList.add(externalOperationParameters2);
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).entity(uploadResponseDTO).build());
        final InputStream inputStream =
            PropertiesUtils.getResourceAsStream("SIP_mauvais_format.pdf");
        final Response response = client.upload(ingestGuid, operationList, inputStream, CommonMediaType.ZIP);
        assertEquals(500, response.getStatus());
        assertNotNull(response.readEntity(String.class));
    }
}
