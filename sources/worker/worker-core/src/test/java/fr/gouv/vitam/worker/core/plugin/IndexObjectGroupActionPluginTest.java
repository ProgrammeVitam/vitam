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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientRest;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IndexObjectGroupActionPluginTest {

    private static final String OBJECT_GROUP = "IndexObjectGroupActionPlugin/objectGroup.json";
    private static final String EXISTING_OBJECT_GROUP_IN_DB =
        "IndexObjectGroupActionPlugin/existing_objectGroup_in_db.json";
    private static final String EXISTING_OBJECT_GROUP = "IndexObjectGroupActionPlugin/existing_objectGroup.json";
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private LogbookLifeCyclesClient logbookLifeCyclesClient;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private MetaDataClient metadataClient;
    private MetaDataClientFactory metaDataClientFactory;
    private List<IOParameter> in;

    HandlerIOImpl handlerIO;

    IndexObjectGroupActionPlugin plugin;

    @Before
    public void setUp() throws URISyntaxException {
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        metadataClient = mock(MetaDataClientRest.class);
        metaDataClientFactory = mock(MetaDataClientFactory.class);

        logbookLifeCyclesClient = mock(LogbookLifeCyclesClient.class);
        logbookLifeCyclesClientFactory = mock(LogbookLifeCyclesClientFactory.class);

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(metaDataClientFactory.getClient()).thenReturn(metadataClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);

        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "IndexObjectGroupActionPluginTest",
            "workerId",
            newArrayList("objectName.json")
        );

        handlerIO.setCurrentObjectId("objectName.json");
        in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "unitId")));
        handlerIO.addInIOParameters(in);
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        when(metadataClient.insertObjectGroup(any())).thenReturn(JsonHandler.createObjectNode());
        handlerIO.getInput().clear();
        handlerIO.getInput().add(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OBJECT_GROUP)));
        plugin = new IndexObjectGroupActionPlugin(metaDataClientFactory);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);

        assertEquals(StatusCode.OK, response.getGlobalStatus());

        ArgumentCaptor<List<JsonNode>> queryArgumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(metadataClient).insertObjectGroups(queryArgumentCaptor.capture());
        String indexedObjectGroup = JsonHandler.unprettyPrint(queryArgumentCaptor.getValue());
        assertThat(indexedObjectGroup).contains(("\"_id\":\"aeaaaaaaaaaaaaababztoakvqik56mqaaaaq\""));
        assertThat(indexedObjectGroup).doesNotContain("_work");
    }

    @Test
    public void givenExistingObjectGroupWhenAddingNewObjectThenOK() throws Exception {
        doNothing().when(metadataClient).updateObjectGroupById(any(), any());
        when(metadataClient.getObjectGroupByIdRaw(any())).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(
                JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(EXISTING_OBJECT_GROUP_IN_DB))
            )
        );
        handlerIO.getInput().clear();
        handlerIO
            .getInput()
            .add(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(EXISTING_OBJECT_GROUP)));
        plugin = new IndexObjectGroupActionPlugin(metaDataClientFactory);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);

        assertEquals(StatusCode.OK, response.getGlobalStatus());

        ArgumentCaptor<JsonNode> queryArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(metadataClient).updateObjectGroupById(
            queryArgumentCaptor.capture(),
            eq("aebaaaaaaacyojsgaay5ialsckziacaaaaaq")
        );

        String indexedObjectGroup = JsonHandler.unprettyPrint(queryArgumentCaptor.getValue());
        assertThat(indexedObjectGroup).contains(("aeaaaaaaaahdmfuhaar36ambrctkscaaaaaq"));
        assertThat(indexedObjectGroup).contains(("aeaaaaaaaacyojsgaay5ialsckz6pbaaaaaq"));
        assertThat(indexedObjectGroup).doesNotContain("_work");
    }

    @Test
    public void givenMetadataBadRequestWhenExecuteOnExistingGotThenReturnResponseKO() throws Exception {
        doThrow(new InvalidParseOperationException("")).when(metadataClient).updateObjectGroupById(any(), any());
        when(metadataClient.getObjectGroupByIdRaw(any())).thenReturn(
            new RequestResponseOK<JsonNode>().addResult(
                JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(EXISTING_OBJECT_GROUP_IN_DB))
            )
        );
        handlerIO.getInput().clear();
        handlerIO
            .getInput()
            .add(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(EXISTING_OBJECT_GROUP)));
        plugin = new IndexObjectGroupActionPlugin(metaDataClientFactory);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);

        assertEquals(StatusCode.KO, response.getGlobalStatus());
    }

    @Test
    public void testMetadataException() throws Exception {
        when(metadataClient.insertObjectGroups(any())).thenThrow(new MetaDataExecutionException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OBJECT_GROUP)));
        plugin = new IndexObjectGroupActionPlugin(metaDataClientFactory);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void testMetadataParseException() throws Exception {
        when(metadataClient.insertObjectGroups(any())).thenThrow(new InvalidParseOperationException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(OBJECT_GROUP)));
        plugin = new IndexObjectGroupActionPlugin(metaDataClientFactory);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }
}
