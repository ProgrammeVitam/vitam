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
package fr.gouv.vitam.worker.core.plugin.reclassification;

import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.model.GraphComputeResponse.GraphComputeAction;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ObjectGroupGraphComputePluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private ObjectGroupGraphComputePlugin objectGroupGraphComputePlugin;

    @Before
    public void setUp() throws Exception {
        MetaDataClientFactory metaDataClientFactory = MetaDataClientFactory.getInstance();
        metaDataClientFactory.setVitamClientType(VitamClientFactoryInterface.VitamClientType.MOCK);
        objectGroupGraphComputePlugin = spy(new ObjectGroupGraphComputePlugin(metaDataClientFactory));
    }

    @Test
    public void testGetGraphComputeAction() {
        GraphComputeAction action = objectGroupGraphComputePlugin.getGraphComputeAction();
        Assertions.assertThat(action).isEqualTo(GraphComputeAction.OBJECTGROUP);
    }

    @Test
    public void testGetPluginKeyName() {
        String pluginName = objectGroupGraphComputePlugin.getPluginKeyName();
        Assertions.assertThat(pluginName).isEqualTo("OBJECT_GROUP_GRAPH_COMPUTE");
    }

    @Test(expected = ProcessingException.class)
    public void executeShouldThrowException() throws ProcessingException {
        HandlerIO handlerIO = mock(HandlerIO.class);
        objectGroupGraphComputePlugin.execute(null, handlerIO);
    }

    @Test
    public void whenExecuteListThenOK() {
        HandlerIO handlerIO = mock(HandlerIO.class);
        WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters();
        workerParameters.setObjectNameList(Lists.newArrayList("a", "b", "c"));
        List<ItemStatus> itemStatuses = objectGroupGraphComputePlugin.executeList(workerParameters, handlerIO);
        assertThat(itemStatuses).hasSize(1);
        ItemStatus itemStatuse = itemStatuses.iterator().next();
        assertThat(itemStatuse.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<Integer> statusMeter = itemStatuse.getStatusMeter();
        assertThat(statusMeter.get(3)).isEqualTo(3);
    }

    @Test
    public void whenExecuteListThenFATAL() {
        HandlerIO handlerIO = mock(HandlerIO.class);
        WorkerParameters workerParameters = WorkerParametersFactory.newWorkerParameters();
        workerParameters.setObjectNameList(Lists.newArrayList("a", "b", "c", "d"));
        List<ItemStatus> itemStatuses = objectGroupGraphComputePlugin.executeList(workerParameters, handlerIO);
        assertThat(itemStatuses).hasSize(1);
        ItemStatus itemStatuse = itemStatuses.iterator().next();
        assertThat(itemStatuse.getGlobalStatus()).isEqualTo(StatusCode.FATAL);
        List<Integer> statusMeter = itemStatuse.getStatusMeter();
        assertThat(statusMeter.get(3)).isEqualTo(3);
        assertThat(statusMeter.get(6)).isEqualTo(1);
    }
}
