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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.database.server.mongodb.BsonHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.elasticsearch.common.collect.Iterators;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransferNotificationActionHandlerIteratorTestSpecific {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String ATR_PATH = "ATR/responseReply.xml";
    private static final String ARCHIVE_ID_TO_GUID_MAP =
        "transferNotificationActionHandler/ARCHIVE_ID_TO_GUID_MAP_objKO.json";
    private static final String DATA_OBJECT_ID_TO_GUID_MAP =
        "transferNotificationActionHandler/DATA_OBJECT_ID_TO_GUID_MAP_objKO.json";
    private static final String DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP =
        "transferNotificationActionHandler/DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP_objKO.json";
    private static final String DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP =
        "transferNotificationActionHandler/DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP_objKO.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS_WITHOUT_INFO_FIELDS =
        "globalSEDAParametersWithoutInfoFields.json";
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP =
        "transferNotificationActionHandler/OBJECT_GROUP_ID_TO_GUID_MAPKO.json";
    private static final String SEDA_PARAMS = "transferNotificationActionHandler/SedaParams.json";

    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final String LOGBOOK_OPERATION = "transferNotificationActionHandler/logbookOperationKO.json";
    private static final String LOGBOOK_LFC_AU = "transferNotificationActionHandler/logbookLifecycleAUKO.json";
    private static final String LOGBOOK_LFC_GOT = "transferNotificationActionHandler/logbookLifecycleGOTKO.json";

    private WorkspaceClient workspaceClient;
    private static final WorkspaceClientFactory workspaceClientFactory = mock(WorkspaceClientFactory.class);
    private StorageClient storageClient;
    private static final StorageClientFactory storageClientFactory = mock(StorageClientFactory.class);
    private static final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory = mock(
        LogbookLifeCyclesClientFactory.class
    );
    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private static final LogbookOperationsClientFactory logbookOperationsClientFactory = mock(
        LogbookOperationsClientFactory.class
    );
    private LogbookOperationsClient logbookOperationsClient;

    private static final ValidationXsdUtils validationXsdUtils = mock(ValidationXsdUtils.class);
    private HandlerIOImpl action;
    private List<IOParameter> in;
    private List<IOParameter> out;
    private GUID guid;
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        guid = GUIDFactory.newGUID();
        params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8080")
            .setUrlMetadata("http://localhost:8080")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId())
            .setProcessId("aeaaaaaaaaaaaaababz4aakxtykbybyaaaaq");
        workspaceClient = mock(WorkspaceClient.class);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);

        storageClient = mock(StorageClient.class);
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        logbookOperationsClient = mock(LogbookOperationsClient.class);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        String objectId = "objectId";
        action = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            guid.getId(),
            "workerId",
            newArrayList(objectId)
        );
        action.setCurrentObjectId(objectId);

        when(validationXsdUtils.checkWithXSD(any(InputStream.class), anyString())).thenReturn(true);

        in = new ArrayList<>();
        for (int i = 0; i < TransferNotificationActionHandler.HANDLER_IO_PARAMETER_NUMBER; i++) {
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file" + i)));
        }
        action.addOutIOParameters(in);
        action.addOutputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        action.addOutputResult(1, PropertiesUtils.getResourceFile(DATA_OBJECT_ID_TO_GUID_MAP), false);
        action.addOutputResult(2, PropertiesUtils.getResourceFile(DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP), false);
        action.addOutputResult(3, PropertiesUtils.getResourceFile(DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP), false);
        action.addOutputResult(
            4,
            PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS_WITHOUT_INFO_FIELDS),
            false
        );
        action.addOutputResult(5, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);
        action.addOutputResult(7, PropertiesUtils.getResourceFile(SEDA_PARAMS), false);
        action.reset();
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, ATR_PATH)));
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenXMLCreationWhenValidButWithoutInfoFieldsInManifestThenResponseOK() throws Exception {
        try (
            TransferNotificationActionHandler handler = new TransferNotificationActionHandler(
                logbookOperationsClientFactory,
                storageClientFactory,
                validationXsdUtils
            )
        ) {
            doReturn(getLogbookOperation()).when(logbookOperationsClient).selectOperationById(any());
            when(logbookLifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any())).thenReturn(
                CloseableIteratorUtils.toCloseableIterator(Iterators.single(getLogbookLifecycleGOT()))
            );
            when(logbookLifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any())).thenReturn(
                CloseableIteratorUtils.toCloseableIterator(Iterators.single(getLogbookLifecycleAU()))
            );

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            action.reset();
            action.addInIOParameters(in);
            action.addOutIOParameters(out);
            WorkerParameters parameters = params
                .putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.KO.name())
                .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler.execute(
                parameters.putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.KO.name()),
                action
            );
            assertEquals(StatusCode.OK, response.getGlobalStatus());
            File atr = action.getNewLocalFile(ATR_PATH);
            String atrString = FileUtil.readFile(atr);
            assertTrue(atrString.contains(SedaConstants.TAG_ARCHIVE_PROFILE));
            assertTrue(atrString.contains(SedaConstants.TAG_DATA_OBJECT_GROUP_SYSTEM_ID));
        }
    }

    @Test
    public void givenXMLCreationWhenProcessKOButWithoutInfoFieldsInManifestThenResponseATROK() throws Exception {
        try (
            TransferNotificationActionHandler handler = new TransferNotificationActionHandler(
                logbookOperationsClientFactory,
                storageClientFactory,
                validationXsdUtils
            )
        ) {
            doReturn(getLogbookOperation()).when(logbookOperationsClient).selectOperationById(any());
            when(logbookLifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any())).thenReturn(
                CloseableIteratorUtils.toCloseableIterator(Iterators.single(getLogbookLifecycleGOT()))
            );
            when(logbookLifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any())).thenReturn(
                CloseableIteratorUtils.toCloseableIterator(Iterators.single(getLogbookLifecycleAU()))
            );

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            action.reset();
            action.addInIOParameters(in);
            action.addOutIOParameters(out);
            WorkerParameters parameters = params
                .putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.KO.name())
                .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler.execute(
                parameters.putParameterValue(WorkerParameterName.workflowStatusKo, StatusCode.KO.name()),
                action
            );
            assertEquals(StatusCode.OK, response.getGlobalStatus());
            File atr = action.getNewLocalFile(ATR_PATH);
            assertTrue(FileUtil.readFile(atr).contains(SedaConstants.TAG_ARCHIVE_PROFILE));
        }
    }

    private static JsonNode getLogbookOperation() throws IOException, InvalidParseOperationException {
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 1));
        final LogbookOperation lop = new LogbookOperation(
            StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_OPERATION))
        );
        response.addResult(BsonHelper.fromDocumentToJsonNode(lop));
        return JsonHandler.toJsonNode(response);
    }

    private static JsonNode getLogbookLifecycleGOT() throws IOException, InvalidParseOperationException {
        return BsonHelper.fromDocumentToJsonNode(
            new LogbookLifeCycleObjectGroup(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_GOT)))
        );
    }

    private static JsonNode getLogbookLifecycleAU() throws IOException, InvalidParseOperationException {
        return BsonHelper.fromDocumentToJsonNode(
            new LogbookLifeCycleUnit(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_AU)))
        );
    }
}
