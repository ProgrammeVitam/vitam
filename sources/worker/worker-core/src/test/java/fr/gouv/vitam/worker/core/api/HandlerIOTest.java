package fr.gouv.vitam.worker.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.UriPrefix;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class})
public class HandlerIOTest {
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    
    @Before
    public void setUp() {
        workspaceClient = mock(WorkspaceClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
    }

    @Test
    public void testHandlerIO() throws Exception {
        final HandlerIO io = new HandlerIO(GUIDFactory.newGUID().getId(), GUIDFactory.newGUID().getId());
        assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
        final File file = PropertiesUtils.getResourceFile("sip.xml");
        List<IOParameter> in = new ArrayList<>();
        ProcessingUri uri = new ProcessingUri(UriPrefix.MEMORY, "file");
        in.add(new IOParameter().setUri(uri));
        List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(uri));
        // First create a Memory out
        io.addOutIOParameters(out);
        assertEquals(0, io.getInput().size());
        assertEquals(1, io.getOutput().size());
        io.addOuputResult(0, file);
        assertEquals(io.getOutput().get(0), uri);
        assertEquals(io.getOutput(0), uri);
        // Now create a Memory in similar to out
        io.addInIOParameters(in);
        assertEquals(1, io.getInput().size());
        assertEquals(io.getInput().get(0), file);
        assertEquals(io.getInput(0), file);
        List<Class<?>> clasz = new ArrayList<>();
        clasz.add(File.class);
        assertTrue(io.checkHandlerIO(1, clasz));
        assertFalse(io.checkHandlerIO(1, new ArrayList<>()));
        assertFalse(io.checkHandlerIO(0, clasz));
        // Now reset, leaving the HandlerIO empty
        io.reset();
        assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
        // After reset, adding again the very same In must give access to Memory items
        io.addInIOParameters(in);
        assertTrue(io.checkHandlerIO(0, clasz));
        assertEquals(io.getInput().get(0), file);
        // After close, adding again the very same In must give no more access to Memory items
        io.close();
        assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
        io.addInIOParameters(in);
        assertFalse(io.checkHandlerIO(0, clasz));
        assertNull(io.getInput(0));
    }

    @Test
    public void testGetFileFromHandlerIO() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(PropertiesUtils.getResourceAsStream("sip.xml"));

        try (final HandlerIO io = new HandlerIO("containerName", "workerId")) {
            assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
            List<IOParameter> in = new ArrayList<>();
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));
            List<IOParameter> out = new ArrayList<>();
            out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")));
            io.addInIOParameters(in);
            io.addOutIOParameters(out);
            Object object = io.getInput(0);
            assertEquals(File.class, object.getClass());
            
            io.addOuputResult(0, object, true);
            assertFalse(((File) object).exists());
        }
    }

    @Test
    public void testConcurrentGetFileFromHandlerIO() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(PropertiesUtils.getResourceAsStream("sip.xml"));

        final HandlerIO io = new HandlerIO("containerName", "workerId");
        assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
        List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));

        final HandlerIO io2 = new HandlerIO("containerName", "workerId2");
        assertTrue(io2.checkHandlerIO(0, new ArrayList<>()));

        io.addInIOParameters(in);
        Object object = io.getInput(0);
        assertEquals(File.class, object.getClass());
        assertTrue(((File) object).exists());
        
        when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(PropertiesUtils.getResourceAsStream("sip.xml"));
        io2.addInIOParameters(in);
        Object object2 = io2.getInput(0);
        assertEquals(File.class, object2.getClass());
        assertTrue(((File) object2).exists());
        
        io.close();
        assertFalse(((File) object).exists());
        assertTrue(((File) object2).exists());
        io2.close();
        assertFalse(((File) object2).exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFileError() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject())).thenThrow(new ContentAddressableStorageNotFoundException(""));
        
        try (final HandlerIO io = new HandlerIO("containerName", "workerId")) {
            assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
            List<IOParameter> in = new ArrayList<>();
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(false));
            io.addInIOParameters(in);
        }
    }

}
