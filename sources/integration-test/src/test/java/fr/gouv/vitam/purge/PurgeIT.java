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
package fr.gouv.vitam.purge;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.core.backup.BackupService;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.purge.PurgeDeleteService;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import org.bson.Document;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.MetadataStorageHelper.getGotWithLFC;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections.LIFECYCLE_OBJECTGROUP;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections.LIFECYCLE_UNIT;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECTGROUP;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.UNIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PurgeIT integration test
 */
public class PurgeIT extends VitamRuleRunner {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PurgeIT.class);
    private StorageClient storageClient;
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private MetaDataClient metaDataClient;

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(
        PurgeIT.class,
        mongoRule.getMongoDatabase().getName(),
        ElasticsearchRule.getClusterName(),
        Sets.newHashSet(
            MetadataMain.class,
            LogbookMain.class,
            WorkspaceMain.class,
            StorageMain.class,
            DefaultOfferMain.class
        )
    );

    private final PurgeDeleteService purgeDeleteService = new PurgeDeleteService();
    private final BackupService backupService = new BackupService();

    @BeforeClass
    public static void beforeClass() throws Exception {
        handleBeforeClass(Arrays.asList(0, 1), Collections.emptyMap());
    }

    @AfterClass
    public static void afterClass() {
        handleAfterClass();
    }

    @Before
    public void setUp() {
        storageClient = StorageClientFactory.getInstance().getClient();
        logbookLifeCyclesClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
        metaDataClient = MetaDataClientFactory.getInstance().getClient();
    }

    @RunWithCustomExecutor
    @Test
    public void should_delete_units_documents() throws Exception {
        final GUID operationGuid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);
        VitamThreadUtils.getVitamSession().setTenantId(0);

        String id_1 = "aeaqaaaaaqhdytymabdeialenehzpbyaaajq";
        String id_2 = "aeaqaaaaaqhdytymabdeialenehzpbqaaaaq";
        String id_3 = "aeaqaaaaaqhdytymabdeialenehzphiaaaeq";

        String UNIT_1 = "purge/unit_1.json";
        String UNIT_2 = "purge/unit_2.json";
        String UNIT_3 = "purge/unit_3.json";

        String LFC_UNIT_1 = "purge/lfc_unit_1.json";
        String LFC_UNIT_2 = "purge/lfc_unit_2.json";
        String LFC_UNIT_3 = "purge/lfc_unit_3.json";

        JsonNode unit_1 = JsonHandler.getFromFile(PropertiesUtils.findFile(UNIT_1));
        JsonNode unit_2 = JsonHandler.getFromFile(PropertiesUtils.findFile(UNIT_2));
        JsonNode unit_3 = JsonHandler.getFromFile(PropertiesUtils.findFile(UNIT_3));

        JsonNode lfc_unit_1 = JsonHandler.getFromFile(PropertiesUtils.findFile(LFC_UNIT_1));
        JsonNode lfc_unit_2 = JsonHandler.getFromFile(PropertiesUtils.findFile(LFC_UNIT_2));
        JsonNode lfc_unit_3 = JsonHandler.getFromFile(PropertiesUtils.findFile(LFC_UNIT_3));

        // saveInRepositories in vitam
        saveInVitam(id_1, unit_1, lfc_unit_1, UNIT);
        saveInVitam(id_2, unit_2, lfc_unit_2, UNIT);
        saveInVitam(id_3, unit_3, lfc_unit_3, UNIT);

        //verify that unit is saved in mongo
        assertThat(getMongoRawUnit(id_1).get("_id").textValue()).isEqualTo(id_1);
        assertThat(getMongoRawUnit(id_2).get("_id").textValue()).isEqualTo(id_2);
        assertThat(getMongoRawUnit(id_3).get("_id").textValue()).isEqualTo(id_3);

        //verify that Lfc is saved in mongo
        assertThat(getRawUnitLifeCycleById(id_1).get("_id").textValue()).isEqualTo(id_1);
        assertThat(getRawUnitLifeCycleById(id_2).get("_id").textValue()).isEqualTo(id_2);
        assertThat(getRawUnitLifeCycleById(id_3).get("_id").textValue()).isEqualTo(id_3);

        //elastic ok   for units         verify that unit is saved in elastic
        assertThat(getUnitIdByTitleForElastic("Archives postérieures à 1789")).isEqualTo(id_1);
        assertThat(getUnitIdByTitleForElastic("Archives antérieures à 1789")).isEqualTo(id_2);
        assertThat(getUnitIdByTitleForElastic("Archives privées")).isEqualTo(id_3);

        //assert storage verify that unit is saved in storage
        checkFileInStorage(UNIT, id_1 + ".json", true);
        checkFileInStorage(UNIT, id_2 + ".json", true);
        checkFileInStorage(UNIT, id_3 + ".json", true);

        //when delete units
        Map<String, String> unitIdsWithStrategies = ImmutableMap.of(id_1, "default", id_2, "default", id_3, "default");
        purgeDeleteService.deleteUnits(unitIdsWithStrategies);

        //then nothing in units mongo
        assertThatMongoRawUnitNotExists(id_1);
        assertThatMongoRawUnitNotExists(id_2);
        assertThatMongoRawUnitNotExists(id_3);

        // then nothing in lfc mongo
        assertThatThrownBy(() -> getRawUnitLifeCycleById(id_1)).hasMessage("Logbook not found ");
        assertThatThrownBy(() -> getRawUnitLifeCycleById(id_2)).hasMessage("Logbook not found ");
        assertThatThrownBy(() -> getRawUnitLifeCycleById(id_3)).hasMessage("Logbook not found ");
        // then nothing in units  elastic
        assertThatThrownBy(() -> getUnitIdByTitleForElastic("Archives postérieures à 1789")).hasMessage("Not found");
        assertThatThrownBy(() -> getUnitIdByTitleForElastic("Archives antérieures à 1789")).hasMessage("Not found");
        assertThatThrownBy(() -> getUnitIdByTitleForElastic("Archives privées")).hasMessage("Not found");

        // then nothing in units + lfc   storage
        checkFileInStorage(UNIT, id_1 + ".json", false);
        checkFileInStorage(UNIT, id_2 + ".json", false);
        checkFileInStorage(UNIT, id_3 + ".json", false);

        // Check offer log
        checkDeletedFilesInOfferLogs(UNIT, id_1 + ".json", id_2 + ".json", id_3 + ".json");
    }

    @RunWithCustomExecutor
    @Test
    public void should_delete_objectGroups_documents() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        final GUID operationGuid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationGuid);

        String id_1 = "aebaaaaaaqhad455abryqalenekbcnyaaaaq";
        String id_2 = "aebaaaaaaqhad455abryqalenekbcqqaaaaq";
        String id_3 = "aebaaaaaaqhad455abryqalenekbcoiaaabq";

        String OBJECT_GROUP_1 = "purge/objectGroup_1.json";
        String OBJECT_GROUP_2 = "purge/objectGroup_2.json";
        String OBJECT_GROUP_3 = "purge/objectGroup_3.json";

        String LFC_OBJECT_GROUP_1 = "purge/lfc_objectGroup_1.json";
        String LFC_OBJECT_GROUP_2 = "purge/lfc_objectGroup_2.json";
        String LFC_OBJECT_GROUP_3 = "purge/lfc_objectGroup_3.json";

        JsonNode object_group_1 = JsonHandler.getFromFile(PropertiesUtils.findFile(OBJECT_GROUP_1));
        JsonNode object_group_2 = JsonHandler.getFromFile(PropertiesUtils.findFile(OBJECT_GROUP_2));
        JsonNode object_group_3 = JsonHandler.getFromFile(PropertiesUtils.findFile(OBJECT_GROUP_3));

        JsonNode lfc_object_group_1 = JsonHandler.getFromFile(PropertiesUtils.findFile(LFC_OBJECT_GROUP_1));
        JsonNode lfc_object_group_2 = JsonHandler.getFromFile(PropertiesUtils.findFile(LFC_OBJECT_GROUP_2));
        JsonNode lfc_object_group_3 = JsonHandler.getFromFile(PropertiesUtils.findFile(LFC_OBJECT_GROUP_3));

        // saveInRepositories in vitam
        saveInVitam(id_1, object_group_1, lfc_object_group_1, OBJECTGROUP);
        saveInVitam(id_2, object_group_2, lfc_object_group_2, OBJECTGROUP);
        saveInVitam(id_3, object_group_3, lfc_object_group_3, OBJECTGROUP);

        //verify that unit is saved in mongo
        assertThat(getMongoRawObjectGroup(id_1).get("_id").textValue()).isEqualTo(id_1);
        assertThat(getMongoRawObjectGroup(id_2).get("_id").textValue()).isEqualTo(id_2);
        assertThat(getMongoRawObjectGroup(id_3).get("_id").textValue()).isEqualTo(id_3);

        //verify that Lfc is saved in mongo
        assertThat(getRawObjectLifeCycleById(id_1).get("_id").textValue()).isEqualTo(id_1);
        assertThat(getRawObjectLifeCycleById(id_2).get("_id").textValue()).isEqualTo(id_2);
        assertThat(getRawObjectLifeCycleById(id_3).get("_id").textValue()).isEqualTo(id_3);

        //elastic ok   for units         verify that objectGroup is saved in elastic
        assertThat(getObjectGrpByIdForElastic(id_1)).isEqualTo(id_1);
        assertThat(getObjectGrpByIdForElastic(id_2)).isEqualTo(id_2);
        assertThat(getObjectGrpByIdForElastic(id_3)).isEqualTo(id_3);

        //assert storage verify that unit is saved in storage
        checkFileInStorage(OBJECTGROUP, id_1 + ".json", true);
        checkFileInStorage(OBJECTGROUP, id_2 + ".json", true);
        checkFileInStorage(OBJECTGROUP, id_3 + ".json", true);

        //when deletion
        Map<String, String> gotIdsWithStrategies = ImmutableMap.of(id_1, "default", id_2, "default", id_3, "default");
        purgeDeleteService.deleteObjectGroups(gotIdsWithStrategies);

        //then nothing in units mongo
        assertThatMongoRawObjectGroupNotExists(id_1);
        assertThatMongoRawObjectGroupNotExists(id_2);
        assertThatMongoRawObjectGroupNotExists(id_3);

        // then nothing in lfc mongo
        assertThatThrownBy(() -> getRawObjectLifeCycleById(id_1)).hasMessage("Logbook not found ");
        assertThatThrownBy(() -> getRawObjectLifeCycleById(id_2)).hasMessage("Logbook not found ");
        assertThatThrownBy(() -> getRawObjectLifeCycleById(id_3)).hasMessage("Logbook not found ");
        // then nothing in units  elastic
        assertThatThrownBy(() -> getObjectGrpByIdForElastic(id_1)).hasMessage("Not found");
        assertThatThrownBy(() -> getObjectGrpByIdForElastic(id_2)).hasMessage("Not found");
        assertThatThrownBy(() -> getObjectGrpByIdForElastic(id_3)).hasMessage("Not found");

        // then nothing in units + lfc   storage
        checkFileInStorage(OBJECTGROUP, id_1 + ".json", false);
        checkFileInStorage(OBJECTGROUP, id_2 + ".json", false);
        checkFileInStorage(OBJECTGROUP, id_3 + ".json", false);

        // Check offer log
        checkDeletedFilesInOfferLogs(OBJECTGROUP, id_1 + ".json", id_2 + ".json", id_3 + ".json");
    }

    private void checkDeletedFilesInOfferLogs(DataCategory dataCategory, String... fileNames)
        throws StorageServerClientException {
        RequestResponse<OfferLog> offerLogRequestResponse = storageClient.getOfferLogs(
            VitamConfiguration.getDefaultStrategy(),
            null,
            dataCategory,
            null,
            100000,
            Order.ASC
        );
        assertThat(offerLogRequestResponse.isOk()).isTrue();
        List<OfferLog> offerLogs = ((RequestResponseOK<OfferLog>) offerLogRequestResponse).getResults();

        List<String> deletedFileNamesInOfferLogs = offerLogs
            .stream()
            .filter(offerLog -> offerLog.getAction() == OfferLogAction.DELETE)
            .map(OfferLog::getFileName)
            .collect(Collectors.toList());

        assertThat(deletedFileNamesInOfferLogs).containsExactlyInAnyOrder(fileNames);
    }

    private void saveInVitam(String id, JsonNode vitamDocument, JsonNode lfc, DataCategory category) {
        try {
            switch (category) {
                case UNIT:
                    saveInRepositories(vitamDocument, lfc, MetadataCollections.UNIT, LIFECYCLE_UNIT);
                    break;
                case OBJECTGROUP:
                    saveInRepositories(vitamDocument, lfc, MetadataCollections.OBJECTGROUP, LIFECYCLE_OBJECTGROUP);
                    break;
                default:
                    throw new UnsupportedOperationException(category.name());
            }

            JsonNode docWithLfc = getGotWithLFC(vitamDocument, lfc);

            byte[] bytes = JsonHandler.unprettyPrint(docWithLfc).getBytes();

            backupService.backup(new ByteArrayInputStream(bytes), category, id + ".json");
        } catch (Exception e) {
            LOGGER.error(e);
        }
    }

    private void saveInRepositories(
        JsonNode vitamDocument,
        JsonNode lfc,
        MetadataCollections objectgroup,
        LogbookCollections collection
    ) throws DatabaseException {
        VitamRepositoryFactory.get()
            .getVitamMongoRepository(objectgroup.getVitamCollection())
            .save(Document.parse(vitamDocument.toString()));

        VitamRepositoryFactory.get()
            .getVitamESRepository(
                objectgroup.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(objectgroup)
            )
            .save(Document.parse(vitamDocument.toString()));

        VitamRepositoryFactory.get()
            .getVitamMongoRepository(collection.getVitamCollection())
            .save(Document.parse(lfc.toString()));
    }

    private JsonNode getMongoRawUnit(String id) throws VitamClientException {
        return ((RequestResponseOK<JsonNode>) metaDataClient.getUnitByIdRaw(id)).getFirstResult();
    }

    private void assertThatMongoRawUnitNotExists(String id) throws VitamClientException {
        RequestResponse<JsonNode> response = metaDataClient.getUnitByIdRaw(id);
        assertThat(response.isOk()).isFalse();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private JsonNode getMongoRawObjectGroup(String id) throws VitamClientException {
        return ((RequestResponseOK<JsonNode>) metaDataClient.getObjectGroupByIdRaw(id)).getFirstResult();
    }

    private void assertThatMongoRawObjectGroupNotExists(String id) throws VitamClientException {
        RequestResponse<JsonNode> response = metaDataClient.getObjectGroupByIdRaw(id);
        assertThat(response.isOk()).isFalse();
        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private String getUnitIdByTitleForElastic(String title) throws Exception {
        Select select = new Select();
        select.setQuery(QueryHelper.eq("Title", title));

        JsonNode $results = metaDataClient.selectUnits(select.getFinalSelect()).get("$results");
        if ($results != null && $results.elements().hasNext()) {
            return $results.get(0).get("#id").textValue();
        }
        throw new VitamException("Not found");
    }

    private String getObjectGrpByIdForElastic(String id) throws Exception {
        Optional<Document> document = VitamRepositoryFactory.get()
            .getVitamESRepository(
                MetadataCollections.OBJECTGROUP.getVitamCollection(),
                metadataIndexManager.getElasticsearchIndexAliasResolver(MetadataCollections.OBJECTGROUP)
            )
            .getByID(id, 0);
        if (document.isPresent()) {
            return id;
        }
        throw new VitamException("Not found");
    }

    private void checkFileInStorage(DataCategory dataCategory, String filename, boolean shouldExist)
        throws StorageNotFoundClientException, StorageServerClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            List<String> offers = storageClient.getOffers(VitamConfiguration.getDefaultStrategy());
            JsonNode information = storageClient.getInformation(
                VitamConfiguration.getDefaultStrategy(),
                dataCategory,
                filename,
                offers,
                false
            );
            boolean fileFound = information.size() > 0;
            assertThat(fileFound).isEqualTo(shouldExist);
        }
    }

    private JsonNode getRawUnitLifeCycleById(String id) throws LogbookClientException, InvalidParseOperationException {
        return logbookLifeCyclesClient.getRawUnitLifeCycleById(id);
    }

    private JsonNode getRawObjectLifeCycleById(String id)
        throws LogbookClientException, InvalidParseOperationException {
        return logbookLifeCyclesClient.getRawObjectGroupLifeCycleById(id);
    }
}
