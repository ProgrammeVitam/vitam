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
package fr.gouv.vitam.worker.core.impl;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.Action;
import fr.gouv.vitam.common.model.processing.ActionDefinition;
import fr.gouv.vitam.common.model.processing.Distribution;
import fr.gouv.vitam.common.model.processing.DistributionKind;
import fr.gouv.vitam.common.model.processing.ProcessBehavior;
import fr.gouv.vitam.common.model.processing.Step;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler;
import fr.gouv.vitam.worker.core.plugin.PluginLoader;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.assertj.core.util.Lists;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkerImplTest {

    private Worker workerImpl;
    private static String workspaceURL;
    private static JunitHelper junitHelper;
    private static int port;

    @Mock
    private PluginLoader pluginLoader;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        workspaceURL = "http://localhost:" + port;
    }

    @AfterClass
    public static void shutdownAfterClass() {
        junitHelper.releasePort(port);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenWorkerImplementWhenWorkParamsIsNullThenThrowsIllegalArgumentException()
        throws IllegalArgumentException, ProcessingException, ContentAddressableStorageServerException {
        workerImpl = WorkerFactory.getInstance(pluginLoader).create();
        workerImpl.run(null, new Step());
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenWorkerImplementWhenStepIsNullThenThrowsIllegalArgumentException()
        throws IllegalArgumentException, ProcessingException, ContentAddressableStorageServerException {
        workerImpl = WorkerFactory.getInstance(pluginLoader).create();
        workerImpl.run(
            WorkerParametersFactory.newWorkerParameters()
                .setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setCurrentStep("currentStep")
                .setContainerName("containerName"),
            null
        );
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenWorkerImplementWhenEmptyActionsInStepThenThrowsIllegalArgumentException()
        throws IllegalArgumentException, HandlerNotFoundException, ProcessingException, ContentAddressableStorageServerException {
        workerImpl = WorkerFactory.getInstance(pluginLoader).create();
        workerImpl.run(
            WorkerParametersFactory.newWorkerParameters()
                .setUrlWorkspace("http://localhost:8083")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setCurrentStep("currentStep")
                .setContainerName("containerName"),
            new Step()
        );
    }

    @Test(expected = ProcessingException.class)
    public void givenWorkerImplementWhenActionIsNullThenThrowsHandlerNotFoundException()
        throws IllegalArgumentException, ProcessingException, ContentAddressableStorageServerException {
        workerImpl = WorkerFactory.getInstance(pluginLoader).create();
        final Step step = new Step();
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        action.setActionDefinition(new ActionDefinition());
        actions.add(action);
        step.setActions(actions);
        workerImpl.run(
            WorkerParametersFactory.newWorkerParameters()
                .setUrlWorkspace(workspaceURL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setCurrentStep("currentStep")
                .setContainerName("containerName"),
            step
        );
    }

    @Test
    public void actionNoBlockTest() throws Exception {
        final Step step = new Step();
        step.setStepName("Traiter_archives");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey(ExtractSedaActionHandler.getId());
        actionDefinition.setBehavior(ProcessBehavior.NOBLOCKING);
        action.setActionDefinition(actionDefinition);
        actions.add(action);
        step.setActions(actions);

        final ActionHandler actionHandler = mock(ExtractSedaActionHandler.class);

        final ItemStatus itemStatus = new ItemStatus("HANDLER_ID");
        itemStatus.setMessage("message");
        itemStatus.setItemId("ITEM_ID_1");
        final StatusCode status = StatusCode.OK;
        itemStatus.increment(status);

        when(actionHandler.executeList(any(), any())).thenReturn(
            Lists.newArrayList(new ItemStatus("HANDLER_ID").setItemsStatus("ITEM_ID_1", itemStatus))
        );
        workerImpl = WorkerFactory.getInstance(pluginLoader)
            .create()
            .addActionHandler(ExtractSedaActionHandler.getId(), actionHandler);
        workerImpl.run(
            WorkerParametersFactory.newWorkerParameters()
                .setUrlWorkspace(workspaceURL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setCurrentStep("currentStep")
                .setContainerName("containerName"),
            step
        );
    }

    @Test
    public void actionBlockTest() throws Exception {
        final Step step = new Step();
        step.setStepName("Traiter_archives");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey(ExtractSedaActionHandler.getId());
        actionDefinition.setBehavior(ProcessBehavior.BLOCKING);
        action.setActionDefinition(actionDefinition);
        actions.add(action);
        step.setActions(actions);

        final ActionHandler actionHandler = mock(ExtractSedaActionHandler.class);
        final ItemStatus itemStatus = new ItemStatus("HANDLER_ID");
        itemStatus.setMessage("message");
        itemStatus.setItemId("ITEM_ID_1");
        final StatusCode status = StatusCode.FATAL;
        itemStatus.increment(status);

        when(actionHandler.executeList(any(), any())).thenReturn(
            Lists.newArrayList(new ItemStatus("HANDLER_ID").setItemsStatus("ITEM_ID_1", itemStatus))
        );
        workerImpl = WorkerFactory.getInstance(pluginLoader)
            .create()
            .addActionHandler(ExtractSedaActionHandler.getId(), actionHandler);
        workerImpl.run(
            WorkerParametersFactory.newWorkerParameters()
                .setUrlWorkspace("http://localhost:8011/")
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList("objectName.json"))
                .setCurrentStep("currentStep")
                .setContainerName("containerName"),
            step
        );
    }

    @Test
    public void actionBlockTestWarning() throws Exception {
        final Step step = new Step();
        step.setStepName("Traiter_archives");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey(ExtractSedaActionHandler.getId());
        actionDefinition.setBehavior(ProcessBehavior.NOBLOCKING);
        action.setActionDefinition(actionDefinition);
        actions.add(action);
        step.setActions(actions);
        step.setDistribution(new Distribution().setElement("Units").setKind(DistributionKind.LIST_ORDERING_IN_FILE));

        final ActionHandler actionHandler = mock(ExtractSedaActionHandler.class);
        final ItemStatus itemStatus = new ItemStatus("HANDLER_ID");
        itemStatus.setMessage("message");
        itemStatus.setItemId("ITEM_ID_1");
        final StatusCode status = StatusCode.FATAL;
        itemStatus.increment(status);

        when(actionHandler.executeList(any(), any())).thenReturn(
            Lists.newArrayList(new ItemStatus("HANDLER_ID").setItemsStatus("ITEM_ID_1", itemStatus))
        );
        workerImpl = WorkerFactory.getInstance(pluginLoader)
            .create()
            .addActionHandler(ExtractSedaActionHandler.getId(), actionHandler);
        workerImpl.run(
            WorkerParametersFactory.newWorkerParameters()
                .setUrlWorkspace(workspaceURL)
                .setUrlMetadata("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(GUIDFactory.newGUID().getId() + ".json"))
                .setCurrentStep("currentStep")
                .setContainerName(GUIDFactory.newGUID().getId()),
            step
        );
    }
}
