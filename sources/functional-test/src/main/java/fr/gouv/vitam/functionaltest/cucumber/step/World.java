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
package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.v2.AccessExternalClientV2;
import fr.gouv.vitam.access.external.client.v2.AccessExternalClientV2Factory;
import fr.gouv.vitam.client.IhmRecetteClient;
import fr.gouv.vitam.client.IhmRecetteClientFactory;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.functionaltest.configuration.TnrClientConfiguration;
import fr.gouv.vitam.functionaltest.cucumber.service.AccessService;
import fr.gouv.vitam.functionaltest.cucumber.service.LogbookService;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class World {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(World.class);

    public static final String TNR_BASE_DIRECTORY = "tnrBaseDirectory";

    private static final String TNR_CONF = "tnr.conf";
    public static final String DEFAULT_ACCESS_CONTRACT_NAME = "ContratTNR";

    private int tenantId;
    private String contractId;
    private String unitId;
    private String objectGroupId;
    private String applicationSessionId;
    private String projectId;
    private String TransactionId;
    private List<JsonNode> results;
    private LogbookEvent logbookEvent;
    private Path sipFile;
    private Path dipFile;
    private Path transferFile;
    private Path atrFile;

    /**
     * id of the operation
     */
    private String operationId;

    /**
     * id of the elimination operation
     */
    private String eliminationOperationId;

    /**
     * Named operation ids
     */
    private final Map<String, String> namedOperationIds = new HashMap<>();

    /**
     * DSL query
     */
    private String query;

    /**
     * Map of operations ids by testSet
     */
    private static final Map<String, String> operationIdsByTestSet = new HashMap<>();

    /**
     * Logbook service
     */
    private LogbookService logbookService;

    /**
     * Access service
     */
    private AccessService accessService;

    /**
     * ingest external client
     */
    private IngestExternalClient ingestClient;

    /**
     * access eternal client
     */
    private AccessExternalClient accessClient;

    /**
     * admin external client
     */
    private AdminExternalClient adminClient;

    private AccessExternalClientV2 adminClientV2;

    /**
     * logbook operations client
     */
    private LogbookOperationsClient logbookOperationsClient;
    /**
     * Storage Client
     */
    private StorageClient storageClient;
    /**
     * tnr configuration
     */
    private TnrClientConfiguration tnrClientConfiguration;
    /**
     *
     */
    WorkspaceClient workspaceClient;

    /**
     * base path of all the feature
     */
    private final String baseDirectory = System.getProperty(TNR_BASE_DIRECTORY);

    /**
     * Collect client
     */
    private CollectExternalClient collectExternalClient;

    /**
     * initialization of client
     *
     * @throws IOException
     */
    @Before
    public void init() throws IOException {
        configuration();
        VitamConfiguration.setSecret(tnrClientConfiguration.getVitamSecret());
        VitamConfiguration.setTenants(tnrClientConfiguration.getTenants());
        VitamConfiguration.setAdminTenant(tnrClientConfiguration.getAdminTenant());
        ingestClient = IngestExternalClientFactory.getInstance().getClient();
        accessClient = AccessExternalClientFactory.getInstance().getClient();
        adminClient = AdminExternalClientFactory.getInstance().getClient();
        adminClientV2 = AccessExternalClientV2Factory.getInstance().getClient();
        collectExternalClient = CollectExternalClientFactory.getInstance().getClient();

        storageClient = StorageClientFactory.getInstance().getClient();
        WorkspaceClientFactory.changeMode(tnrClientConfiguration.getUrlWorkspace());
        configureLogbookClient();
        logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient();
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
        logbookService = new LogbookService();
        accessService = new AccessService();
    }

    /**
     * define a tenant
     *
     * @param tenantId id of the tenant
     * @throws Throwable
     */
    @Given("^les tests effectués sur le tenant (\\d+)$")
    public void the_test_are_done_on_tenant(int tenantId) throws Throwable {
        this.tenantId = tenantId;
    }

    /**
     * define a contractId
     *
     * @param contractId id of the access contract
     * @throws Throwable
     */
    @Given("^les tests effectués sur le contrat id (.*)$")
    public void the_test_are_done_on_contract(String contractId) throws Throwable {
        this.contractId = contractId;
    }

    /**
     * defines application session id
     *
     * @param applicationSessionId id of the application session
     * @throws Throwable
     */
    @Given("^les tests effectués avec l'identifiant de transaction (.*)$")
    public void the_tests_are_done_with_application_session_id(String applicationSessionId) {
        this.applicationSessionId = applicationSessionId;
    }

    /**
     * Clear the map of operations ids by testSet.
     */
    @Given("^les jeux de tests réinitialisés$")
    public void the_reinit_of_test_set() {
        operationIdsByTestSet.clear();
    }

    /**
     * @return tenant ID
     */
    public int getTenantId() {
        return tenantId;
    }

    /**
     * @return ingest client
     */
    public IngestExternalClient getIngestClient() {
        return ingestClient;
    }

    /**
     * @return access client
     */
    public AccessExternalClient getAccessClient() {
        return accessClient;
    }

    /**
     * @return admin client
     */
    public AdminExternalClient getAdminClient() {
        return adminClient;
    }

    /**
     * Workspace client
     *
     * @return workspaceClient
     */
    public WorkspaceClient getWorkspaceClient() {
        return workspaceClient;
    }

    /**
     * Workspace client
     *
     * @return logbookOperationsClient
     */
    public LogbookOperationsClient getLogbookOperationsClient() {
        return logbookOperationsClient;
    }

    /**
     * Storage client
     *
     * @return storageClient
     */
    public StorageClient getStorageClient() {
        return storageClient;
    }

    /**
     * @return operation ID
     */
    public String getOperationId() {
        return operationId;
    }

    /**
     * @param operationId operation ID
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getEliminationOperationId() {
        return eliminationOperationId;
    }

    public void setEliminationOperationId(String eliminationOperationId) {
        this.eliminationOperationId = eliminationOperationId;
    }

    /**
     * @return the dsl query
     */
    public String getQuery() {
        return query;
    }

    /**
     * @param query the query
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Get an operation id for the test set
     *
     * @param testSet test set identifier
     * @return operation id
     */
    public static String getOperationId(String testSet) {
        return operationIdsByTestSet.get(testSet);
    }

    /**
     * Set an operation id for a test set
     *
     * @param testSet test set identifier
     * @param operationId operation id
     */
    public static void setOperationId(String testSet, String operationId) {
        operationIdsByTestSet.put(testSet, operationId);
    }

    public String getNamedOperationId(String name) {
        return namedOperationIds.get(name);
    }

    public void setNamedOperationId(String name, String namedOperationId) {
        this.namedOperationIds.put(name, namedOperationId);
    }

    /**
     * @return unitId
     */
    public String getUnitId() {
        return unitId;
    }

    /**
     * @param unitId
     */
    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    /**
     * @param objectGroupId
     */
    public void setObjectGroupId(String objectGroupId) {
        this.objectGroupId = objectGroupId;
    }

    /**
     * @return objectGroupId
     */
    public String getObjectGroupId() {
        return objectGroupId;
    }

    /**
     * @return contractId
     */
    public String getContractId() {
        if (contractId == null) {
            return DEFAULT_ACCESS_CONTRACT_NAME;
        }
        return contractId;
    }

    /**
     * @param contractId
     * @return this
     */
    @Given("^J'utilise le contrat d'access (.*)$")
    public World setContractId(String contractId) {
        this.contractId = contractId;
        return this;
    }

    public String getApplicationSessionId() {
        return applicationSessionId;
    }

    public LogbookService getLogbookService() {
        return logbookService;
    }

    public AccessService getAccessService() {
        return accessService;
    }

    /**
     *
     */
    @After
    public void finish() {
        adminClient.close();
        accessClient.close();
        ingestClient.close();
        storageClient.close();
        workspaceClient.close();
        logbookOperationsClient.close();
        collectExternalClient.close();
    }

    /**
     * write operation ID on cucumber report
     *
     * @param scenario
     */
    @After
    public void writeOperationId(Scenario scenario) {
        scenario.write(operationId);
    }

    /**
     * @return base directory on .feature file
     */
    public String getBaseDirectory() {
        return baseDirectory;
    }

    private void configuration() {
        try {
            File confFile = PropertiesUtils.findFile(TNR_CONF);
            tnrClientConfiguration = PropertiesUtils.readYaml(confFile, TnrClientConfiguration.class);
            SanityChecker.checkParameter(baseDirectory);
        } catch (IOException | InvalidParseOperationException e) {
            LOGGER.error("Unable to load configuration File: {}" + TNR_CONF, e);
        }
    }

    /**
     * delete data before testing
     */
    private void purgeData() {
        try (IhmRecetteClient ihmRecetteClient = IhmRecetteClientFactory.getInstance().getClient()) {
            tnrClientConfiguration
                .getTenantsTest()
                .forEach(i -> {
                    try {
                        ihmRecetteClient.deleteTnrCollectionsTenant(i.toString());
                    } catch (VitamException e) {
                        // FAIL WHEN unable purge ?
                        LOGGER.error(
                            "Unable purge data " + i + " on tenant: " + i + Arrays.toString(e.getStackTrace())
                        );
                    }
                });
        }
    }

    private void configureLogbookClient() throws IOException {
        File confFile = PropertiesUtils.findFile("logbook-client.conf");
        ClientConfigurationImpl conf = PropertiesUtils.readYaml(confFile, ClientConfigurationImpl.class);
        LogbookOperationsClientFactory.changeMode(conf);
    }

    List<JsonNode> getResults() {
        return results;
    }

    void setResults(List<JsonNode> results) {
        this.results = results;
    }

    LogbookEvent getLogbookEvent() {
        return logbookEvent;
    }

    void setLogbookEvent(LogbookEvent logbookEvent) {
        this.logbookEvent = logbookEvent;
    }

    public AccessExternalClientV2 getAdminClientV2() {
        return adminClientV2;
    }

    public CollectExternalClient getCollectExternalClient() {
        return collectExternalClient;
    }

    public Path getSipFile() {
        return sipFile;
    }

    public void setSipFile(Path path) {
        this.sipFile = path;
    }

    public Path getDipFile() {
        return dipFile;
    }

    public void setDipFile(Path dipFile) {
        this.dipFile = dipFile;
    }

    public Path getTransferFile() {
        return transferFile;
    }

    public World setTransferFile(Path transferFile) {
        this.transferFile = transferFile;
        return this;
    }

    public Path getAtrFile() {
        return atrFile;
    }

    public World setAtrFile(Path atrFile) {
        this.atrFile = atrFile;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getTransactionId() {
        return TransactionId;
    }

    public void setTransactionId(String transactionId) {
        TransactionId = transactionId;
    }
}
