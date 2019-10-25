/*
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
 */
package fr.gouv.vitam.functional;

import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeRefence;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.integration.test.IngestInternalIT;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookElasticsearchAccess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * IngestContract
 */
public class IngestContractIT extends VitamRuleRunner {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestContractIT.class);
    private static final Integer TENANT_ID = 0;
    private static final String FILE_INGEST_CONTRACT_OK = "/functional-admin/ingest-contract/referential_contracts_ok.json";


    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(IngestInternalIT.class, mongoRule.getMongoDatabase().getName(),
            elasticsearchRule.getClusterName(),
            Sets.newHashSet(
                WorkspaceMain.class,
                StorageMain.class,
                LogbookMain.class,
                MetadataMain.class,
                AccessInternalMain.class,
                AdminManagementMain.class,
                IngestInternalMain.class,
                ProcessManagementMain.class,
                WorkerMain.class

            ));

    private static LogbookElasticsearchAccess esClient;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);

        // ES client
        final List<ElasticsearchNode> esNodes = new ArrayList<>();
        esNodes.add(new ElasticsearchNode("localhost", elasticsearchRule.getTcpPort()));
        esClient = new LogbookElasticsearchAccess(elasticsearchRule.getClusterName(), esNodes);

        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        storageClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.PRODUCTION);
        runAfter();
        VitamClientFactory.resetConnections();
    }


    @Before
    public void setUpBefore() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(newOperationLogbookGUID(0));
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_ingest_contract_ok() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        InputStream contract = getClass().getResourceAsStream(FILE_INGEST_CONTRACT_OK);
        AdminManagementClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_FUNCTIONAL_ADMIN);
        LogbookOperationsClientFactory.getInstance().changeServerPort(runner.PORT_SERVICE_LOGBOOK);
        // When
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            LogbookOperationsClient logbookOperationsClient = LogbookOperationsClientFactory.getInstance().getClient()) {
            Response.Status status = client.importIngestContracts(
                getFromStringAsTypeRefence(JsonHandler.getFromInputStream(contract).toString(),
                new TypeReference<List<IngestContractModel>>() {}));
            Select select = new Select();
            select.setQuery(QueryHelper.eq("evType", "STP_IMPORT_INGEST_CONTRACT"));
            final JsonNode result = logbookOperationsClient.selectOperation(select.getFinalSelect());
            // Then
            JsonNode events = result.get("$results").get(0).get("events");
            assertThat(events.isArray());
            assertThat(events.size()).isEqualTo(2);
            assertThat(status.getStatusCode()).isEqualTo(201);
        }
    }
}
