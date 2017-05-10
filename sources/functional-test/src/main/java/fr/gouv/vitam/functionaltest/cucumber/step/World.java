/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.cucumber.step;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.client.IhmRecetteClient;
import fr.gouv.vitam.client.IhmRecetteClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.functionaltest.configuration.TnrClientConfiguration;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Fail;

import java.io.File;
import java.io.IOException;

public class World {

    public static final String TNR_BASE_DIRECTORY = "tnrBaseDirectory";

    private static final String TNR_CONF = "tnr.conf";
    public static final String DEFAULT_ACCESS_CONTRACT_NAME = "SIA archives nationales";


    private int tenantId;
    private static boolean beforeTest = true;


    private String contractId;

    /**
     * id of the operation
     */
    private String operationId;

    /**
     * ingest external client
     */
    private IngestExternalClient ingestClient;

    /**
     * access eternal client
     */
    private AccessExternalClient accessClient;

    /**
     * admin eternal client
     */
    private AdminExternalClient adminClient;
    /**
     * Storage Client
     */
    StorageClient storageClient;
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
    private String baseDirectory = System.getProperty(TNR_BASE_DIRECTORY);

    @Before
    public void init() throws IOException {
        configuration();
        if (beforeTest) {
            purgeData();
            beforeTest = false;
        }
        ingestClient = IngestExternalClientFactory.getInstance().getClient();
        accessClient = AccessExternalClientFactory.getInstance().getClient();
        adminClient = AdminExternalClientFactory.getInstance().getClient();
        storageClient = StorageClientFactory.getInstance().getClient();
        WorkspaceClientFactory.changeMode(tnrClientConfiguration.getUrlWorkspace());
        workspaceClient = WorkspaceClientFactory.getInstance().getClient();
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
     * Workspace  client
     *
     * @return
     */
    public WorkspaceClient getWorkspaceClient() {
        return workspaceClient;
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

    /**
     * @return
     */
    public String getContractId() {
        if (contractId == null) {
            return DEFAULT_ACCESS_CONTRACT_NAME;
        }
        return contractId;
    }

    /**
     * @param contractId
     * @return
     */
    public World setContractId(String contractId) {
        this.contractId = contractId;
        return this;
    }

    @After
    public void finish() {
        adminClient.close();
        accessClient.close();
        ingestClient.close();
        storageClient.close();
        workspaceClient.close();
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
        File confFile = null;
        try {
            confFile = PropertiesUtils.findFile(TNR_CONF);
            tnrClientConfiguration = PropertiesUtils.readYaml(confFile, TnrClientConfiguration.class);

        } catch (IOException e) {
            Fail.fail("Unable to load configuration File: \n"+e.getMessage());
        }

    }

    private void purgeData() {
        try(IhmRecetteClient ihmRecetteClient = IhmRecetteClientFactory.getInstance().getClient() ) {
            tnrClientConfiguration.getTenantsTest().stream().forEach((i) -> {
                try {
                    ihmRecetteClient.deleteTnrCollectionsTenant(i.toString());
                } catch (VitamException e) {
                    // FAIL WHEN unable purge ?
                    Fail.fail("Unable purge data "+i.toString()+" on tenant: " + i+e.getStackTrace());
                }
            });
        }
    }
}
