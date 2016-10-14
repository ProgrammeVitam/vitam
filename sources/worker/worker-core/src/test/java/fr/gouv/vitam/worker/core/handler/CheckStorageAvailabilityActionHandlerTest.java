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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({SedaUtilsFactory.class})
public class CheckStorageAvailabilityActionHandlerTest {

    CheckStorageAvailabilityActionHandler handler = new CheckStorageAvailabilityActionHandler();
    private static final String HANDLER_ID = "CheckStorageAvailability";
    private SedaUtils sedaUtils;
    private final HandlerIO handlerIO = new HandlerIO("");

    @Before
    public void setUp() {
        PowerMockito.mockStatic(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
    }

    @Test
    public void givenSedaNotExistWhenCheckStorageThenReturnResponseFatal() throws Exception {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils)
            .computeTotalSizeOfObjectsInManifest(anyObject());
        PowerMockito.when(SedaUtilsFactory.create()).thenReturn(sedaUtils);
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");
        final EngineResponse response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getStatus());
        assertTrue(response.getOutcomeMessages().values().contains(OutcomeMessage.STORAGE_OFFER_KO_UNAVAILABLE));
    }

    @Test
    public void givenSedaExistWhenCheckStorageExecuteThenReturnResponseKO() throws Exception {
        Mockito.doReturn(new Long(838860800)).when(sedaUtils)
            .computeTotalSizeOfObjectsInManifest(anyObject());
        Mockito.doReturn(new Long(838860800)).when(sedaUtils)
            .getManifestSize(anyObject());
        PowerMockito.when(SedaUtilsFactory.create()).thenReturn(sedaUtils);
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");
        final EngineResponse response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getStatus());
        assertTrue(response.getOutcomeMessages().values().contains(OutcomeMessage.STORAGE_OFFER_SPACE_KO));
    }


    @Test
    public void givenSedaExistWhenCheckStorageExecuteThenReturnResponseOK() throws Exception {
        Mockito.doReturn(new Long(1024)).when(sedaUtils)
            .computeTotalSizeOfObjectsInManifest(anyObject());
        Mockito.doReturn(new Long(1024)).when(sedaUtils)
            .getManifestSize(anyObject());
        PowerMockito.when(SedaUtilsFactory.create()).thenReturn(sedaUtils);
        assertEquals(CheckStorageAvailabilityActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata("fakeUrl")
                .setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName("containerName");
        final EngineResponse response = handler.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getStatus());
    }

}
