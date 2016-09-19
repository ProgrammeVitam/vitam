package fr.gouv.vitam.worker.core.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;

public class IndexObjectGroupActionHandlerTest {
    
    IndexObjectGroupActionHandler handler;
    private static final String HANDLER_ID = "IndexObjectGroup";
    private SedaUtilsFactory factory;
    private SedaUtils sedaUtils;

    @Before
    public void setUp() {
        factory = mock(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseWARNING()
        throws XMLStreamException, IOException, ProcessingException {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).indexObjectGroup(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new IndexObjectGroupActionHandler(factory);
        assertEquals(IndexObjectGroupActionHandler.getId(), HANDLER_ID);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata
            ("fakeUrl").setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName
            ("containerName");
        final EngineResponse response = handler.execute(params);
        assertEquals(response.getStatus(), StatusCode.WARNING);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK()
        throws XMLStreamException, IOException, ProcessingException {
        Mockito.doNothing().when(sedaUtils).indexObjectGroup(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new IndexObjectGroupActionHandler(factory);
        final WorkerParameters params = WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("fakeUrl").setUrlMetadata
            ("fakeUrl").setObjectName("objectName.json").setCurrentStep("currentStep").setContainerName
            ("containerName");
        final EngineResponse response = handler.execute(params);
        assertEquals(response.getStatus(), StatusCode.OK);
    }

}
