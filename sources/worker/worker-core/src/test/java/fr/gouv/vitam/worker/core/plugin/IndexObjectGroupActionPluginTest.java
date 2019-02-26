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
package fr.gouv.vitam.worker.core.plugin;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
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
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, MetaDataClientFactory.class})
public class IndexObjectGroupActionPluginTest {

    IndexObjectGroupActionPlugin plugin;
    private WorkspaceClient workspaceClient;
    private MetaDataClient metadataClient;
    private static final String HANDLER_ID = "OG_METADATA_INDEXATION";
    private static final String OBJECT_GROUP = "objectGroup.json";
    private final InputStream objectGroup;
    private WorkspaceClientFactory workspaceClientFactory;
    private List<IOParameter> in;
    private JsonNode og;
    
    HandlerIOImpl handlerIO;

    public IndexObjectGroupActionPluginTest() throws FileNotFoundException, InvalidParseOperationException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        og = JsonHandler.getFromInputStream(objectGroup);
    }

    @Before
    public void setUp() throws URISyntaxException {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(MetaDataClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        handlerIO = new HandlerIOImpl("IndexObjectGroupActionPluginTest", "workerId", newArrayList("objectName.json"));
        metadataClient = mock(MetaDataClientRest.class);

        handlerIO.setCurrentObjectId("objectName.json");
        in = new ArrayList<>();
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "unitId")));
        handlerIO.addInIOParameters(in);
    }

    @After
    public void clean() {
        handlerIO.partialClose();
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK()
        throws Exception {
        when(metadataClient.insertObjectGroup(any())).thenReturn(JsonHandler.createObjectNode());
        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        plugin = new IndexObjectGroupActionPlugin();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);

        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void testMetadataException()
        throws Exception {
        when(metadataClient.insertObjectGroups(any())).thenThrow(new MetaDataExecutionException(""));
        
        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        plugin = new IndexObjectGroupActionPlugin();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void testMetadataParseException()
        throws Exception {
        when(metadataClient.insertObjectGroups(any())).thenThrow(new InvalidParseOperationException(""));

        handlerIO.getInput().clear();
        handlerIO.getInput().add(og);
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        plugin = new IndexObjectGroupActionPlugin();
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setObjectName("objectName.json").setCurrentStep("currentStep")
                .setContainerName("IndexObjectGroupActionPluginTest");
        final ItemStatus response = plugin.executeList(params, handlerIO).get(0);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

}
