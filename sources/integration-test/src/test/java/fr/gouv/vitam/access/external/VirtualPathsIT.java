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
package fr.gouv.vitam.access.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.client.AdminExternalClientFactory;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.antivirus.rest.AntivirusMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.FacetResult;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.common.database.builder.facet.FacetHelper.terms;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.metadata.core.MetaDataImpl.SNAPSHOT_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class VirtualPathsIT extends VitamRuleRunner {

    private static final Integer TENANT_ID = 0;
    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";
    private static final String ACCESS_CONTRACT = "aName3";

    private static final String TEST_VIRTUAL_TREE_SIP = "sip/virtual_tree_sip.zip";

    private static String ingestOperationId;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        VirtualPathsIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            WorkerMain.class,
            AdminManagementMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            ProcessManagementMain.class,
            AccessInternalMain.class,
            IngestInternalMain.class,
            AccessExternalMain.class,
            IngestExternalMain.class,
            AntivirusMain.class
        )
    );

    private static AccessExternalClient accessExternalClient;
    private static IngestExternalClient ingestExternalClient;
    private static AdminExternalClient adminExternalClient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        accessExternalClient = AccessExternalClientFactory.getInstance().getClient();
        ingestExternalClient = IngestExternalClientFactory.getInstance().getClient();
        adminExternalClient = AdminExternalClientFactory.getInstance().getClient();

        new DataLoader("integration-ingest-internal").prepareData();

        ingest_sip_with_virtual_trees();
    }

    public static void ingest_sip_with_virtual_trees() throws Exception {
        try (InputStream inputStream = PropertiesUtils.getResourceAsStream(TEST_VIRTUAL_TREE_SIP)) {
            RequestResponse<Void> response = ingestExternalClient.ingest(
                new VitamContext(TENANT_ID)
                    .setApplicationSessionId(APPLICATION_SESSION_ID)
                    .setAccessContract(ACCESS_CONTRACT),
                inputStream,
                DEFAULT_WORKFLOW.name(),
                ProcessAction.RESUME.name()
            );

            assertThat(response.isOk()).as(JsonHandler.unprettyPrint(response)).isTrue();

            ingestOperationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            assertThat(ingestOperationId).as("%s not found for request", X_REQUEST_ID).isNotNull();

            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(adminExternalClient);
            boolean process_timeout = vitamPoolingClient.wait(
                TENANT_ID,
                ingestOperationId,
                ProcessState.COMPLETED,
                1800,
                1_000L,
                TimeUnit.MILLISECONDS
            );
            if (!process_timeout) {
                Assertions.fail(
                    "Sip processing not finished: operation (" + ingestOperationId + "). Timeout exceeded."
                );
            }
        }
    }

    @Test
    @RunWithCustomExecutor
    public void testSearchVirtualTreeSip() throws Exception {
        VitamContext vitamContext = new VitamContext(TENANT_ID)
            .setApplicationSessionId(APPLICATION_SESSION_ID)
            .setAccessContract(ACCESS_CONTRACT);

        SelectMultiQuery dslRequest = new SelectMultiQuery();
        dslRequest.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), ingestOperationId));
        dslRequest.addFacets(terms("virtualPaths", "#vups", 5, FacetOrder.ASC));
        dslRequest.addProjection(
            JsonHandler.createObjectNode().put("Title", 1).put("FilePlanPosition", 1).put("#vups", 1)
        );

        // WHEN
        RequestResponse<JsonNode> resultUnit = accessExternalClient.selectUnits(
            vitamContext,
            dslRequest.getFinalSelect()
        );

        // THEN
        assertTrue(resultUnit.isOk());

        List<JsonNode> unitsResults = ((RequestResponseOK<JsonNode>) resultUnit).getResults();
        List<FacetResult> facetResults = ((RequestResponseOK<JsonNode>) resultUnit).getFacetResults();

        assertThat(unitsResults).hasSize(8);
        assertThat(facetResults).hasSize(1);

        JsonNode auUnit1Txt = unitsResults
            .stream()
            .filter(unit -> "AU 1.txt".equals(unit.get("Title").asText()))
            .findAny()
            .get();

        assertThat(auUnit1Txt.get("#vups")).isEqualTo(JsonHandler.createArrayNode().add("/Dossier 1"));
        Map<String, Long> virtualPathsStats = facetResults
            .stream()
            .filter(result -> "virtualPaths".equals(result.getName()))
            .map(FacetResult::getBuckets)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(Collectors.toMap(FacetBucket::getValue, FacetBucket::getCount));

        assertThat(virtualPathsStats.get("/Dossier 1")).isEqualTo(4);
        assertThat(virtualPathsStats.get("/Dossier 1/Dossier 1.2")).isEqualTo(1);
        assertThat(virtualPathsStats.get("/Dossier 1/Dossier 1.1")).isEqualTo(1);
    }

    @After
    public void after() {
        runAfterMongo(Set.of(SNAPSHOT_COLLECTION));
    }

    @AfterClass
    public static void tearDownAfterClass() {
        if (accessExternalClient != null) {
            accessExternalClient.close();
        }
        if (ingestExternalClient != null) {
            ingestExternalClient.close();
        }
        if (adminExternalClient != null) {
            adminExternalClient.close();
        }

        handleAfterClass();
        runAfter();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
    }
}
