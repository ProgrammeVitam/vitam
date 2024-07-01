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

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UnitAttachmentPluginTest {

    private static final String UNITS_TO_ATTACH_DIR = "UnitsToAttach";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    @InjectMocks
    private UnitAttachmentPlugin unitAttachmentPlugin;

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Before
    public void init() {
        doReturn(logbookLifeCyclesClient).when(logbookLifeCyclesClientFactory).getClient();
        doReturn(metaDataClient).when(metaDataClientFactory).getClient();
    }

    @Test
    @RunWithCustomExecutor
    public void testDetachment() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(0);

        // Given
        String containedId = GUIDFactory.newGUID().toString();
        String unitId = GUIDFactory.newGUID().toString();
        final WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters()
            .setWorkerGUID(GUIDFactory.newGUID().getId())
            .setContainerName(containedId)
            .setObjectNameList(Lists.newArrayList(unitId))
            .setObjectName(unitId)
            .setCurrentStep("StepName");

        HandlerIO handlerIO = mock(HandlerIO.class);
        doReturn(JsonHandler.toJsonNode(new HashSet<>(Arrays.asList("parentId1", "parentId2"))))
            .when(handlerIO)
            .getJsonFromWorkspace(eq(UNITS_TO_ATTACH_DIR + "/" + unitId));

        // When
        unitAttachmentPlugin.execute(parameters, handlerIO);

        // Then
        verify(metaDataClient).updateUnitById(any(), eq(unitId));

        ArgumentCaptor<LogbookLifeCycleUnitParameters> logbookLCParam = ArgumentCaptor.forClass(
            LogbookLifeCycleUnitParameters.class
        );
        verify(logbookLifeCyclesClient).update(logbookLCParam.capture(), eq(LifeCycleStatusCode.LIFE_CYCLE_COMMITTED));

        assertThat(logbookLCParam.getValue().getStatus()).isEqualTo(StatusCode.OK);
        String evDetData = logbookLCParam.getValue().getMapParameters().get(LogbookParameterName.eventDetailData);
        ReclassificationEventDetails eventDetails = JsonHandler.getFromString(
            evDetData,
            ReclassificationEventDetails.class
        );
        assertThat(eventDetails.getAddedParents()).containsExactlyInAnyOrder("parentId1", "parentId2");
    }
}
