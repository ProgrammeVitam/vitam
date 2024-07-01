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

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CheckObjectSizeActionPluginTest {

    private static final String CHECK_SIZE = "CHECK_SIZE";

    private static final String OBJECT_GROUP_CORRECT_SIZE =
        "checkSizeActionPlugin/aeaaaaaaaaf2zkp7abyvqalsbf3erriaaaba.json";

    private static final String OBJECT_GROUP_INCORRECT_SIZE =
        "checkSizeActionPlugin/aeaaaaaaaaf2zkp7abyvqalsbf3erriaaabb.json";

    private static final String OBJECT_GROUP_MISSED_THEN_CALCULATED_SIZE =
        "checkSizeActionPlugin/aeaaaaaaaaf2zkp7abyvqalsbf3erriaaabc.json";

    private static final String OBJECT_GROUP_INCORRECT_SIZE_MULTIBINARY_OBJECTS =
        "checkSizeActionPlugin/aeaaaaaaaaf2zkp7abyvqalsbf3erriaaabd.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(workspaceClient, logbookOperationsClient, logbookLifeCyclesClient);
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
    }

    @Test
    public void checkCorrectSizeForBinary() throws Exception {
        // Given
        InputStream objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_CORRECT_SIZE);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objName1"))).thenReturn(
            Response.status(Response.Status.OK).entity(objectGroup).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/Content/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"))).thenReturn(
            Response.status(Response.Status.OK)
                .entity(
                    PropertiesUtils.getResourceAsStream(
                        "checkSizeActionPlugin/binaryObject/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"
                    )
                )
                .build()
        );

        CheckObjectSizeActionPlugin plugin = new CheckObjectSizeActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objName1");
        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckSizeActionPluginTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = plugin.execute(params, handlerIO);

        // Then
        Integer count = response.getStatusMeter().get(StatusCode.OK.ordinal());
        assertEquals(1L, count.longValue());
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertEquals(
            CHECK_SIZE,
            response.getItemsStatus().get(CHECK_SIZE).getSubTaskStatus().values().iterator().next().getItemId()
        );
        handlerIO.close();
    }

    @Test
    public void checkInCorrectSizeForBinary() throws Exception {
        // Given
        InputStream objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_INCORRECT_SIZE);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objName2"))).thenReturn(
            Response.status(Response.Status.OK).entity(objectGroup).build()
        );
        CheckObjectSizeActionPlugin plugin = new CheckObjectSizeActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objName2");
        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckSizeActionPluginTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = plugin.execute(params, handlerIO);

        // Then
        Integer count = response.getStatusMeter().get(StatusCode.WARNING.ordinal());
        Assertions.assertThat(count).isEqualTo(1);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
        assertEquals(
            response.getItemsStatus().get(CHECK_SIZE).getSubTaskStatus().values().iterator().next().getItemId(),
            CHECK_SIZE
        );
        handlerIO.close();
    }

    @Test
    public void checkMissedSizeForBinary() throws Exception {
        // Given
        InputStream objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_MISSED_THEN_CALCULATED_SIZE);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objName2"))).thenReturn(
            Response.status(Response.Status.OK).entity(objectGroup).build()
        );
        CheckObjectSizeActionPlugin plugin = new CheckObjectSizeActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objName2");
        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckSizeActionPluginTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = plugin.execute(params, handlerIO);

        // Then
        Integer count = response.getStatusMeter().get(StatusCode.OK.ordinal());
        Assertions.assertThat(count).isEqualTo(1);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertEquals(
            response.getItemsStatus().get(CHECK_SIZE).getSubTaskStatus().values().iterator().next().getItemId(),
            CHECK_SIZE
        );
        handlerIO.close();
    }

    @Test
    public void checkInCorrectForBinaryInMultiBInaryGot() throws Exception {
        // Given
        InputStream objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_INCORRECT_SIZE_MULTIBINARY_OBJECTS);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objName3"))).thenReturn(
            Response.status(Response.Status.OK).entity(objectGroup).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/Content/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"))).thenReturn(
            Response.status(Response.Status.OK)
                .entity(
                    PropertiesUtils.getResourceAsStream(
                        "checkSizeActionPlugin/binaryObject/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"
                    )
                )
                .build()
        );

        CheckObjectSizeActionPlugin plugin = new CheckObjectSizeActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objName3");
        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckSizeActionPluginTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        handlerIO.addOutIOParameters(out);

        // When
        final ItemStatus response = plugin.execute(params, handlerIO);

        // Then
        Integer count = response.getStatusMeter().get(StatusCode.WARNING.ordinal());
        Assertions.assertThat(count).isEqualTo(1);
        assertEquals(StatusCode.WARNING, response.getGlobalStatus());
        assertEquals(
            response.getItemsStatus().get(CHECK_SIZE).getSubTaskStatus().values().iterator().next().getItemId(),
            CHECK_SIZE
        );
        handlerIO.close();
    }

    private DefaultWorkerParameters getDefaultWorkerParameters() {
        DefaultWorkerParameters workerParam = WorkerParametersFactory.newWorkerParameters(
            "pId",
            "stepId",
            "CheckSizeActionPluginTest",
            "currentStep",
            Lists.newArrayList("objName"),
            "metadataURL",
            "workspaceURL"
        );
        return workerParam;
    }
}
