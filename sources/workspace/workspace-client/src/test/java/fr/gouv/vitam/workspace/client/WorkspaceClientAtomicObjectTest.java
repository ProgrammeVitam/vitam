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
package fr.gouv.vitam.workspace.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WorkspaceClientAtomicObjectTest extends ResteasyTestApplication {

    protected static WorkspaceClient client;
    private static final ExpectedResults mock = mock(ExpectedResults.class);

    private static final String CONTAINER_NAME = "myContainer";
    private static final String OBJECT_NAME = "myObject";

    static WorkspaceClientFactory factory = WorkspaceClientFactory.getInstance();

    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        WorkspaceClientAtomicObjectTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (WorkspaceClient) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    private InputStream stream = null;

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockObjectResource(mock));
    }

    @Path("workspace/v1/")
    public static class MockObjectResource {

        private final ExpectedResults mock;

        public MockObjectResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @Path("atomic_containers/{containerName}/objects/{objectName}")
        @POST
        @Consumes(MediaType.APPLICATION_OCTET_STREAM)
        @Produces(MediaType.APPLICATION_JSON)
        public Response putAtomicObject(
            InputStream stream,
            @PathParam("containerName") String containerName,
            @PathParam("objectName") String objectName
        ) {
            return mock.post();
        }
    }

    // create
    @Test(expected = IllegalArgumentException.class)
    public void givenNullParamWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1.pdf");
        long length = getLength("file1.pdf");
        client.putAtomicObject(CONTAINER_NAME, null, stream, length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenEmptyParamWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1.pdf");
        long length = getLength("file1.pdf");
        client.putAtomicObject(CONTAINER_NAME, "", stream, length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenInvalidLengthWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1.pdf");
        client.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, stream, -1);
    }

    @Test(expected = ContentAddressableStorageServerException.class)
    public void givenServerErrorWhenCreateObjectThenRaiseAnException() throws Exception {
        stream = getInputStream("file1.pdf");
        long length = getLength("file1.pdf");
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        client.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, stream, length);
    }

    @Test
    public void givenObjectNotFoundWhenCreateObjectThenReturnCreated() throws Exception {
        stream = getInputStream("file1.pdf");
        long length = getLength("file1.pdf");
        when(mock.post()).thenReturn(Response.status(Status.CREATED).build());
        client.putAtomicObject(CONTAINER_NAME, OBJECT_NAME, stream, length);
    }

    private InputStream getInputStream(String file) throws FileNotFoundException {
        return PropertiesUtils.getResourceAsStream(file);
    }

    private long getLength(String file) throws FileNotFoundException {
        return PropertiesUtils.getResourceFile(file).length();
    }
}
