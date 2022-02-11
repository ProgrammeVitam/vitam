/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.access.external.client.AccessExternalClientFactory;
import fr.gouv.vitam.access.external.rest.AccessExternalMain;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.external.rest.IngestExternalMain;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import io.restassured.RestAssured;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.VitamServerRunner.ACCESS_EXTERNAL_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.ACCESS_INTERNAL_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.INGEST_EXTERNAL_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.INGEST_INTERNAL_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.LOGBOOK_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.METADATA_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_LOGBOOK;
import static fr.gouv.vitam.common.VitamServerRunner.PROCESSING_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.WORKER_PATH;
import static fr.gouv.vitam.common.VitamServerRunner.WORKSPACE_PATH;
import static fr.gouv.vitam.common.model.objectgroup.FileInfoModel.FILENAME;
import static fr.gouv.vitam.common.model.objectgroup.FileInfoModel.LAST_MODIFIED;
import static fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse.OPERATIONS;
import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AccessExternalIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final String APPLICATION_SESSION_ID = "ApplicationSessionId";
    private static final String ACCESS_CONTRACT = "aName3";

    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON =
        "integration-processing/mass-update/unit_00.json";
    private static final String INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON =
        "integration-processing/mass-update/unit_lfc_00.json";

    @ClassRule
    public static VitamServerRunner runner =
        new VitamServerRunner(AccessExternalIT.class, mongoRule.getMongoDatabase().getName(),
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
                IngestExternalMain.class));

    private static AccessExternalClient accessExternalClient;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
        accessExternalClient = AccessExternalClientFactory.getInstance().getClient();

        new DataLoader("integration-ingest-internal").prepareData();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        runAfter();
        fr.gouv.vitam.common.external.client.VitamClientFactory.resetConnections();
        fr.gouv.vitam.common.client.VitamClientFactory.resetConnections();
    }

    @RunWithCustomExecutor
    @Test
    public void selectUnitsWithTrackTotalHitsInDSL() throws Exception {
        // given
        insertUnitAndLFC(INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_00_JSON,
            INTEGRATION_PROCESSING_MASS_UPDATE_UNIT_LFC_00_JSON);

        VitamContext vitamContext = new VitamContext(tenantId)
            .setApplicationSessionId(APPLICATION_SESSION_ID)
            .setAccessContract(ACCESS_CONTRACT);

        // WHEN
        RequestResponse<JsonNode> unitsWithPrecision = getUnitsWithTrackTotalHits(true, vitamContext);
        RequestResponse<JsonNode> unitsWithoutPrecision = getUnitsWithTrackTotalHits(false, vitamContext);

        // THEN
        assertFalse(unitsWithPrecision.isOk());
        assertThat(unitsWithPrecision.getStatus()).isEqualTo(Status.UNAUTHORIZED.getStatusCode());
        assertThat(((VitamError<JsonNode>) unitsWithPrecision).getDescription()).contains(
            "$track_total_hits is not authorized!");

        List<JsonNode> resultsWithoutPrecision = ((RequestResponseOK<JsonNode>) unitsWithoutPrecision).getResults();
        assertNotNull(resultsWithoutPrecision);
        assertThat(resultsWithoutPrecision.size()).isGreaterThan(0);
    }


    @RunWithCustomExecutor
    @Test
    public void selectObjectGroupsByDSLWithBlackListedFields() throws Exception {
        // given
        VitamContext vitamContext = new VitamContext(tenantId)
            .setApplicationSessionId(APPLICATION_SESSION_ID)
            .setAccessContract(ACCESS_CONTRACT);

        final List<String> declaredBlackListedFieldsForGotInMetadatConf =
            List.of(FILENAME, LAST_MODIFIED, OPERATIONS);
        final String OBJECTGROUP_RESOURCE_FILE = "database/got.json";

        List<Document> gots = JsonHandler.getFromFileAsTypeReference(PropertiesUtils.getResourceFile(
            OBJECTGROUP_RESOURCE_FILE), new TypeReference<>() {
        });
        VitamRepositoryFactory.get().getVitamMongoRepository(MetadataCollections.OBJECTGROUP.getVitamCollection())
            .save(gots);
        VitamRepositoryFactory.get().getVitamESRepository(MetadataCollections.OBJECTGROUP.getVitamCollection(),
            metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP))
            .save(gots);

        final String queryDsl =
            "{ \"$query\" : [ { \"$exists\": \"#id\" } ], " +
                " \"$filter\": { }, " +
                " \"$projection\" : { } " +
                " }";

        RequestResponse<JsonNode> response =
            accessExternalClient.selectObjects(vitamContext, JsonHandler.getFromString(queryDsl));
        // THEN
        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        RequestResponseOK<JsonNode> jsonNode = (RequestResponseOK<JsonNode>) response;
        jsonNode.getResults().forEach(result -> {
            declaredBlackListedFieldsForGotInMetadatConf.forEach(
                field -> assertFalse(result.toString().contains(field)));
        });
    }

    private void insertUnitAndLFC(final String unitFile, final String lfcFile)
        throws InvalidParseOperationException, FileNotFoundException, MetaDataExecutionException {
        List<Unit> units = JsonHandler.getFromFileAsTypeReference(PropertiesUtils.getResourceFile(
            unitFile),
            new TypeReference<>() {
            });
        MetadataCollections.UNIT.<Unit>getVitamCollection().getCollection().insertMany(units);
        MetadataCollections.UNIT.getEsClient()
            .insertFullDocuments(MetadataCollections.UNIT, tenantId, units);


        List<LogbookLifeCycleUnit> unitsLfc = JsonHandler.getFromFileAsTypeReference(PropertiesUtils.getResourceFile(
            lfcFile),
            new TypeReference<>() {
            });

        LogbookCollections.LIFECYCLE_UNIT.<LogbookLifeCycleUnit>getVitamCollection().getCollection()
            .insertMany(unitsLfc);
    }

    private RequestResponse<JsonNode> getUnitsWithTrackTotalHits(boolean shouldTrackTotalHits,
        VitamContext vitamContext)
        throws VitamClientException, InvalidParseOperationException, InvalidCreateOperationException {
        SelectMultiQuery select = new SelectMultiQuery();
        select.addQueries(QueryHelper.eq("Title", "sous fonds"));
        select.trackTotalHits(shouldTrackTotalHits);
        select.setProjection(
            JsonHandler.createObjectNode().set(
                BuilderToken.PROJECTION.FIELDS.name(),
                JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), 1)));
        return accessExternalClient.selectUnits(vitamContext, select.getFinalSelect());
    }
}
