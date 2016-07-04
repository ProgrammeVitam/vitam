package fr.gouv.vitam.processing.worker.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

public class CheckConformityActionHandlerTest {
    CheckConformityActionHandler handlerVersion;
    private static final String HANDLER_ID = "CheckConformity";
    private SedaUtilsFactory factory;
    private SedaUtils sedaUtils;
    private static final WorkParams params =
        new WorkParams()
        .setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""))
        .setGuuid("").setContainerName("Action").setCurrentStep("check conformity");

    @Before
    public void setUp() {
        factory = mock(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
    }
    
    @Test
    public void givenConformityCheckWhenTrueThenResponseOK() 
        throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        List<String> digestMessageInvalidList = new ArrayList<String>();
        Mockito.doReturn(digestMessageInvalidList).when(sedaUtils).checkConformityBinaryObject(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handlerVersion = new CheckConformityActionHandler(factory);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params);
        assertEquals(response.getStatus(), StatusCode.OK);
    }
    
    @Test
    public void givenConformityCheckWhenFalseThenResponseWarning() 
        throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        List<String> digestMessageInvalidList = new ArrayList<String>();
        digestMessageInvalidList.add("ZGVmYXVsdA==");
        digestMessageInvalidList.add("ZGVmYXVsdB==");
        Mockito.doReturn(digestMessageInvalidList).when(sedaUtils).checkConformityBinaryObject(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handlerVersion = new CheckConformityActionHandler(factory);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params);
        assertEquals(response.getStatus(), StatusCode.KO);
    }
    
    @Test
    public void givenConformityCheckWhenExceptionThenResponseKO() 
        throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).checkConformityBinaryObject(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handlerVersion = new CheckConformityActionHandler(factory);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params);
        assertEquals(response.getStatus(), StatusCode.KO);
    }
    
    @Test
    public void givenConformityCheckWhenContentAddressableStorageNotFoundExceptionThenResponseKO() 
        throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        Mockito.doThrow(new ContentAddressableStorageNotFoundException("")).when(sedaUtils).checkConformityBinaryObject(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handlerVersion = new CheckConformityActionHandler(factory);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params);
        assertEquals(response.getStatus(), StatusCode.KO);
    }
    
    @Test
    public void givenConformityCheckWhenContentAddressableStorageServerExceptionThenResponseKO() 
        throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        Mockito.doThrow(new ContentAddressableStorageServerException("")).when(sedaUtils).checkConformityBinaryObject(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handlerVersion = new CheckConformityActionHandler(factory);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params);
        assertEquals(response.getStatus(), StatusCode.KO);
    }
    
    @Test
    public void givenConformityCheckWhenContentAddressableStorageExceptionThenResponseKO() 
        throws ProcessingException, URISyntaxException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, ContentAddressableStorageException{
        Mockito.doThrow(new ContentAddressableStorageException("")).when(sedaUtils).checkConformityBinaryObject(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handlerVersion = new CheckConformityActionHandler(factory);
        assertEquals(CheckConformityActionHandler.getId(), HANDLER_ID);
        final EngineResponse response = handlerVersion.execute(params);
        assertEquals(response.getStatus(), StatusCode.KO);
    }
}
