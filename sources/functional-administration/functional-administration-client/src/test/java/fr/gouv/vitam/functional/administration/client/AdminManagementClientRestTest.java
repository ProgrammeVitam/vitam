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
package fr.gouv.vitam.functional.administration.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;

public class AdminManagementClientRestTest extends VitamJerseyTest {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/adminmanagement/v1";
    protected AdminManagementClientRest client;
    static final String QUERY =
        "{\"$query\":{\"$and\":[{\"$eq\":{\"OriginatingAgency\":\"OriginatingAgency\"}}]},\"$filter\":{},\"$projection\":{}}";

    private static final Integer TENANT_ID = 0;
    private static final String DATE = "2017-01-01";

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public AdminManagementClientRestTest() {
        super(AdminManagementClientFactory.getInstance());
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (AdminManagementClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
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

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }


    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }


    @Path("/adminmanagement/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @POST
        @Path("/format/check")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkFormat(InputStream xmlPronom) {
            return expectedResponse.post();
        }

        @POST
        @Path("/format/import")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importFormat(InputStream xmlPronom) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("/format/delete")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteFormat() {
            return expectedResponse.post();
        }

        @GET
        @Path("/format/{id_format}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getFormatByID() {
            return expectedResponse.get();
        }

        @POST
        @Path("/format/document")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getFormats() {
            return expectedResponse.get();
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }

        @POST
        @Path("/rules/check")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkRulesFile(InputStream xmlPronom) {
            return expectedResponse.post();
        }

        @POST
        @Path("/rules/import")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importRulesFile(InputStream xmlPronom) {
            return expectedResponse.post();
        }

        @DELETE
        @Path("/rules/delete")
        @Produces(MediaType.APPLICATION_JSON)
        public Response deleteRulesFile() {
            return expectedResponse.post();
        }

        @GET
        @Path("/rules/{id_rule}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response findRuleByID() {
            return expectedResponse.get();
        }

        @POST
        @Path("/rules/document")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getRulesFile() {
            return expectedResponse.post();
        }

        @POST
        @Path("/accession-register/document")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getFunds() {
            return expectedResponse.post();
        }

        @POST
        @Path("/accession-register")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createAccessionRegister() {
            return expectedResponse.post();
        }

        @POST
        @Path("/accession-register/detail/{id}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAccessionRegisterDetail() {
            return expectedResponse.post();
        }

        @POST
        @Path("/entrycontracts")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importContracts(ArrayNode contractsToImport, @Context UriInfo uri) {
            return expectedResponse.post();
        }

        @GET
        @Path("/entrycontracts")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findContracts(@Context UriInfo uri) {
            return expectedResponse.get();
        }

        @POST
        @Path("/accesscontracts")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importAccessContracts(List<AccessContractModel> accessContractModelList) {

            return expectedResponse.post();
        }

        @GET
        @Path("/accesscontracts")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findAccessContracts(JsonNode queryDsl) {
            return expectedResponse.get();
        }



        @POST
        @Path("/profiles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response createProfiles(List<ProfileModel> accessContractModelList) {
            return expectedResponse.post();
        }

        @PUT
        @Path("/profiles/{id}")
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importProfileFile(@PathParam("id") String profileMetadataId,
            InputStream profileFile) {
            return expectedResponse.put();
        }

        @GET
        @Path("/profiles")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response findProfiles(JsonNode queryDsl) {
            return expectedResponse.get();
        }

        @GET
        @Path("/profiles/{id}")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("id") String id)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

        @POST
        @Path("/contexts")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response importContexts(List<ContextModel> ContextModelList) {
            return expectedResponse.post();
        }

