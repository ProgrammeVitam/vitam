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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, SedaUtilsFactory.class})
public class CheckConformityActionHandlerTest {
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP = "OBJECT_GROUP_ID_TO_GUID_MAP_obj.json";
    private static final String BDO_TO_OBJECT_GROUP_ID_MAP = "BDO_TO_OBJECT_GROUP_ID_MAP_obj.json";
    private static final String SRC_TEST_RESOURCE = "src/test/resource";
    private static final String MESSAGE_DIGEST = "ZGVmYXVsdA==";
    CheckConformityActionHandler conformityHandler;
    private static final String HANDLER_ID = "CheckConformity";
    private static final String SIP = "sip.xml";
    private SedaUtils sedaUtils;
    private HandlerIO action;
    private WorkspaceClient workspaceClient;
    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
            .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
        PowerMockito.when(SedaUtilsFactory.create()).thenReturn(sedaUtils);

        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.create(anyObject())).thenReturn(workspaceClient);
        action = new HandlerIO("containerName");
    }

    @Test
    public void givenConformityCheckWhenTrueThenResponseOK()
        throws Exception {
        when(workspaceClient.computeObjectDigest(anyObject(), anyObject(), anyObject())).thenReturn(MESSAGE_DIGEST);

        action.addInput(PropertiesUtils.getResourceFile(SIP));
        action.addInput(PropertiesUtils.getResourceFile(BDO_TO_OBJECT_GROUP_ID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
        conformityHandler = new CheckConformityActionHandler();
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = conformityHandler.execute(params, action);
        assertEquals(response.getStatus(), StatusCode.OK);
        assertEquals(OutcomeMessage.CHECK_CONFORMITY_OK, response.getOutcomeMessages().get("CheckConformity"));
    }

    @Test
    public void givenConformityCheckWhenWrongDigestThenKO()
        throws Exception {
        when(workspaceClient.computeObjectDigest(anyObject(), anyObject(), anyObject())).thenReturn("Wrong digest");
        action.addInput(PropertiesUtils.getResourceFile(SIP));
        action.addInput(PropertiesUtils.getResourceFile(BDO_TO_OBJECT_GROUP_ID_MAP));
        action.addInput(PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP));
        conformityHandler = new CheckConformityActionHandler();
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);

        final EngineResponse response = conformityHandler.execute(params, action);
        assertEquals(response.getStatus(), StatusCode.KO);
        assertEquals(OutcomeMessage.CHECK_CONFORMITY_KO, response.getOutcomeMessages().get("CheckConformity"));
    }

    @Test
    public void givenConformityCheckWhenWorkspaceErrorThenKO()
        throws Exception {
        action.addInput(new File(SRC_TEST_RESOURCE + SIP));
        action.addInput(new File(SRC_TEST_RESOURCE + "BDO_TO_OBJECT_GROUP_ID_MAP_obj.jsons"));
        action.addInput(new File(SRC_TEST_RESOURCE + OBJECT_GROUP_ID_TO_GUID_MAP));
        conformityHandler = new CheckConformityActionHandler();
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = conformityHandler.execute(params, action);
        assertEquals(response.getStatus(), StatusCode.KO);
        assertEquals(OutcomeMessage.CHECK_CONFORMITY_KO, response.getOutcomeMessages().get("CheckConformity"));
    }
}
