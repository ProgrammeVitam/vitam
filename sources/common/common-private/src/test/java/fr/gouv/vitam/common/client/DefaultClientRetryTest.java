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
package fr.gouv.vitam.common.client;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication;
import fr.gouv.vitam.common.serverv2.VitamServerTestRunner;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.jetty.server.internal.HttpConnection;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DefaultClientRetryTest extends ResteasyTestApplication {

    private static final String RESOURCE_PATH = "/vitam-test/v1";

    private static final ExpectedResults mock = mock(ExpectedResults.class);

    private static DefaultClient client;

    static TestVitamClientFactory<DefaultClient> factory = new TestVitamClientFactory<>(1, RESOURCE_PATH);
    public static VitamServerTestRunner vitamServerTestRunner = new VitamServerTestRunner(
        DefaultClientRetryTest.class,
        factory
    );

    @BeforeClass
    public static void setUpBeforeClass() throws Throwable {
        vitamServerTestRunner.start();
        client = (DefaultClient) vitamServerTestRunner.getClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Throwable {
        vitamServerTestRunner.runAfter();
    }

    @Before
    public void before() {
        reset(mock);
    }

    @Override
    public Set<Object> getResources() {
        return Sets.newHashSet(new MockResource(mock));
    }

    @Path(RESOURCE_PATH)
    @ApplicationPath("webresources")
    public static class MockResource {

        private final ExpectedResults mock;

        public MockResource(ExpectedResults mock) {
            this.mock = mock;
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public void fatalGet() {
            mock.get();
            abort();
        }

        @HEAD
        @Produces(MediaType.APPLICATION_JSON)
        public void fatalHead() {
            mock.head();
            abort();
        }

        @OPTIONS
        @Produces(MediaType.APPLICATION_JSON)
        public void abortOptions() {
            mock.options();
            abort();
        }

        @DELETE
        @Produces(MediaType.APPLICATION_JSON)
        public void fatalDelete() {
            mock.delete();
            abort();
        }

        @PUT
        @Produces(MediaType.APPLICATION_JSON)
        public void fatalPut() throws IOException {
            mock.put();
            abort();
        }

        @POST
        @Produces(MediaType.APPLICATION_JSON)
        public void fatalPost() throws IOException {
            mock.post();
            abort();
        }

        private static void abort() {
            HttpConnection connection = HttpConnection.getCurrentConnection();
            if (connection != null) {
                connection.getEndPoint().close();
            }
        }
    }

    @Test
    public void shouldRetryGet() {
        assertThatThrownBy(() -> client.make(VitamRequestBuilder.get().withPath("/"))).isInstanceOf(
            VitamClientInternalException.class
        );
        verify(mock, times(3)).get();
    }

    @Test
    public void shouldRetryGetWithSerializableBody() {
        assertThatThrownBy(
            () -> client.make(VitamRequestBuilder.get().withJson().withBody("{}").withPath("/"))
        ).isInstanceOf(VitamClientInternalException.class);
        verify(mock, times(3)).get();
    }

    @Test
    public void shouldNotRetryGetWithInputStreamBody() {
        assertThatThrownBy(
            () ->
                client.make(
                    VitamRequestBuilder.get()
                        .withJson()
                        .withBody(new ByteArrayInputStream("{}".getBytes()))
                        .withPath("/")
                )
        ).isInstanceOf(VitamClientInternalException.class);
        verify(mock, times(1)).get();
    }

    @Test
    public void shouldRetryHead() {
        assertThatThrownBy(() -> client.make(VitamRequestBuilder.head().withPath("/"))).isInstanceOf(
            VitamClientInternalException.class
        );
        verify(mock, times(3)).head();
    }

    @Test
    public void shouldRetryDelete() {
        assertThatThrownBy(() -> client.make(VitamRequestBuilder.delete().withPath("/"))).isInstanceOf(
            VitamClientInternalException.class
        );
        verify(mock, times(3)).delete();
    }

    @Test
    public void shouldNotRetryPost() {
        assertThatThrownBy(() -> client.make(VitamRequestBuilder.post().withPath("/"))).isInstanceOf(
            VitamClientInternalException.class
        );
        verify(mock, times(1)).post();
    }

    @Test
    public void shouldNotRetryPostWithSerializableBody() {
        assertThatThrownBy(
            () -> client.make(VitamRequestBuilder.post().withJson().withBody("{}").withPath("/"))
        ).isInstanceOf(VitamClientInternalException.class);
        verify(mock, times(1)).post();
    }

    @Test
    public void shouldNotRetryPostWithInputStreamBody() {
        assertThatThrownBy(
            () ->
                client.make(
                    VitamRequestBuilder.post()
                        .withJson()
                        .withBody(new ByteArrayInputStream("{}".getBytes()))
                        .withPath("/")
                )
        ).isInstanceOf(VitamClientInternalException.class);
        verify(mock, times(1)).post();
    }

    @Test
    public void shouldRetryPut() {
        assertThatThrownBy(() -> client.make(VitamRequestBuilder.put().withPath("/"))).isInstanceOf(
            VitamClientInternalException.class
        );
        verify(mock, times(3)).put();
    }

    @Test
    public void shouldRetryPutWithSerializableBody() {
        assertThatThrownBy(
            () -> client.make(VitamRequestBuilder.put().withJson().withBody("{}").withPath("/"))
        ).isInstanceOf(VitamClientInternalException.class);
        verify(mock, times(3)).put();
    }

    @Test
    public void shouldNotRetryPutWithInputStreamBody() {
        assertThatThrownBy(
            () ->
                client.make(
                    VitamRequestBuilder.put()
                        .withJson()
                        .withBody(new ByteArrayInputStream("{}".getBytes()))
                        .withPath("/")
                )
        ).isInstanceOf(VitamClientInternalException.class);
        verify(mock, times(1)).put();
    }
}
