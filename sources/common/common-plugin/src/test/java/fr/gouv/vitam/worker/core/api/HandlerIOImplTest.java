package fr.gouv.vitam.worker.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.common.CompressInformation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class})
public class HandlerIOImplTest {
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
        final HandlerIOImpl io = new HandlerIOImpl(GUIDFactory.newGUID().getId(), GUIDFactory.newGUID().getId());
        assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
        final File file = PropertiesUtils.getResourceFile("sip.xml");
        final List<IOParameter> in = new ArrayList<>();
        final ProcessingUri uri = new ProcessingUri(UriPrefix.MEMORY, "file");
        in.add(new IOParameter().setUri(uri));
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(uri));
        // First create a Memory out
        io.addOutIOParameters(out);
        assertEquals(0, io.getInput().size());
        assertEquals(1, io.getOutput().size());
        io.addOuputResult(0, file, false);
        assertEquals(io.getOutput().get(0), uri);
        assertEquals(io.getOutput(0), uri);
        // Now create a Memory in similar to out
        io.addInIOParameters(in);
        assertEquals(1, io.getInput().size());
        assertEquals(io.getInput().get(0), file);
        assertEquals(io.getInput(0), file);
        final List<Class<?>> clasz = new ArrayList<>();
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

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build());

        try (final HandlerIO io = new HandlerIOImpl("containerName", "workerId")) {
            assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
            final List<IOParameter> in = new ArrayList<>();
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));
            final List<IOParameter> out = new ArrayList<>();
            out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")));
            io.addInIOParameters(in);
            io.addOutIOParameters(out);
            final Object object = io.getInput(0);
            assertEquals(File.class, object.getClass());

            io.addOuputResult(0, object, true, false);
            assertFalse(((File) object).exists());
        }
    }

    @Test
    public void testConcurrentGetFileFromHandlerIO() throws Exception {
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build());

        final HandlerIOImpl io = new HandlerIOImpl("containerName", "workerId");
        assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));

        final HandlerIOImpl io2 = new HandlerIOImpl("containerName", "workerId2");
        assertTrue(io2.checkHandlerIO(0, new ArrayList<>()));

        io.addInIOParameters(in);
        final Object object = io.getInput(0);
        assertEquals(File.class, object.getClass());
        assertTrue(((File) object).exists());

        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build());
        io2.addInIOParameters(in);
        final Object object2 = io2.getInput(0);
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
        when(workspaceClient.getObject(anyObject(), anyObject()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));

        try (final HandlerIO io = new HandlerIOImpl("containerName", "workerId")) {
            assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
            final List<IOParameter> in = new ArrayList<>();
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(false));
            io.addInIOParameters(in);
        }
    }

    @Test
    public void should_compress_file() throws Exception {
        // Given
        WorkspaceClient workspaceClient = mock(WorkspaceClient.class);
        String containerName = "containerName";
        HandlerIO handlerIO = new HandlerIOImpl(workspaceClient, containerName, "workerId");
        when(workspaceClient.isExistingContainer(containerName)).thenReturn(true);

        // When
        handlerIO.zipWorkspace("test.zip", "1", "2");

        // Then
        CompressInformation compressInformation = new CompressInformation(Lists.newArrayList("1", "2"), "test.zip");
        verify(workspaceClient).compress(containerName, compressInformation);
    }

}
