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
package fr.gouv.vitam.preservation;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClient;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;

import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.MOCK;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ingest Internal integration test
 */
public class PreservationIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final String contractId = "contract";

    private static final HashSet<Class> servers = Sets.newHashSet(
        AccessInternalMain.class,
        AdminManagementMain.class,
        ProcessManagementMain.class,
        LogbookMain.class,
        WorkspaceMain.class,
        MetadataMain.class,
        WorkerMain.class,
        LogbookMain.class,
        IngestInternalMain.class,
        BatchReportMain.class
    );

    private static final String mongoName = mongoRule.getMongoDatabase().getName();
    private static final String esName = elasticsearchRule.getClusterName();

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(PreservationIT.class, mongoName, esName, servers);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        String configurationPath = PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(MOCK);
        new DataLoader("integration-ingest-internal").prepareData();

    }

    @AfterClass
    public static void tearDownAfterClass() {
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        File griffinsExecFolder = PropertiesUtils.getResourceFile("preservation/");
        VitamConfiguration.setVitamGriffinExecFolder(griffinsExecFolder.getAbsolutePath());
        VitamConfiguration.setVitamGriffinInputFilesFolder(tmpGriffinFolder.getRoot().getAbsolutePath());

        AccessInternalClientFactory factory = AccessInternalClientFactory.getInstance();
        factory.changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
        factory.setVitamClientType(PRODUCTION);

        Path griffinExecutable = griffinsExecFolder.toPath().resolve("griffinId").resolve("griffin");
        griffinExecutable.toFile().setExecutable(true);

        AccessContractModel contract = new AccessContractModel();
        contract.setName(contractId);
        contract.setIdentifier(contractId);
        contract.setStatus(ActivationStatus.ACTIVE);
        contract.setEveryOriginatingAgency(true);
        contract.setCreationdate("10/12/1800");
        contract.setActivationdate("10/12/1800");
        contract.setDeactivationdate("31/12/4200");

        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();

        client.importAccessContracts(Collections.singletonList(contract));
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_preservation_workflow_without_error() throws Exception {
        // Given
        AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();

        AccessInternalClientFactory factory = AccessInternalClientFactory.getInstance();
        AccessInternalClient client = factory.getClient();

        try {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            VitamThreadUtils.getVitamSession().setContractId(contractId);
            VitamThreadUtils.getVitamSession().setContextId("Context_IT");
            VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

            FileInputStream distributionFile = new FileInputStream(PropertiesUtils.getResourceFile("preservation/distribution_preservation_test.jsonL"));

            client.startPreservation(distributionFile);
            wait(operationGuid.toString());

        // When
            ArrayNode jsonNode = (ArrayNode) accessClient.selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

         // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText()).allMatch(outcome -> outcome.equals(StatusCode.OK.name()));
        } finally {
            accessClient.close();
            client.close();
        }
    }

    private void wait(String operationId) {
        int nbTry = 0;
        ProcessingManagementClient processingClient = ProcessingManagementClientFactory.getInstance().getClient();
        while (!processingClient.isOperationCompleted(operationId)) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            }
            if (nbTry == NB_TRY)
                break;
            nbTry++;
        }
    }

}