        @POST
        @Path("/audit")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response launchAudit(JsonNode options) {
            return expectedResponse.post();
        }

    }


    @Test
    public void givenInputstreamOKWhenCheckThenReturnOK() throws ReferentialException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        Response checkFormatReponse = client.checkFormat(stream);
        assertEquals(Status.OK.getStatusCode(), checkFormatReponse.getStatus());
    }

    @Test(expected = ReferentialException.class)
    public void givenInputstreamKOWhenCheckThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("FF-vitam-format-KO.xml");
        assertEquals(Status.BAD_REQUEST, client.checkFormat(stream));
    }


    @Test
    public void givenInputstreamOKWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream, "FF-vitam.xml");
    }


    @Test(expected = ReferentialException.class)
    public void givenAnInvalidQueryThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final Select select = new Select();
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream, "FF-vitam.xml");
        client.getFormats(select.getFinalSelect());
        client.getFormatByID("HDE");
    }

    @Test(expected = ReferentialException.class)
    public void givenAnInvalidIDThenReturnNOTFOUND() throws Exception {
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream, "FF-vitam.xml");
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getFormatByID("HDE");
    }

    /***********************************************************************************
     * Rules Manager
     *
     * @throws FileNotFoundException
     ***********************************************************************************/

    @Test
    public void givenInputstreamRulesFileOKWhenCheckThenReturnOK() throws ReferentialException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        Response responseCheckRulesFile = client.checkRulesFile(stream);
        assertNotNull(responseCheckRulesFile);
    }


    @Test()
    public void givenInputstreamKORulesFileWhenCheckThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_StringToNumber.csv");
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());
        Response response = client.checkRulesFile(stream);
        assertEquals(406, response.getStatus());
        assertNotNull(response);
    }


    @Test
    @RunWithCustomExecutor
    public void givenInputstreamOKRulesFileWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_StringToNumber.csv");
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.importRulesFile(stream, "jeu_donnees_KO_regles_CSV_StringToNumber.csv");
    }


    @Test(expected = FileRulesException.class)
    @RunWithCustomExecutor
    public void givenAnInvalidFileThenKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.importRulesFile(stream, "jeu_donnees_KO_regles_CSV_Parameters.csv");

    }

    /**
     * @throws FileRulesException
     * @throws InvalidParseOperationException
     * @throws DatabaseConflictException
     * @throws FileNotFoundException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = FileRulesException.class)
    @RunWithCustomExecutor
    public void givenIllegalArgumentThenthrowFilesRuleException()
        throws FileRulesException, InvalidParseOperationException, DatabaseConflictException, FileNotFoundException,
        AdminManagementClientServerException {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity("Wrong format").build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        try {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            client.importRulesFile(stream, "jeu_donnees_OK_regles_CSV.csv");
        } catch (FileRulesException e) {
            assertEquals("Wrong format", e.getMessage());
            throw (e);
        } catch (ReferentialException e) {
            fail("May not happen here");
        }
    }

    @Test(expected = ReferentialException.class)
    public void givenAnInvalidIDForRuleThenReturnNotFound() throws Exception {
        final InputStream stream = PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.importRulesFile(stream, "jeu_donnees_OK_regles_CSV.csv");
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getRuleByID("HDE");
    }

    /**
     * @throws FileRulesException
     * @throws InvalidParseOperationException
     * @throws DatabaseConflictException
     * @throws FileNotFoundException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = InvalidParseOperationException.class)
    @RunWithCustomExecutor
    public void givenInvalidQuerythenReturnko()
        throws ReferentialException, InvalidParseOperationException, DatabaseConflictException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final Select select = new Select();
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        client.importRulesFile(stream, "jeu_donnees_OK_regles_CSV.csv");
        final JsonNode result = client.getRules(select.getFinalSelect());
    }

    @Test
    @RunWithCustomExecutor
    public void createAccessionRegister()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.createorUpdateAccessionRegister(new AccessionRegisterDetailModel().setStartDate(DATE).setEndDate(DATE)
            .setLastUpdate(DATE));
    }

    @Test(expected = AccessionRegisterException.class)
    @RunWithCustomExecutor
    public void createAccessionRegisterError()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.createorUpdateAccessionRegister(new AccessionRegisterDetailModel().setStartDate(DATE).setEndDate(DATE)
            .setLastUpdate(DATE));
    }

    @Test(expected = AccessionRegisterException.class)
    @RunWithCustomExecutor
    public void createAccessionRegisterUnknownError()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.createorUpdateAccessionRegister(new AccessionRegisterDetailModel().setStartDate(DATE).setEndDate(DATE)
            .setLastUpdate(DATE));
    }

    /**
     * Accession Register Detail
     **/

    @Test
    public void getAccessionRegisterDetail()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{}").build());
        client.getAccessionRegisterDetail("id", JsonHandler.getFromString(QUERY));
    }

    @Test(expected = ReferentialException.class)
    public void getAccessionRegisterDetailError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getAccessionRegisterDetail("id", JsonHandler.getFromString(QUERY));
    }

    @Test(expected = AccessionRegisterException.class)
    public void getAccessionRegisterDetailUnknownError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.getAccessionRegisterDetail("id", JsonHandler.getFromString(QUERY));
    }

    /**
     * Accession Register Summary
     **/
    @Test
    public void getAccessionRegisterSummary()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{}").build());
        client.getAccessionRegister(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = ReferentialException.class)
    public void getAccessionRegisterSummaryError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getAccessionRegister(JsonHandler.getFromString(QUERY));
    }

    @Test
    public void getAccessionRegisterSummaryUnknownError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity("{}").build());
        client.getAccessionRegister(JsonHandler.getFromString(QUERY));
    }

    @Test
    @RunWithCustomExecutor
    public void importIngestContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<IngestContractModel>().addAllResults(getIngestContracts())).build());
        Status resp = client.importIngestContracts(new ArrayList<>());
        assertEquals(resp, Status.CREATED);
    }


    /**
     * Test that findIngestContracts is reachable and does not return elements
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllIngestContractsThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<IngestContractModel>()).build());
        RequestResponse resp = client.findIngestContracts(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }


    /**
     * Test that findIngestContracts is reachable and return two elements as expected
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllIngestContractsThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<IngestContractModel>().addAllResults(getIngestContracts())).build());
        RequestResponse resp = client.findIngestContracts(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
        assertThat(((RequestResponseOK) resp).getResults().iterator().next()).isInstanceOf(IngestContractModel.class);
    }


    /**
     * Test that findIngestContractsByID is reachable
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findIngestContractsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<IngestContractModel>()).build());
        RequestResponse resp = client.findIngestContractsByID("fakeId");
    }

    @Test
    @RunWithCustomExecutor
    public void importAccessContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<AccessContractModel>().addAllResults(getAccessContracts())).build());
        Status resp = client.importAccessContracts(new ArrayList<>());
        assertEquals(resp, Status.CREATED);
    }


    /**
     * Test that findAccessContracts is reachable and does not return elements
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllAccessContractsThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<AccessContractModel>()).build());
        RequestResponse resp = client.findAccessContracts(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }


    /**
     * Test that findAccessContracts is reachable and return two elements as expected
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllAccessContractsThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<AccessContractModel>().addAllResults(getAccessContracts())).build());
        RequestResponse resp = client.findAccessContracts(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
        assertThat(((RequestResponseOK) resp).getResults().iterator().next()).isInstanceOf(AccessContractModel.class);
    }


    /**
     * Test that findAccessContractsByID is reachable
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findAccessContractsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<AccessContractModel>()).build());
        RequestResponse resp = client.findAccessContractsByID("fakeId");
    }


    private List<AccessContractModel> getAccessContracts()
        throws FileNotFoundException, InvalidParseOperationException {
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>() {});
    }

    private List<IngestContractModel> getIngestContracts()
        throws FileNotFoundException, InvalidParseOperationException {
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {});
    }



    @Test
    @RunWithCustomExecutor
    public void createProfilesWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<ProfileModel>().addAllResults(getProfiles())).build());
        RequestResponse resp = client.createProfiles(new ArrayList<>());
        assertEquals(resp.getHttpCode(), Status.CREATED.getStatusCode());
    }


    @Test
    @RunWithCustomExecutor
    public void importProfileFileWithFakeFileReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.put()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).build());
        RequestResponse resp = client.importProfileFile("fakeId", new FakeInputStream(0l));
        assertEquals(resp.getHttpCode(), Status.CREATED.getStatusCode());
    }

    /**
     * Test that profiles is reachable and does not return elements
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllProfilesThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<ProfileModel>()).build());
        RequestResponse resp = client.findProfiles(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
    }


    /**
     * Test that profiles is reachable and return two elements as expected
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test
    @RunWithCustomExecutor
    public void findAllProfilesThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<ProfileModel>().addAllResults(getProfiles())).build());
        RequestResponse resp = client.findProfiles(JsonHandler.createObjectNode());
        assertThat(resp).isInstanceOf(RequestResponseOK.class);
        assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
        assertThat(((RequestResponseOK) resp).getResults().iterator().next()).isInstanceOf(ProfileModel.class);
    }


    /**
     * Test that profiles by id is reachable
     * 
     * @throws FileNotFoundException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findProfilesByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get()).thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<ProfileModel>()).build());
        RequestResponse resp = client.findProfilesByID("fakeId");
    }


    @Test
    @RunWithCustomExecutor
    public void givenProfileIdWhenDownloadProfileFileThenOK() throws Exception {

        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        Response response = client.downloadProfileFile("OP_ID");
        assertNotNull(response);
    }

    private List<ProfileModel> getProfiles() throws FileNotFoundException, InvalidParseOperationException {
        File fileProfiles = PropertiesUtils.getResourceFile("profile_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileProfiles, new TypeReference<List<ProfileModel>>() {});
    }

    @Test
    @RunWithCustomExecutor
    public void importContextsWithCorrectJsonReturnCreated()
        throws ReferentialException, FileNotFoundException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<ContextModel>().addAllResults(getContexts())).build());
        Status resp = client.importContexts(new ArrayList<>());
        assertEquals(resp, Status.CREATED);
    }

    @Test
    @RunWithCustomExecutor
    public void launchAuditWithCorrectJsonReturnAccepted()
        throws ReferentialException, FileNotFoundException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED)
            .build());
        RequestResponse<JsonNode> resp = client.launchAuditWorkflow(JsonHandler.createObjectNode());
        assertEquals(resp.getStatus(), Status.ACCEPTED.getStatusCode());
    }

    private List<ContextModel> getContexts() throws FileNotFoundException, InvalidParseOperationException {
        File fileContexts = PropertiesUtils.getResourceFile("contexts_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {});
    }
}
