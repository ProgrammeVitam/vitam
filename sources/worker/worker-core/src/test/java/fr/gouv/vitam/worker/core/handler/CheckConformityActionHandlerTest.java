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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, SedaUtilsFactory.class})
public class CheckConformityActionHandlerTest {
    CheckConformityActionHandler handler;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private static final String HANDLER_ID = "CHECK_DIGEST";

    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaaaaababaumakxynrf3sqaaaaq.json";
    private InputStream objectGroup;
    private static final String bdo1 =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp";
    private static final String bdo2 =
        "d156f4a4cc725cc6eaaafdcb7936c9441d25bdf033e4e2f1852cf540d39713446cfcd42f2ba087eb66f3f9dbfeca338180ca64bdde645706ec14499311d557f4.txt";
    private static final String bdo3 =
        "fe2b0664fc66afd85f839be6ee4b6433b60a06b9a4481e0743c9965394fa0b8aa51b30df11f3281fef3d7f6c86a35cd2925351076da7abc064ad89369edf44f0.png";
    private static final String bdo4 = "f332ca3fd108067eb3500df34283485a1c35e36bdf8f4bd3db3fd9064efdb954.pdf";

    @Before
    public void setUp() {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
    }

    @After
    public void setDown() {}

    @Test
    public void getNonStandardDigestUpdate() throws Exception {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        when(workspaceClient.getObject(anyObject(), eq("ObjectGroup/objName")))
            .thenReturn(Response.status(Status.OK).entity(objectGroup).build());
        when(workspaceClient.getObject(anyObject(), eq("SIP/content/" + bdo1)))
            .thenReturn(
                Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo1)).build());

        when(workspaceClient.getObject(anyObject(), eq("SIP/content/" + bdo2)))
            .thenReturn(
                Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo2)).build());
        when(workspaceClient.getObject(anyObject(), eq("SIP/content/" + bdo3)))
            .thenReturn(
                Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo3)).build());
        when(workspaceClient.getObject(anyObject(), eq("SIP/content/" + bdo4)))
            .thenReturn(
                Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo4)).build());

        // assertNotNull(objectGroup);
        handler = new CheckConformityActionHandler();
        final WorkerParameters params = getDefaultWorkerParameters();
        final HandlerIOImpl handlerIO = new HandlerIOImpl("CheckConformityActionHandlerTest", "workerId");
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "SHA-512")));
        handlerIO.addInIOParameters(in);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final ItemStatus response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        handlerIO.close();
    }

    private DefaultWorkerParameters getDefaultWorkerParameters() {
        return WorkerParametersFactory.newWorkerParameters("pId", "stepId", "CheckConformityActionHandlerTest",
            "currentStep", "objName", "metadataURL", "workspaceURL");
    }
}
