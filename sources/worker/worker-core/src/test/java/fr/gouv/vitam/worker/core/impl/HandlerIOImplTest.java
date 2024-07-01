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
package fr.gouv.vitam.worker.core.impl;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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
        handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            GUIDFactory.newGUID().getId(),
            GUIDFactory.newGUID().getId(),
            OBJECT_IDS
        );
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
        when(workspaceClient.getObject(any(), eq("objectName1"))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build()
        );
        when(workspaceClient.getObject(any(), eq("objectName2"))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip1.xml")).build()
        );

        handlerIO.setCurrentObjectId(CURRENT_OBJECT);
        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName1")).setOptional(false));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName2")).setOptional(true));
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName3")));
        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);
        final Object object1 = handlerIO.getInput(0);
        assertEquals(File.class, object1.getClass());
        final Object object2 = handlerIO.getInput(1);
        assertEquals(File.class, object2.getClass());

        handlerIO.addOutputResult(0, object1, true, false);
        assertFalse(((File) object1).exists());
    }

    @Test
    public void testConcurrentGetFileFromHandlerIO() throws Exception {
        when(workspaceClient.getObject(any(), any())).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build()
        );

        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));

        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(false));
        handlerIO.addInIOParameters(in);

        final Object object = handlerIO.getInput(0);
        assertEquals(File.class, object.getClass());
        assertTrue(((File) object).exists());

        final HandlerIOImpl io2 = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "containerName",
            "workerId2",
            OBJECT_IDS
        );
        io2.setCurrentObjectId(CURRENT_OBJECT);
        assertTrue(io2.checkHandlerIO(0, new ArrayList<>()));

        when(workspaceClient.getObject(any(), any())).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("sip.xml")).build()
        );
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

    @Test
    public void testGetFileError() throws Exception {
        when(workspaceClient.getObject(any(), any())).thenThrow(new ContentAddressableStorageNotFoundException(""));

        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(false));
        handlerIO.addInIOParameters(in);
        assertThatThrownBy(() -> handlerIO.getFile(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testGetLazyOptinalFileError() {
        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(true));
        handlerIO.addInIOParameters(in);
        assertNull(handlerIO.getFile(0));
    }

    @Test
    public void testGetLazyFileError() {
        assertTrue(handlerIO.checkHandlerIO(0, new ArrayList<>()));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE, "objectName")).setOptional(false));
        handlerIO.addInIOParameters(in);
        assertThatCode(() -> handlerIO.getFile(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Handler input not found exception: objectName");
    }
}
