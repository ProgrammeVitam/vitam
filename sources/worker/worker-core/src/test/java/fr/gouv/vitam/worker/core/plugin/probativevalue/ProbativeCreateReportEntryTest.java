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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbStorageModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportEntry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Collections;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.plugin.StoreObjectGroupActionPlugin.STORING_OBJECT_TASK_ID;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationStorageBinaryPlugin.MESSAGE_DIGEST;
import static fr.gouv.vitam.worker.core.plugin.preservation.TestWorkerParameter.TestWorkerParameterBuilder.workerParameterBuilder;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.ProbativeCreateReportEntry.NO_BINARY_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ProbativeCreateReportEntryTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private StorageClientFactory storageClientFactory;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @InjectMocks
    private ProbativeCreateReportEntry probativeCreateReportEntry;

    private MetaDataClient metaDataClient = mock(MetaDataClient.class);
    private StorageClient storageClient = mock(StorageClient.class);
    private LogbookOperationsClient logbookOperationsClient = mock(LogbookOperationsClient.class);
    private LogbookLifeCyclesClient logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        given(storageClientFactory.getClient()).willReturn(storageClient);
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        given(logbookLifeCyclesClientFactory.getClient()).willReturn(logbookLifeCyclesClient);
    }

    @Test
    public void should_return_item_status_FATAL_with_empty_ProbativeReportEntry_when_cannot_found_GOT_from_database() throws Exception {
        // Given
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.set("unitIds", objectMapper.createArrayNode().add("unitId"));
        metadata.put("usageVersion", "usageVersion");

        String objectGroupId = "objectGroupId";
        WorkerParameters param = workerParameterBuilder()
            .withObjectName(objectGroupId)
            .withObjectMetadata(metadata)
            .build();

        File reportFile = tempFolder.newFile();
        TestHandlerIO handler = new TestHandlerIO();
        handler.setNewLocalFile(reportFile);

        given(metaDataClient.getObjectGroupByIdRaw(objectGroupId)).willReturn(new VitamError("ERROR"));

        // When
        ItemStatus itemStatus = probativeCreateReportEntry.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(FATAL);
        assertThat(objectMapper.readValue(handler.getFileFromWorkspace(objectGroupId), ProbativeReportEntry.class))
            .extracting(ProbativeReportEntry::getObjectGroupId).isEqualTo(objectGroupId);
    }

    @Test
    public void should_return_item_status_KO_with_empty_ProbativeReportEntry_when_cannot_found_versionModel_from_database() throws Exception {
        // Given
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.set("unitIds", objectMapper.createArrayNode().add("unitId"));
        metadata.put("usageVersion", "Thumbnail_42");

        String objectGroupId = "objectGroupId";
        WorkerParameters param = workerParameterBuilder()
            .withObjectName(objectGroupId)
            .withObjectMetadata(metadata)
            .build();

        File reportFile = tempFolder.newFile();
        TestHandlerIO handler = new TestHandlerIO();
        handler.setNewLocalFile(reportFile);

        given(metaDataClient.getObjectGroupByIdRaw(objectGroupId)).willReturn(getResponseWith("version_id", "storage_id", "BinaryMaster_25", "OPI"));

        // When
        ItemStatus itemStatus = probativeCreateReportEntry.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        assertThat(objectMapper.readValue(handler.getFileFromWorkspace(objectGroupId), ProbativeReportEntry.class))
            .extracting(ProbativeReportEntry::getObjectId).isEqualTo(NO_BINARY_ID);
    }

    @Test
    public void should_return_item_status_KO_with_empty_ProbativeReportEntry_when_cannot_found_digest_from_storage() throws Exception {
        // Given
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.set("unitIds", objectMapper.createArrayNode().add("unitId"));
        metadata.put("usageVersion", "BinaryMaster_25");

        String objectGroupId = "objectGroupId";
        String versionId = "VERSION_ID";
        WorkerParameters param = workerParameterBuilder()
            .withObjectName(objectGroupId)
            .withObjectMetadata(metadata)
            .build();

        File reportFile = tempFolder.newFile();
        TestHandlerIO handler = new TestHandlerIO();
        handler.setNewLocalFile(reportFile);

        String storageId = "storage_id_1";
        String usageVersion = "BinaryMaster_25";

        ObjectNode storageInformation = createStorageInformationWithDigest(storageId, "");

        given(metaDataClient.getObjectGroupByIdRaw(objectGroupId)).willReturn(getResponseWith(versionId, storageId, usageVersion, "OPI"));
        given(storageClient.getInformation("default", OBJECT, versionId, Collections.singletonList(storageId), true)).willReturn(storageInformation);

        // When
        ItemStatus itemStatus = probativeCreateReportEntry.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        assertThat(objectMapper.readValue(handler.getFileFromWorkspace(objectGroupId), ProbativeReportEntry.class))
            .extracting(ProbativeReportEntry::getObjectId).isEqualTo(versionId);
    }

    @Test
    public void should_return_item_status_KO_with_empty_ProbativeReportEntry_when_cannot_found_object_group_LFC_digest_from_logbook() throws Exception {
        // Given
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.set("unitIds", objectMapper.createArrayNode().add("unitId"));
        metadata.put("usageVersion", "BinaryMaster_25");

        String objectGroupId = "objectGroupId";
        String versionId = "VERSION_ID";
        WorkerParameters param = workerParameterBuilder()
            .withObjectName(objectGroupId)
            .withObjectMetadata(metadata)
            .build();

        File reportFile = tempFolder.newFile();
        TestHandlerIO handler = new TestHandlerIO();
        handler.setNewLocalFile(reportFile);

        String storageId = "storage_id_1";
        String usageVersion = "BinaryMaster_25";

        given(metaDataClient.getObjectGroupByIdRaw(objectGroupId)).willReturn(getResponseWith(versionId, storageId, usageVersion, "OPI"));
        given(storageClient.getInformation("default", OBJECT, versionId, Collections.singletonList(storageId), true)).willReturn(createStorageInformationWithDigest(storageId, "DIGEST_FROM_STORAGE"));
        given(logbookLifeCyclesClient.getRawObjectGroupLifeCycleById(objectGroupId)).willReturn(objectMapper.valueToTree(new LogbookLifecycle()));

        // When
        ItemStatus itemStatus = probativeCreateReportEntry.execute(param, handler);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(KO);
        assertThat(objectMapper.readValue(handler.getFileFromWorkspace(objectGroupId), ProbativeReportEntry.class))
            .extracting(ProbativeReportEntry::getObjectId).isEqualTo(versionId);
    }

    private JsonNode createUnitLFCFrom(String versionId, String lastPersistedDate) {
        LogbookEvent o = new LogbookEvent();
        o.setObId(versionId);
        o.setOutDetail(STORING_OBJECT_TASK_ID + EvidenceStatus.OK.name());
        LogbookLifecycle fromValue = new LogbookLifecycle();
        fromValue.setEvents(Collections.singletonList(o));
        fromValue.setLastPersistedDate(lastPersistedDate);
        return objectMapper.valueToTree(fromValue);
    }

    private JsonNode createObjectGroupLifecycleFrom(String versionId, String messageDigest) throws JsonProcessingException {
        LogbookEvent o = new LogbookEvent();
        o.setObId(versionId);
        o.setOutDetail(STORING_OBJECT_TASK_ID + EvidenceStatus.OK.name());
        o.setEvDetData(objectMapper.writeValueAsString(objectMapper.createObjectNode().put(MESSAGE_DIGEST, messageDigest)));
        LogbookLifecycle fromValue = new LogbookLifecycle();
        fromValue.setEvents(Collections.singletonList(o));
        return objectMapper.valueToTree(fromValue);
    }

    private ObjectNode createStorageInformationWithDigest(String storageId, String digest) {
        ObjectNode storageInformation = objectMapper.createObjectNode();
        storageInformation.set(storageId, objectMapper.createObjectNode().put("digest", digest));
        return storageInformation;
    }

    private RequestResponseOK<JsonNode> getResponseWith(String versionId, String storageId, String usageVersion, String opi) {
        DbVersionsModel versionsModel = new DbVersionsModel();
        versionsModel.setDataObjectVersion(usageVersion);
        versionsModel.setId(versionId);
        versionsModel.setOpi(opi);
        DbStorageModel storage = new DbStorageModel();
        storage.setOfferIds(Collections.singletonList(storageId));
        versionsModel.setStorage(storage);
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        DbObjectGroupModel groupModel = new DbObjectGroupModel();
        DbQualifiersModel qualifiersModel = new DbQualifiersModel();
        qualifiersModel.setVersions(Collections.singletonList(versionsModel));
        groupModel.setQualifiers(Collections.singletonList(qualifiersModel));
        responseOK.addResult(objectMapper.valueToTree(groupModel));
        return responseOK;
    }

}