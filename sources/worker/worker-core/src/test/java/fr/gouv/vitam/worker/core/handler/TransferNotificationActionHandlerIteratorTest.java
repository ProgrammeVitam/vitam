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
package fr.gouv.vitam.worker.core.handler;

import static com.google.common.collect.Lists.newArrayList;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.processing.common.parameter.WorkerParameterName.workflowStatusKo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

import com.fasterxml.jackson.databind.JsonNode;

import com.mongodb.util.JSON;
import fr.gouv.vitam.common.PropertiesUtils;
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
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "org.xml.sax.*", "javax.management.*"})
@PrepareForTest({WorkspaceClientFactory.class, LogbookLifeCyclesClientFactory.class,
    LogbookOperationsClientFactory.class, ValidationXsdUtils.class})
public class TransferNotificationActionHandlerIteratorTest {
    private static final String ARCHIVE_ID_TO_GUID_MAP =
        "transferNotificationActionHandler/ARCHIVE_ID_TO_GUID_MAP_objKO.json";
    private static final String DATA_OBJECT_ID_TO_GUID_MAP =
        "transferNotificationActionHandler/DATA_OBJECT_ID_TO_GUID_MAP_objKO.json";
    private static final String DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP =
        "transferNotificationActionHandler/DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP_objKO.json";
    private static final String DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP =
        "transferNotificationActionHandler/DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP_objKO.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP =
        "transferNotificationActionHandler/OBJECT_GROUP_ID_TO_GUID_MAPKO.json";

    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final String LOGBOOK_OPERATION_OK = "transferNotificationActionHandler/logbookOperationOK.json";
    private static final String LOGBOOK_OPERATION_KO = "transferNotificationActionHandler/logbookOperationKO.json";
    private static final String LOGBOOK_OPERATION_WARNING =
        "transferNotificationActionHandler/logbookOperationWarning.json";
    private static final String LOGBOOK_LFC_AU = "transferNotificationActionHandler/logbookLifecycleAUKO.json";
    private static final String LOGBOOK_LFC_AU_OK = "transferNotificationActionHandler/logbookLifecycleAU.json";
    private static final String LOGBOOK_LFC_AU_WARNING =
        "transferNotificationActionHandler/logbookLifecycleAUWarning.json";
    private static final String LOGBOOK_LFC_GOT = "transferNotificationActionHandler/logbookLifecycleGOTKO.json";
    private static final String LOGBOOK_LFC_GOT_WARNING =
        "transferNotificationActionHandler/logbookLifecycleGOTWarning.json";
    public static final String ID = "_id";

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private LogbookLifeCyclesClientFactory lifeCyclesClientFactory;
    private LogbookLifeCyclesClient lifeCyclesClient;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private LogbookOperationsClient logbookOperationsClient;

    private HandlerIOImpl handlerIO;
    private List<IOParameter> in;
    private List<IOParameter> out;
    private GUID guid;
    private WorkerParameters params;

    @Before
    public void setUp() throws Exception {
        guid = GUIDFactory.newGUID();
        PowerMockito.mockStatic(ValidationXsdUtils.class);
        PowerMockito.when(ValidationXsdUtils.checkWithXSD(any(), any())).thenReturn(true);
        params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8080")
                .setUrlMetadata("http://localhost:8080").setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName(guid.getId()).setProcessId("aeaaaaaaaaaaaaababz4aakxtykbybyaaaaq");
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        String objectId = "objectId";
        handlerIO = new HandlerIOImpl(guid.getId(), "workerId", newArrayList(objectId));
        handlerIO.setCurrentObjectId(objectId);
        
        logbookOperationsClient = mock(LogbookOperationsClient.class);
        PowerMockito.mockStatic(LogbookOperationsClientFactory.class);
        logbookOperationsClientFactory = mock(LogbookOperationsClientFactory.class);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance()).thenReturn(logbookOperationsClientFactory);
        PowerMockito.when(LogbookOperationsClientFactory.getInstance().getClient()).thenReturn(logbookOperationsClient);

        lifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        PowerMockito.mockStatic(LogbookLifeCyclesClientFactory.class);
        lifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance()).thenReturn(lifeCyclesClientFactory);
        PowerMockito.when(LogbookLifeCyclesClientFactory.getInstance().getClient()).thenReturn(lifeCyclesClient);

        in = new ArrayList<>();
        for (int i = 0; i < TransferNotificationActionHandler.HANDLER_IO_PARAMETER_NUMBER; i++) {
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "file" + i)));
        }
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(DATA_OBJECT_ID_TO_GUID_MAP), false);
        handlerIO.addOutputResult(2, PropertiesUtils.getResourceFile(DATA_OBJECT_TO_OBJECT_GROUP_ID_MAP), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(DATA_OBJECT_ID_TO_DATA_OBJECT_DETAIL_MAP), false);
        handlerIO.addOutputResult(4, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS), false);
        handlerIO.addOutputResult(5, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);

        File  existingGOTGUIDToNewGotGUIDInAttachmentFile = handlerIO.getNewLocalFile("existingGOTGUIDToNewGotGUIDInAttachmentFile");
        JsonHandler.writeAsFile(JsonHandler.createObjectNode(), existingGOTGUIDToNewGotGUIDInAttachmentFile);
            handlerIO.addOutputResult(6, existingGOTGUIDToNewGotGUIDInAttachmentFile, false);

        handlerIO.reset();
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/responseReply.xml")));
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    public void givenXMLCreationWhenValidThenResponseOK()
        throws Exception {
        try (TransferNotificationActionHandler handler = new TransferNotificationActionHandler();) {


            doReturn(getLogbookOperationOK()).when(logbookOperationsClient).selectOperationById(any()
            );
            when(lifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleGOT())))
                .thenReturn(new RequestResponseOK<JsonNode>());
            when(lifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAU())))
                .thenReturn(new RequestResponseOK<JsonNode>());

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            handlerIO.reset();
            handlerIO.addInIOParameters(in);
            handlerIO.addOutIOParameters(out);
            WorkerParameters parameters =
                params.putParameterValue(workflowStatusKo, StatusCode.OK.name())
                    .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler
                .execute(parameters, handlerIO);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
        }
    }


    @Test
    public void givenXMLCreationWhenValidThenResponseWarningAUGOT()
        throws Exception {
        try (TransferNotificationActionHandler handler = new TransferNotificationActionHandler()) {

            doReturn(getLogbookOperationWarning()).when(logbookOperationsClient).selectOperationById(
                any()
            );
            when(lifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleGOTWarning())))
                .thenReturn(new RequestResponseOK<JsonNode>());
            when(lifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAUWarning())))
                .thenReturn(new RequestResponseOK<JsonNode>());

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            handlerIO.reset();
            handlerIO.addInIOParameters(in);
            handlerIO.addOutIOParameters(out);
            WorkerParameters parameters =
                params.putParameterValue(workflowStatusKo, StatusCode.WARNING.name())
                    .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler
                .execute(parameters, handlerIO);
            final InputStream xmlFile;
            try {
                xmlFile = handlerIO.getInputStreamFromWorkspace(out.get(0).getUri().getPath());
            } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
                throw new ProcessingException(e);
            }
            assertEquals(StatusCode.OK, response.getGlobalStatus());

            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader reader = xmlInputFactory.createXMLEventReader(xmlFile);
            String archiveUnitId = null;
            boolean isEventPresent = false,
                    mgmtPresent =true;
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement() &&
                    event.asStartElement().getName().getLocalPart().equals("ArchiveUnit")) {
                    event = reader.nextEvent();
                    if (event.isStartElement() &&
                        event.asStartElement().getName().getLocalPart().equals("Management")) {
                        mgmtPresent = true;
                        event = reader.nextEvent();
                        if (event.isStartElement() &&
                            event.asStartElement().getName().getLocalPart().equals("LogBook")) {
                            event = reader.nextEvent();
                            if (event.isStartElement() &&
                                event.asStartElement().getName().getLocalPart().equals("Event")) {
                                isEventPresent = true;
                                event = reader.nextEvent();
                            }
                        }
                        while(!event.isEndElement() ||
                            !event.asEndElement().getName().getLocalPart().equals("Management")){
                            event = reader.nextEvent();
                        }

                    }
                    if(mgmtPresent){
                        event = reader.nextEvent();
                    }
                    if (event.isStartElement() &&
                        event.asStartElement().getName().getLocalPart().equals("Content")) {
                        event = reader.nextEvent();
                    }
                    if (event.isStartElement() &&
                        event.asStartElement().getName().getLocalPart().equals("SystemId")) {
                        String elementText = reader.getElementText();
                        if(isEventPresent) {
                            archiveUnitId = elementText;
                            break;
                        }
                    }
                    event = reader.nextEvent();
                }
            }

            JsonNode guidAU = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP));
            assertTrue(guidAU.toString().contains(archiveUnitId));
        }
    }

    @Test
    public void givenXMLCreationWhenProcessKOThenResponseATROK()
        throws Exception {
        try (TransferNotificationActionHandler handler = new TransferNotificationActionHandler();) {

            doReturn(getLogbookOperationKO()).when(logbookOperationsClient).selectOperationById(any()
            );
            when(lifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleGOT())))
                .thenReturn(new RequestResponseOK<JsonNode>());
            when(lifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAU())))
                .thenReturn(new RequestResponseOK<JsonNode>());

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            handlerIO.reset();
            handlerIO.addInIOParameters(in);
            handlerIO.addOutIOParameters(out);
            WorkerParameters parameters =
                params.putParameterValue(workflowStatusKo, KO.name())
                    .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler
                .execute(parameters.putParameterValue(workflowStatusKo, KO.name()),
                    handlerIO);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
        }
    }


    @Test
    public void givenXMLCreationWhenProcessKOBeforeLifecycleThenResponseATRKO()
        throws Exception {
        try (TransferNotificationActionHandler handler = new TransferNotificationActionHandler();) {

            doReturn(getLogbookOperationKO()).when(logbookOperationsClient).selectOperationById(any()
            );
            when(lifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAUOK())))
                .thenReturn(new RequestResponseOK<JsonNode>());
            when(lifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAUOK())))
                .thenReturn(new RequestResponseOK<JsonNode>());

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            handlerIO.reset();
            handlerIO.addInIOParameters(in);
            handlerIO.addOutIOParameters(out);
            WorkerParameters parameters =
                params.putParameterValue(workflowStatusKo, KO.name())
                    .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler
                .execute(parameters, handlerIO);
            assertEquals(StatusCode.OK, response.getGlobalStatus());
        }
    }


    @Test
    public void givenExceptionLogbookWhenProcessKOBeforeLifecycleThenResponseKO()
        throws Exception {
        try (TransferNotificationActionHandler handler = new TransferNotificationActionHandler()) {

            doThrow(new LogbookClientException("")).when(logbookOperationsClient).selectOperationById(
                any()
            );

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            handlerIO.reset();
            handlerIO.addInIOParameters(in);
            handlerIO.addOutIOParameters(out);
            WorkerParameters parameters =
                params.putParameterValue(workflowStatusKo, KO.name())
                    .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler
                .execute(parameters, handlerIO);
            assertEquals(KO, response.getGlobalStatus());
        }
    }

    @Test
    public void givenExceptionLogbookLCUnitWhenProcessOKThenResponseKO()
        throws Exception {
        try (TransferNotificationActionHandler handler = new TransferNotificationActionHandler();) {

            doReturn(getLogbookOperationKO()).when(logbookOperationsClient).selectOperationById(any()
            );
            when(lifeCyclesClient.objectGroupLifeCyclesByOperationIterator(any(), any(), any()))
                .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleGOT())))
                .thenReturn(new RequestResponseOK<JsonNode>());
            doReturn(new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAUWarning()))
                .when(lifeCyclesClient).unitLifeCyclesByOperationIterator(any(),
                any(), any());
            doThrow(new LogbookClientException("")).when(lifeCyclesClient)
                .unitLifeCyclesByOperationIterator(any(), any(), any());

            assertEquals(TransferNotificationActionHandler.getId(), HANDLER_ID);
            handlerIO.reset();
            handlerIO.addInIOParameters(in);
            handlerIO.addOutIOParameters(out);
            WorkerParameters parameters =
                params.putParameterValue(workflowStatusKo, KO.name())
                    .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());
            final ItemStatus response = handler
                .execute(parameters, handlerIO);
            assertEquals(KO, response.getGlobalStatus());
        }
    }

    @Test
    public void givenExceptionLogbookLCObjectWhenProcessOKThenResponseKO() throws Exception {
        // Given
        TransferNotificationActionHandler handler = new TransferNotificationActionHandler();

        doReturn(getLogbookOperationKO()).when(logbookOperationsClient).selectOperationById(any()
        );

        doThrow(new LogbookClientException("")).when(lifeCyclesClient)
            .objectGroupLifeCyclesByOperationIterator(any(), any(), any());

        when(lifeCyclesClient.unitLifeCyclesByOperationIterator(any(), any(), any()))
            .thenReturn((new RequestResponseOK<JsonNode>().addResult(getLogbookLifecycleAU())))
            .thenReturn(new RequestResponseOK<JsonNode>());
        
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);
        WorkerParameters parameters = params.putParameterValue(workflowStatusKo, KO.name())
            .putParameterValue(WorkerParameterName.logBookTypeProcess, LogbookTypeProcess.INGEST.name());

        // When
        final ItemStatus response = handler.execute(parameters, handlerIO);

        // Then
        assertThat(response.getGlobalStatus()).isEqualTo(KO);
    }

    private static JsonNode getLogbookOperationKO()
        throws IOException, InvalidParseOperationException {
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 1));
        final LogbookOperation lop =
            new LogbookOperation(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_OPERATION_KO)));
        response.addResult(JsonHandler.getFromString(JSON.serialize(lop)));
        return JsonHandler.toJsonNode(response);
    }

    private static JsonNode getLogbookOperationOK()
        throws IOException, InvalidParseOperationException {
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 1));
        final LogbookOperation lop =
            new LogbookOperation(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_OPERATION_OK)));
        response.addResult(JsonHandler.getFromString(JSON.serialize(lop)));
        return JsonHandler.toJsonNode(response);
    }

    private static JsonNode getLogbookOperationWarning()
        throws IOException, InvalidParseOperationException {
        final RequestResponseOK response = new RequestResponseOK().setHits(new DatabaseCursor(1, 0, 1));
        final LogbookOperation lop =
            new LogbookOperation(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_OPERATION_WARNING)));
        response.addResult(JsonHandler.getFromString(JSON.serialize(lop)));
        return JsonHandler.toJsonNode(response);
    }

    private static JsonNode getLogbookLifecycleGOT()
        throws IOException, InvalidParseOperationException {
        return JsonHandler.getFromString(
            JSON.serialize(new LogbookLifeCycleObjectGroup(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_GOT)))
                ));
    }

    private static JsonNode getLogbookLifecycleAU()
        throws IOException, InvalidParseOperationException {
        return JsonHandler.getFromString(
            JSON.serialize(new LogbookLifeCycleUnit(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_AU)))
                ));
    }

    private static JsonNode getLogbookLifecycleAUOK()
        throws IOException, InvalidParseOperationException {
        return JsonHandler.getFromString(
            JSON.serialize(new LogbookLifeCycleUnit(StreamUtils.toString(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_AU_OK)))
                ));
    }

    private static JsonNode getLogbookLifecycleGOTWarning()
        throws IOException, InvalidParseOperationException {
        return
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_GOT_WARNING));
    }

    private static JsonNode getLogbookLifecycleAUWarning()
        throws IOException, InvalidParseOperationException {
        JsonNode fromInputStream =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(LOGBOOK_LFC_AU_WARNING));
        return fromInputStream;

    }

}
