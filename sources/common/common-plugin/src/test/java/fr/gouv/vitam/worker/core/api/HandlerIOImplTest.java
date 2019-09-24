package fr.gouv.vitam.worker.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.common.CompressInformation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class HandlerIOImplTest {

    private static final String CURRENT_OBJECT = "1";
    private static final ArrayList<String> OBJECT_IDS = Lists.newArrayList(CURRENT_OBJECT);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClient workspaceClient;
    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;


    private HandlerIO handlerIO;

    @Before
    public void init() {
        handlerIO =
            new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, GUIDFactory.newGUID().getId(),
                GUIDFactory.newGUID().getId(), OBJECT_IDS);
        handlerIO.setCurrentObjectId(CURRENT_OBJECT);

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
    }

    @Test
    public void testHandlerIO() throws Exception {
        handlerIO.setCurrentObjectId(CURRENT_OBJECT);

        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        final File file = PropertiesUtils.getResourceFile("sip.xml");
        final List<IOParameter> in = new ArrayList<>();
        final ProcessingUri uri = new ProcessingUri(UriPrefix.MEMORY, "file");
        in.add(new IOParameter().setUri(uri));
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(uri));
        // First create a Memory out
        handlerIO.addOutIOParameters(out);
        assertEquals(0, handlerIO.getInput().size());
        assertEquals(1, handlerIO.getOutput().size());
        handlerIO.addOutputResult(0, file, false);
        assertEquals(handlerIO.getOutput().get(0), uri);
        assertEquals(handlerIO.getOutput(0), uri);
        // Now create a Memory in similar to out
        handlerIO.addInIOParameters(in);
        assertEquals(1, handlerIO.getInput().size());
        assertEquals(handlerIO.getInput().get(0), file);
        assertEquals(handlerIO.getInput(0), file);
        final List<Class<?>> clasz = new ArrayList<>();
        clasz.add(File.class);
        assertTrue(handlerIO.checkHandlerIO(1, clasz));
        assertFalse(handlerIO.checkHandlerIO(1, new ArrayList<>()));
        assertFalse(handlerIO.checkHandlerIO(0, clasz));
        // Now reset, leaving the HandlerIO empty
        handlerIO.reset();
        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        // After reset, adding again the very same In must give access to Memory items
        handlerIO.addInIOParameters(in);
        assertTrue(handlerIO.checkHandlerIO(0, clasz));
        assertEquals(handlerIO.getInput().get(0), file);
        // After close, adding again the very same In must give no more access to Memory items
        handlerIO.close();
        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        handlerIO.addInIOParameters(in);
        assertFalse(handlerIO.checkHandlerIO(0, clasz));
        assertNull(handlerIO.getInput(0));
    }

    @Test
    public void testGetFileFromHandlerIO() throws Exception {

        when(workspaceClient.getObject(any(), any()))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build());

        try (final HandlerIO io = new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, "containerName", "workerId", OBJECT_IDS)) {
            io.setCurrentObjectId(CURRENT_OBJECT);
            assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
            final List<IOParameter> in = new ArrayList<>();
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));
            final List<IOParameter> out = new ArrayList<>();
            out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")));
            io.addInIOParameters(in);
            io.addOutIOParameters(out);
            final Object object = io.getInput(0);
            assertEquals(File.class, object.getClass());

            io.addOutputResult(0, object, true, false);
            assertFalse(((File) object).exists());
        }
    }

    @Test
    public void testConcurrentGetFileFromHandlerIO() throws Exception {
        when(workspaceClient.getObject(any(), any()))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build());

        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));

        final HandlerIOImpl io2 = new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, "containerName", "workerId2", OBJECT_IDS);
        io2.setCurrentObjectId(CURRENT_OBJECT);
        assertTrue(io2.checkHandlerIO(0, new ArrayList<>()));

        handlerIO.addInIOParameters(in);
        final Object object = handlerIO.getInput(0);
        assertEquals(File.class, object.getClass());
        assertTrue(((File) object).exists());

        when(workspaceClient.getObject(any(), any()))
            .thenReturn(Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build());
        io2.addInIOParameters(in);
        final Object object2 = io2.getInput(0);
        assertEquals(File.class, object2.getClass());
        assertTrue(((File) object2).exists());
        handlerIO.close();
        assertFalse(((File) object).exists());
        assertTrue(((File) object2).exists());
        io2.close();
        assertFalse(((File) object2).exists());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetFileError() throws Exception {
        when(workspaceClient.getObject(any(), any()))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));

        try (final HandlerIO io = new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, "containerName", "workerId", OBJECT_IDS)) {
            assertTrue(io.checkHandlerIO(0, new ArrayList<>()));
            final List<IOParameter> in = new ArrayList<>();
            in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(false));
            io.addInIOParameters(in);
        }
    }
}
