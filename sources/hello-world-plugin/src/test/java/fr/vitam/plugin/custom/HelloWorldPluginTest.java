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
package fr.vitam.plugin.custom;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class HelloWorldPluginTest {

    @Test
    public void testExecuteThenOK() {
        HelloWorldPlugin helloWorldPlugin = new HelloWorldPlugin();
        WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
        HandlerIO handlerIO = mock(HandlerIO.class);
        doReturn("HelloWorldPluginTest").when(handlerIO).getInput(0);
        ItemStatus itemStatus = helloWorldPlugin.execute(parameters, handlerIO);
        assertTrue(itemStatus != null);
        assertTrue(itemStatus.getEvDetailData().contains("var_name"));
        assertTrue(itemStatus.getEvDetailData().contains("HelloWorldPluginTest"));
    }

    @Test
    public void testExecuteListThenOK() throws ContentAddressableStorageServerException, ProcessingException {
        HelloWorldPlugin helloWorldPlugin = new HelloWorldPlugin();
        WorkerParameters parameters = WorkerParametersFactory.newWorkerParameters();
        parameters.setObjectNameList(Lists.newArrayList("objectId"));
        HandlerIO handlerIO = mock(HandlerIO.class);
        doReturn("HelloWorldPluginTest").when(handlerIO).getInput(0);
        List<ItemStatus> itemStatus = helloWorldPlugin.executeList(parameters, handlerIO);
        assertTrue(itemStatus != null);
        assertTrue(itemStatus.size() == 1);
        assertTrue(itemStatus.get(0).getEvDetailData().contains("var_name"));
        assertTrue(itemStatus.get(0).getEvDetailData().contains("HelloWorldPluginTest"));
    }
}
