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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server2.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server2.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

public class AdminManagementClientRestTest extends VitamJerseyTest {

    protected static final String HOSTNAME = "localhost";
    protected static final String PATH = "/adminmanagement/v1";
    protected AdminManagementClientRest client;
    static final String QUERY =
        "{\"$query\":{\"$and\":[{\"$eq\":{\"OriginatingAgency\":\"OriginatingAgency\"}}]},\"$filter\":{},\"$projection\":{}}";


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

        @POST
        @Path("/format/{id_format}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getFormatByID() {
            return expectedResponse.post();
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

        @POST
        @Path("/rules/{id_rule}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response findRuleByID() {
            return expectedResponse.post();
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
        @Path("/accession-register/detail")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getAccessionRegisterDetail() {
            return expectedResponse.post();
        }
    }


    @Test
    public void givenInputstreamOKWhenCheckThenReturnOK() throws ReferentialException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        assertEquals(Status.OK, client.checkFormat(stream));
    }

    @Test(expected = ReferentialException.class)
    public void givenInputstreamKOWhenCheckThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("FF-vitam-format-KO.xml");
        assertEquals(Status.PRECONDITION_FAILED, client.checkFormat(stream));
    }


    @Test
    public void givenInputstreamOKWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);
    }


    @Test
    public void whenDeteleFormatReturnKO() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.OK).build());
        client.deleteFormat();
    }


    @Test(expected = ReferentialException.class)
    public void givenAnInvalidQueryThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final Select select = new Select();
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);
        client.getFormats(select.getFinalSelect());
        client.getFormatByID("HDE");
    }

    @Test(expected = ReferentialException.class)
    public void givenAnInvalidIDThenReturnNOTFOUND() throws Exception {
        final InputStream stream = PropertiesUtils.getResourceAsStream("FF-vitam.xml");
        client.importFormat(stream);
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
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
        assertEquals(Status.OK, client.checkRulesFile(stream));
    }


    @Test(expected = ReferentialException.class)
    public void givenInputstreamKORulesFileWhenCheckThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_StringToNumber.csv");
        assertEquals(Status.PRECONDITION_FAILED, client.checkRulesFile(stream));

    }


    @Test
    public void givenInputstreamOKRulesFileWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_StringToNumber.csv");
        client.importRulesFile(stream);
    }


    @Test
    public void whenDeteleRulesFileReturnKO() throws Exception {
        when(mock.delete()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.deleteRulesFile();
    }


    @Test(expected = FileRulesException.class)
    public void givenAnInvalidFileThenKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final Select select = new Select();
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_KO_regles_CSV_Parameters.csv");
        client.importRulesFile(stream);

    }

    /**
     * @param select
     * @throws FileRulesException
     * @throws InvalidParseOperationException
     * @throws DatabaseConflictException
     * @throws FileNotFoundException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = FileRulesException.class)
    public void givenIllegalArgumentThenthrowFilesRuleException()
        throws FileRulesException, InvalidParseOperationException, DatabaseConflictException, FileNotFoundException,
        AdminManagementClientServerException {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.importRulesFile(stream);
        final JsonNode result = client.getRuleByID("APP-00001");

    }

    /**
     * @param select
     * @throws FileRulesException
     * @throws InvalidParseOperationException
     * @throws DatabaseConflictException
     * @throws FileNotFoundException
     * @throws AdminManagementClientServerException
     */
    @Test(expected = InvalidParseOperationException.class)
    public void givenInvalidQuerythenReturnko()
        throws FileRulesException, InvalidParseOperationException, DatabaseConflictException, FileNotFoundException,
        AdminManagementClientServerException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final Select select = new Select();
        final InputStream stream =
            PropertiesUtils.getResourceAsStream("jeu_donnees_OK_regles_CSV.csv");
        client.importRulesFile(stream);
        final JsonNode result = client.getRules(select.getFinalSelect());
    }

    @Test
    public void createAccessionRegister()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.createorUpdateAccessionRegister(new AccessionRegisterDetail());
    }

    @Test(expected = AccessionRegisterException.class)
    public void createAccessionRegisterError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        client.createorUpdateAccessionRegister(new AccessionRegisterDetail());
    }

    @Test(expected = AccessionRegisterException.class)
    public void createAccessionRegisterUnknownError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.createorUpdateAccessionRegister(new AccessionRegisterDetail());
    }

    /** Accession Register Detail **/

    @Test
    public void getAccessionRegisterDetail()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{}").build());
        client.getAccessionRegisterDetail(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = ReferentialException.class)
    public void getAccessionRegisterDetailError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        client.getAccessionRegisterDetail(JsonHandler.getFromString(QUERY));
    }

    @Test(expected = AccessionRegisterException.class)
    public void getAccessionRegisterDetailUnknownError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        client.getAccessionRegisterDetail(JsonHandler.getFromString(QUERY));
    }

    /** Accession Register Summary **/
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

}
