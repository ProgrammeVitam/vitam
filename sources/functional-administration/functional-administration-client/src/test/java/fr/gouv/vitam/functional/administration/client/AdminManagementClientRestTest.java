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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.model.ProcessPause;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.api.AdminManagementResourceMock;
import fr.gouv.vitam.functional.administration.client.api.AdminReconstructionResourceMock;
import fr.gouv.vitam.functional.administration.client.api.AgenciesResourceMock;
import fr.gouv.vitam.functional.administration.client.api.ArchiveUnitProfileResourceMock;
import fr.gouv.vitam.functional.administration.client.api.ContextResourceMock;
import fr.gouv.vitam.functional.administration.client.api.ContractResourceMock;
import fr.gouv.vitam.functional.administration.client.api.EvidenceResourceMock;
import fr.gouv.vitam.functional.administration.client.api.OntologyResourceMock;
import fr.gouv.vitam.functional.administration.client.api.PreservationResourceMock;
import fr.gouv.vitam.functional.administration.client.api.ProbativeValueResourceMock;
import fr.gouv.vitam.functional.administration.client.api.ProfileResourceMock;
import fr.gouv.vitam.functional.administration.client.api.ReindexationResourceMock;
import fr.gouv.vitam.functional.administration.client.api.SecurityProfileResourceMock;
import fr.gouv.vitam.functional.administration.common.exception.AccessionRegisterException;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class AdminManagementClientRestTest extends ResteasyTestApplication {

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    static final String QUERY =
        "{\"$query\":{\"$and\":[{\"$eq\":{\"OriginatingAgency\":\"OriginatingAgency\"}}]},\"$filter\":{},\"$projection\":{}}";

    private static final Integer TENANT_ID = 0;
    private static final String DATE = "2017-01-01";



    private final static ExpectedResults mock = mock(ExpectedResults.class);

    public static VitamServerTestRunner
        vitamServerTestRunner =
        new VitamServerTestRunner(AdminManagementClientRestTest.class, AdminManagementClientFactory.getInstance());


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
        return Sets.newHashSet(
            new AdminManagementResourceMock(mock),
            new AdminReconstructionResourceMock(mock),
            new AgenciesResourceMock(mock),
            new ArchiveUnitProfileResourceMock(mock),
            new ContextResourceMock(mock),
            new ContractResourceMock(mock),
            new EvidenceResourceMock(mock),
            new OntologyResourceMock(mock),
            new PreservationResourceMock(mock),
            new ProbativeValueResourceMock(mock),
            new ProfileResourceMock(mock),
            new ReindexationResourceMock(mock),
            new SecurityProfileResourceMock(mock)
        );
    }

    @Before
    public void before() {
        reset(mock);
    }

    @Test
    public void givenInputstreamOKWhenCheckThenReturnOK() throws ReferentialException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            Response checkFormatReponse = client.checkFormat(new FakeInputStream(1));
            assertEquals(Status.OK.getStatusCode(), checkFormatReponse.getStatus());
        }
    }

    @Test(expected = ReferentialException.class)
    public void givenInputstreamKOWhenCheckThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            assertEquals(Status.BAD_REQUEST, client.checkFormat(new FakeInputStream(1)));
        }
    }


    @Test
    public void givenInputstreamOKWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.importFormat(new FakeInputStream(1), "DROID_SignatureFile_V94.xml");
        }
    }


    @Test(expected = ReferentialException.class)
    public void givenAnInvalidQueryThenReturnKO() throws Exception {
        when(mock.post())
            .thenReturn(Response.status(Status.OK).build())
            .thenReturn(Response.status(Status.BAD_REQUEST).build());

        final Select select = new Select();
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.importFormat(new FakeInputStream(1), "DROID_SignatureFile_V94.xml");
        }
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getFormats(select.getFinalSelect());
        }
    }

    @Test(expected = ReferentialException.class)
    public void givenAnInvalidIDThenReturnNOTFOUND() throws Exception {
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.importFormat(new FakeInputStream(1), "DROID_SignatureFile_V94.xml");
        }
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.getFormatByID("HDE");
        }
    }

    @Test
    public void givenInputstreamRulesFileOKWhenCheckThenReturnOK() throws ReferentialException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            Response responseCheckRulesFile = client.checkRulesFile(new FakeInputStream(1));
            assertNotNull(responseCheckRulesFile);
        }
    }


    @Test()
    public void givenInputstreamKORulesFileWhenCheckThenReturnKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            Response response = client.checkRulesFile(new FakeInputStream(1));
            assertEquals(400, response.getStatus());
            assertNotNull(response);
        }
    }


    @Test
    @RunWithCustomExecutor
    public void givenInputstreamOKRulesFileWhenImportThenReturnOK() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.importRulesFile(new FakeInputStream(1), "jeu_donnees_KO_regles_CSV_StringToNumber.csv");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void createCreateAgenciesReturnCreated() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.importAgenciesFile(new FakeInputStream(1), "vitam.conf");
        }
    }

    @Test(expected = ReferentialException.class)
    @RunWithCustomExecutor
    public void createAnInvalidAgencyFileThenKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner
            .getClient()) {
            client.importAgenciesFile(new FakeInputStream(1), "vitam.conf");
        }
    }

    @Test(expected = FileRulesException.class)
    @RunWithCustomExecutor
    public void givenAnInvalidFileThenKO() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.importRulesFile(new FakeInputStream(1), "jeu_donnees_KO_regles_CSV_Parameters.csv");
        }

    }

    @Test(expected = FileRulesException.class)
    @RunWithCustomExecutor
    public void givenIllegalArgumentThenthrowFilesRuleException()
        throws FileRulesException, DatabaseConflictException, FileNotFoundException {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity("Wrong format").build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
            client.importRulesFile(new FakeInputStream(1), "jeu_donnees_OK_regles_CSV.csv");
        } catch (FileRulesException e) {
            assertEquals("Wrong format", e.getMessage());
            throw (e);
        } catch (ReferentialException e) {
            fail("May not happen here");
        }
    }

    @Test(expected = ReferentialException.class)
    public void givenAnInvalidIDForRuleThenReturnNotFound() throws Exception {
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.importRulesFile(new FakeInputStream(1), "jeu_donnees_OK_regles_CSV.csv");
        }
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getRuleByID("HDE");
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    @RunWithCustomExecutor
    public void givenInvalidQuerythenReturnko()
        throws ReferentialException, InvalidParseOperationException, DatabaseConflictException {
        when(mock.post()).thenReturn(Response.status(Status.OK).build());
        final Select select = new Select();
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.importRulesFile(new FakeInputStream(1), "jeu_donnees_OK_regles_CSV.csv");
            final JsonNode result = client.getRules(select.getFinalSelect());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void createAccessionRegister()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.createorUpdateAccessionRegister(
                new AccessionRegisterDetailModel().setOpc("IDD").setOriginatingAgency("OG").setStartDate(DATE)
                    .setEndDate(DATE)
                    .setLastUpdate(DATE));
        }
    }

    @Test(expected = AccessionRegisterException.class)
    @RunWithCustomExecutor
    public void createAccessionRegisterError()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.createorUpdateAccessionRegister(
                new AccessionRegisterDetailModel().setOpc("IDD").setOriginatingAgency("OG").setStartDate(DATE)
                    .setEndDate(DATE)
                    .setLastUpdate(DATE));
        }
    }

    @Test(expected = AccessionRegisterException.class)
    @RunWithCustomExecutor
    public void createAccessionRegisterUnknownError()
        throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.createorUpdateAccessionRegister(
                new AccessionRegisterDetailModel().setOpc("IDD").setOriginatingAgency("OG").setStartDate(DATE)
                    .setEndDate(DATE)
                    .setLastUpdate(DATE));
        }
    }

    @Test
    public void getAccessionRegisterDetail()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{}").build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getAccessionRegisterDetail("id", JsonHandler.getFromString(QUERY));
        }
    }

    @Test(expected = ReferentialException.class)
    public void getAccessionRegisterDetailError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getAccessionRegisterDetail("id", JsonHandler.getFromString(QUERY));
        }
    }

    @Test(expected = AccessionRegisterException.class)
    public void getAccessionRegisterDetailUnknownError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getAccessionRegisterDetail("id", JsonHandler.getFromString(QUERY));
        }
    }

    @Test
    public void getAccessionRegisterSummary()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.OK).entity("{}").build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getAccessionRegister(JsonHandler.getFromString(QUERY));
        }
    }

    @Test(expected = ReferentialException.class)
    public void getAccessionRegisterSummaryError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getAccessionRegister(JsonHandler.getFromString(QUERY));
        }
    }

    @Test(expected = InvalidParseOperationException.class)
    public void getAccessionRegisterSummaryUnknownError()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).entity("{}").build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            client.getAccessionRegister(JsonHandler.getFromString(QUERY));
        }
    }

    @Test
    @RunWithCustomExecutor
    public void importIngestContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<IngestContractModel>().addAllResults(getIngestContracts())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            Status resp = client.importIngestContracts(new ArrayList<>());
            assertEquals(resp, Status.CREATED);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllIngestContractsThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<IngestContractModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findIngestContracts(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllIngestContractsThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<IngestContractModel>().addAllResults(getIngestContracts())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findIngestContracts(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
            assertThat(((RequestResponseOK) resp).getResults().iterator().next())
                .isInstanceOf(IngestContractModel.class);
        }
    }

    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findIngestContractsByIdThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<IngestContractModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findIngestContractsByID("fakeId");
        }
    }

    @Test
    @RunWithCustomExecutor
    public void importAccessContractsWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<AccessContractModel>().addAllResults(getAccessContracts())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            Status resp = client.importAccessContracts(new ArrayList<>());
            assertEquals(resp, Status.CREATED);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllAccessContractsThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<AccessContractModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findAccessContracts(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllAccessContractsThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<AccessContractModel>().addAllResults(getAccessContracts())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findAccessContracts(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
            assertThat(((RequestResponseOK) resp).getResults().iterator().next())
                .isInstanceOf(AccessContractModel.class);
        }
    }

    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findAccessContractsByIdThenReturnEmpty()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<AccessContractModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findAccessContractsByID("fakeId");
        }
    }


    private List<AccessContractModel> getAccessContracts()
        throws FileNotFoundException, InvalidParseOperationException {
        File fileContracts = PropertiesUtils.getResourceFile("contracts_access_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<AccessContractModel>>() {
        });
    }

    private List<IngestContractModel> getIngestContracts()
        throws FileNotFoundException, InvalidParseOperationException {
        File fileContracts = PropertiesUtils.getResourceFile("referential_contracts_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileContracts, new TypeReference<List<IngestContractModel>>() {
        });
    }



    @Test
    @RunWithCustomExecutor
    public void createProfilesWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<ProfileModel>().addAllResults(getProfiles())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createProfiles(new ArrayList<>());
            assertEquals(resp.getHttpCode(), Status.CREATED.getStatusCode());
        }
    }


    @Test
    @RunWithCustomExecutor
    public void importProfileFileWithFakeFileReturnCreated()
        throws ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.put()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.importProfileFile("fakeId", new FakeInputStream(0l));
            assertEquals(resp.getHttpCode(), Status.CREATED.getStatusCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllProfilesThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<ProfileModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findProfiles(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void updateProfile()
        throws InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.put()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<ProfileModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.updateProfile("fakeId", JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllProfilesThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<ProfileModel>().addAllResults(getProfiles())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findProfiles(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
            assertThat(((RequestResponseOK) resp).getResults().iterator().next()).isInstanceOf(ProfileModel.class);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllAgenciesThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<ProfileModel>().addAllResults(getProfiles())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            JsonNode resp = client.getAgencies(JsonHandler.createObjectNode());
        }
    }

    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findProfilesByIdThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get())
            .thenReturn(Response.status(Status.OK).entity(new RequestResponseOK<ProfileModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findProfilesByID("fakeId");
        }
    }



    @RunWithCustomExecutor
    @Test
    public void findAgencyByIdThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get())
            .thenReturn(Response.status(Status.NOT_FOUND).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<AgenciesModel> response = client.getAgencyById("fakeId");
            assertFalse(response.isOk());
            assertEquals(response.getStatus(), Status.NOT_FOUND.getStatusCode());
        }
    }



    @Test
    @RunWithCustomExecutor
    public void givenProfileIdWhenDownloadProfileFileThenOK() throws Exception {

        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            Response response = client.downloadProfileFile("OP_ID");
            assertNotNull(response);
        }
    }

    private List<ProfileModel> getProfiles() throws FileNotFoundException, InvalidParseOperationException {
        File fileProfiles = PropertiesUtils.getResourceFile("profile_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileProfiles, new TypeReference<List<ProfileModel>>() {
        });
    }

    @Test
    @RunWithCustomExecutor
    public void importContextsWithCorrectJsonReturnCreated()
        throws ReferentialException, FileNotFoundException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<ContextModel>().addAllResults(getContexts())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            Status resp = client.importContexts(new ArrayList<>());
            assertEquals(resp, Status.CREATED);
        }
    }

    @Test(expected = ReferentialException.class)
    @RunWithCustomExecutor
    public void importContextsWithNoCorrectJsonReturnCreated()
        throws ReferentialException, FileNotFoundException, InvalidParseOperationException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST)
            .entity(new RequestResponseOK<ContextModel>().addAllResults(getContexts())).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            Status resp = client.importContexts(new ArrayList<>());
        }
    }


    @Test
    @RunWithCustomExecutor
    public void launchAuditWithCorrectJsonReturnAccepted()
        throws ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED)
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<JsonNode> resp = client.launchAuditWorkflow(JsonHandler.createObjectNode());
            assertEquals(resp.getStatus(), Status.ACCEPTED.getStatusCode());
        }
    }

    private List<ContextModel> getContexts() throws FileNotFoundException, InvalidParseOperationException {
        File fileContexts = PropertiesUtils.getResourceFile("contexts_ok.json");
        return JsonHandler.getFromFileAsTypeRefence(fileContexts, new TypeReference<List<ContextModel>>() {
        });
    }

    @Test
    @RunWithCustomExecutor
    public void launchRuleAuditWithCorrectJsonReturnAccepted()
        throws ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(Response.status(Status.ACCEPTED)
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<JsonNode> resp = client.launchRuleAudit();
            assertEquals(resp.getStatus(), Status.ACCEPTED.getStatusCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void launchReindexationTest()
        throws ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post())
            .thenReturn(Response.status(Status.CREATED).entity(JsonHandler.unprettyPrint(new RequestResponseOK<>()))
                .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<IndexationResult> resp = client.launchReindexation(JsonHandler.createArrayNode());
            assertEquals(resp.getStatus(), Status.CREATED.getStatusCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void switchIndexesTest()
        throws ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.post()).thenReturn(Response.status(Status.OK)
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<IndexationResult> resp = client.switchIndexes(JsonHandler.createArrayNode());
            assertEquals(resp.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void evidenceAuditTest()
        throws ReferentialException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.OK)
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<JsonNode> resp = client.evidenceAudit(new Select().getFinalSelect());
            assertEquals(resp.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void rectificationAuditTest() throws AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.OK)
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<JsonNode> resp = client.rectificationAudit("id");
            assertEquals(resp.getStatus(), Status.OK.getStatusCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void createArchiveUnitProfilesWithCorrectJsonReturnCreated()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.post()).thenReturn(Response.status(Status.CREATED)
            .entity(new RequestResponseOK<ArchiveUnitProfileModel>().addAllResults(getArchiveUnitProfiles()))
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.createArchiveUnitProfiles(new ArrayList<>());
            assertEquals(Status.CREATED.getStatusCode(), resp.getHttpCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllArchiveUnitProfilesThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).entity(new RequestResponseOK<ArchiveUnitProfileModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findArchiveUnitProfiles(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(0);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void updateArchiveUnitProfile()
        throws InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.put()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<ArchiveUnitProfileModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.updateArchiveUnitProfile("fakeId", JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
        }
    }

    @Test
    @RunWithCustomExecutor
    public void findAllArchiveUnitProfilesThenReturnTwo()
        throws FileNotFoundException, InvalidParseOperationException, AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK)
            .entity(new RequestResponseOK<ArchiveUnitProfileModel>().addAllResults(getArchiveUnitProfiles()))
            .build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findArchiveUnitProfiles(JsonHandler.createObjectNode());
            assertThat(resp).isInstanceOf(RequestResponseOK.class);
            assertThat(((RequestResponseOK) resp).getResults()).hasSize(2);
            assertThat(((RequestResponseOK) resp).getResults().iterator().next())
                .isInstanceOf(ArchiveUnitProfileModel.class);
        }
    }

    @Test(expected = ReferentialNotFoundException.class)
    @RunWithCustomExecutor
    public void findArchiveUnitProfilesByIdThenReturnEmpty()
        throws InvalidParseOperationException, AdminManagementClientServerException,
        ReferentialNotFoundException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        when(mock.get())
            .thenReturn(
                Response.status(Status.OK).entity(new RequestResponseOK<ArchiveUnitProfileModel>()).build());
        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse resp = client.findArchiveUnitProfilesByID("fakeId");
            VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        }
    }

    private List<ArchiveUnitProfileModel> getArchiveUnitProfiles()
        throws FileNotFoundException, InvalidParseOperationException {
        File fileArchiveUnitProfiles = PropertiesUtils.getResourceFile("archive_unit_profile_ok.json");
        return JsonHandler
            .getFromFileAsTypeRefence(fileArchiveUnitProfiles, new TypeReference<List<ArchiveUnitProfileModel>>() {
            });
    }

    @Test
    @RunWithCustomExecutor
    public void testForceUpdate()
        throws AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        ProcessPause info = new ProcessPause("INGEST", 0, null);
        when(mock.post()).thenReturn(Response.status(Status.OK).build());

        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ProcessPause> resp = client.forcePause(info);
            assertEquals(Status.OK.getStatusCode(), resp.getHttpCode());
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testRemoveForceUpdate()
        throws AdminManagementClientServerException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        ProcessPause info = new ProcessPause("INGEST", 0, null);
        when(mock.post()).thenReturn(Response.status(Status.OK).build());

        try (AdminManagementClientRest client = (AdminManagementClientRest) vitamServerTestRunner.getClient()) {
            RequestResponse<ProcessPause> resp = client.removeForcePause(info);
            assertEquals(Status.OK.getStatusCode(), resp.getHttpCode());
        }
    }

}
