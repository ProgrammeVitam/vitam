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
package fr.gouv.vitam.worker.core.handler;

import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtils.CheckSedaValidationStatus;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CheckSedaActionHandlerTest {

    private SedaUtilsFactory sedaUtilsFactory = mock(SedaUtilsFactory.class);
    private CheckSedaActionHandler handler = new CheckSedaActionHandler(sedaUtilsFactory);
    private static final String HANDLER_ID = "CHECK_SEDA";
    private HandlerIOImpl action;
    private SedaUtils sedaUtils;
    private List<IOParameter> in;
    private GUID guid;

    @Before
    public void setUp() {
        sedaUtils = mock(SedaUtils.class);
        guid = GUIDFactory.newGUID();
        action = new HandlerIOImpl(guid.getId(), "workerId", com.google.common.collect.Lists.newArrayList());
        in = new ArrayList<>();
        when(sedaUtilsFactory.createSedaUtils(action)).thenReturn(sedaUtils);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenWorkspaceWhenXmlNotExistThenReturnResponseKO() throws ProcessingException {
        Mockito.doReturn(CheckSedaValidationStatus.NO_FILE).when(sedaUtils).checkSedaValidation(any());
        when(sedaUtils.getMandatoryValues(any())).thenThrow(new ProcessingException(""));
        assertNotNull(CheckSedaActionHandler.getId());
        assertEquals(CheckSedaActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        // itemId is used as eventType so do not change it
        assertEquals(response.getItemId(), HANDLER_ID);
        // global outcome detail subcode is used to specify error type
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "NO_FILE");
    }

    @Test
    public void givenWorkspaceWhenXmlExistThenReturnResponseOK() {
        Mockito.doReturn(CheckSedaValidationStatus.VALID).when(sedaUtils).checkSedaValidation(any());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void givenWorkspaceWhenXmlIsEmptyThenReturnResponseKO() {
        Mockito.doReturn(CheckSedaValidationStatus.NOT_XSD_VALID).when(sedaUtils).checkSedaValidation(any());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        // itemId is used as eventType so do not change it
        assertEquals(response.getItemId(), HANDLER_ID);
        // global outcome detail subcode is used to specify error type
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "NOT_XSD_VALID");
    }

    @Test
    public void givenWorkspaceWhenFileNotXmlThenReturnResponseKO() {
        Mockito.doReturn(CheckSedaValidationStatus.NOT_XML_FILE).when(sedaUtils).checkSedaValidation(any());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        // itemId is used as eventType so do not change it
        assertEquals(response.getItemId(), HANDLER_ID);
        // global outcome detail subcode is used to specify error type
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "NOT_XML_FILE");
    }

    @Test
    public void givenWorkspaceWhenXmlNotThereThenReturnResponseKO() {
        Mockito.doReturn(CheckSedaValidationStatus.NO_FILE).when(sedaUtils).checkSedaValidation(any());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        // itemId is used as eventType so do not change it
        assertEquals(response.getItemId(), HANDLER_ID);
        // global outcome detail subcode is used to specify error type
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "NO_FILE");
    }

    @Test
    public void givenWorkspaceWhenThereAreManyManifestThenReturnResponseKO() {
        Mockito.doReturn(CheckSedaValidationStatus.MORE_THAN_ONE_MANIFEST).when(sedaUtils).checkSedaValidation(any());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        // itemId is used as eventType so do not change it
        assertEquals(response.getItemId(), HANDLER_ID);
        // global outcome detail subcode is used to specify error type
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "CONTAINER_FORMAT.FILE");
    }

    @Test
    public void givenWorkspaceWhenThereAreManyFolderThenReturnResponseKO() {
        Mockito.doReturn(CheckSedaValidationStatus.MORE_THAN_ONE_FOLDER_CONTENT)
            .when(sedaUtils)
            .checkSedaValidation(any());
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters()
            .setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectNameList(Lists.newArrayList("objectName.json"))
            .setObjectName("objectName.json")
            .setCurrentStep("currentStep")
            .setContainerName(guid.getId());
        final ItemStatus response = handler.execute(params, action);
        assertEquals(response.getGlobalStatus(), StatusCode.KO);
        // itemId is used as eventType so do not change it
        assertEquals(response.getItemId(), HANDLER_ID);
        // global outcome detail subcode is used to specify error type
        assertEquals(response.getGlobalOutcomeDetailSubcode(), "CONTAINER_FORMAT.DIRECTORY");
    }
}
