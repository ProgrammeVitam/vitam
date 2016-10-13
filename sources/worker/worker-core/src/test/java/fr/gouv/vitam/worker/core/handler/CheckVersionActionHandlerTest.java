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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.worker.core.api.HandlerIO;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({SedaUtilsFactory.class})
public class CheckVersionActionHandlerTest {
    CheckVersionActionHandler handlerVersion = new CheckVersionActionHandler();
    private static final String HANDLER_ID = "CheckVersion";
    private SedaUtils sedaUtils;
    private final WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata
        ("fakeUrl").setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName
        ("containerName");
    private HandlerIO handlerIO = new HandlerIO("");

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
        PowerMockito.when(SedaUtilsFactory.create()).thenReturn(sedaUtils);
    }
    
    @Test
    public void givenWorkspaceExistWhenCheckIsTrueThenReturnResponseOK() 
        throws ProcessingException, IOException, URISyntaxException{
        List<String> invalidVersionList = new ArrayList<String>();
        Mockito.doReturn(invalidVersionList).when(sedaUtils).checkSupportedBinaryObjectVersion(anyObject());
        assertEquals(CheckVersionActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params, handlerIO);
        assertEquals(response.getStatus(), StatusCode.OK);
    }
    
    @Test
    public void givenWorkspaceExistWhenCheckIsFalseThenReturnResponseWarning() 
        throws ProcessingException, IOException, URISyntaxException{
        List<String> invalidVersionList = new ArrayList<String>();
        invalidVersionList.add("PhysicalMaste");
        Mockito.doReturn(invalidVersionList).when(sedaUtils).checkSupportedBinaryObjectVersion(anyObject());
        assertEquals(CheckVersionActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params, handlerIO);
        assertEquals(response.getStatus(), StatusCode.KO);
    }
    
    @Test
    public void givenWorkspaceExistWhenExceptionExistThenReturnResponseFatal() 
        throws ProcessingException, IOException, URISyntaxException{
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).checkSupportedBinaryObjectVersion(anyObject());
        assertEquals(CheckVersionActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params, handlerIO);
        assertEquals(response.getStatus(), StatusCode.FATAL);
    }
}
