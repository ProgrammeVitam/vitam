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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;

import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.ingest.internal.model.UploadResponseDTO;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

public class IngestInternalClientRestTest extends JerseyTest {

    private static final String HOST = "localhost";
    private static final String PATH = "/ingest/v1";
    private static JunitHelper junitHelper = JunitHelper.getInstance();
    private static int port = junitHelper.findAvailablePort();
    private final IngestInternalClientRest client;
    private UploadResponseDTO uploadResponseDTO;

    protected ExpectedResults mock;

    public IngestInternalClientRestTest() {
        client = new IngestInternalClientRest(HOST, port);
    }


    interface ExpectedResults {
        Response post();

        Response get();
    }

    @Path(PATH)
    public static class IngestInternalMockResource {

        private final ExpectedResults expectedResponse;

        public IngestInternalMockResource(ExpectedResults expectedResponse) {
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

        @Path("/status")
        @GET
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response status() {
            return expectedResponse.get();
        }
    }

    @Override
    protected Application configure() {
        set(TestProperties.DUMP_ENTITY, true);
        forceSet(TestProperties.CONTAINER_PORT, String.valueOf(port));

        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(MultiPartFeature.class);
        mock = mock(ExpectedResults.class);
        resourceConfig.registerInstances(new IngestInternalMockResource(mock));
        return resourceConfig;
    }

    @Test
    public void givenStartedServerWhenGetStatusThenReturnOK() {
        when(mock.get()).thenReturn(Response.status(Status.OK).build());
        assertThat(client.status()).isEqualTo(200);
    }

    @Test
    public void givenStartedServerWhenUploadSipThenReturnOK() throws Exception {

        final List<LogbookParameters> operationList = new ArrayList<LogbookParameters>();

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

        InputStream inputStreamATR = PropertiesUtils.getResourcesAsStream("ATR_example.xml");
        when(mock.post())
            .thenReturn(Response.status(Status.OK).entity(FileUtil.readInputStream(inputStreamATR)).build());
        final InputStream inputStream =
            PropertiesUtils.getResourcesAsStream("SIP_bordereau_avec_objet_OK.zip");
        final Response response = client.upload(operationList, inputStream);
        inputStreamATR = PropertiesUtils.getResourcesAsStream("ATR_example.xml");
        assertEquals(response.readEntity(String.class), FileUtil.readInputStream(inputStreamATR));
    }

    @Test(expected = VitamException.class)
    public void givenVirusWhenUploadSipThenReturnKO() throws Exception {

        final List<LogbookParameters> operationList = new ArrayList<LogbookParameters>();

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

        final InputStream inputStreamATR = PropertiesUtils.getResourcesAsStream("ATR_example.xml");
        when(mock.post()).thenReturn(
            Response.status(Status.INTERNAL_SERVER_ERROR).entity(FileUtil.readInputStream(inputStreamATR)).build());
        client.upload(operationList, null);
    }

    @Test(expected = VitamException.class)
    public void givenServerErrorWhenPostSipThenRaiseAnException() throws Exception {

        final List<LogbookParameters> operationList = new ArrayList<LogbookParameters>();

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
            PropertiesUtils.getResourcesAsStream("SIP_bordereau_avec_objet_OK.zip");
        client.upload(operationList, inputStream);
    }

    @Test(expected = VitamException.class)
    public void givenStartedServerWhenUploadSipNonZipThenReturnKO() throws Exception {

        final List<LogbookParameters> operationList = new ArrayList<LogbookParameters>();

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
            PropertiesUtils.getResourcesAsStream("SIP_mauvais_format.pdf");
        client.upload(operationList, inputStream);
    }
}
