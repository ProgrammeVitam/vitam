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
package fr.gouv.vitam.common.server.application.junit;

import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.server.application.junit.ResteasyTestApplication.ExpectedResults;
import io.undertow.Undertow;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.junit.rules.ExternalResource;

import javax.ws.rs.core.Application;

import static org.mockito.Mockito.mock;

public class VitamServerTestRunner extends ExternalResource {
    private final UndertowJaxrsServer server;
    final MockOrRestClient _client;
    final VitamClientFactoryInterface<?> factory;

    public VitamServerTestRunner(Class<? extends Application> application, VitamClientFactoryInterface<?> factory, int serverPortNumber) {

        this.factory = factory;
        Undertow.Builder builder =
            Undertow.builder().addHttpListener(serverPortNumber, "localhost");
        server = new UndertowJaxrsServer().start(builder);
        server.deploy(application);
        factory.changeServerPort(serverPortNumber);
        _client = factory.getClient();
    }

    @Override
    protected void after() throws Throwable {
        server.stop();
    }

    public MockOrRestClient getClient() {
        return _client;
    }

    public void runAfter() throws Throwable {
        after();
    }

    public ExpectedResults getExpectedResultsMock() {
       return mock(ExpectedResults.class);
    }
}
