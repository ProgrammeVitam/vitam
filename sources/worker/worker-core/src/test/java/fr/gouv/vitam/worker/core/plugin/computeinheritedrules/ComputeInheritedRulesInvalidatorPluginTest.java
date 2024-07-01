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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.set;
import static fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper.unset;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ComputeInheritedRulesInvalidatorPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private MetaDataClient metaDataClient;

    private ComputeInheritedRulesInvalidatorPlugin computeInheritedRulesInvalidatorPlugin;

    @Before
    public void setUp() throws Exception {
        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        computeInheritedRulesInvalidatorPlugin = new ComputeInheritedRulesInvalidatorPlugin(metaDataClientFactory);
    }

    @Test
    public void should_invalidate_correctly_unit() throws Exception {
        String unitToUpdate = "aeeaaaaaagehslhxabfh2all2cnh4zyaaaaq";

        HandlerIO handlerIO = mock(HandlerIO.class);
        WorkerParameters workerParameters = mock(WorkerParameters.class);
        when(workerParameters.getObjectNameList()).thenReturn(Collections.singletonList(unitToUpdate));

        when(metaDataClient.updateUnitBulk(any(JsonNode.class))).thenReturn(new RequestResponseOK<JsonNode>());

        computeInheritedRulesInvalidatorPlugin.executeList(workerParameters, handlerIO);

        ArgumentCaptor valueCapture = ArgumentCaptor.forClass(ObjectNode.class);
        verify(metaDataClient).updateUnitBulk((JsonNode) valueCapture.capture());

        UpdateMultiQuery expectedUpdate = new UpdateMultiQuery();
        expectedUpdate.addRoots(unitToUpdate);
        expectedUpdate.addActions(
            set(VitamFieldsHelper.validComputedInheritedRules(), false),
            unset(VitamFieldsHelper.computedInheritedRules())
        );
        ObjectNode expectedArgument = expectedUpdate.getFinalUpdate();

        assertEquals(expectedArgument, valueCapture.getValue());
    }
}
