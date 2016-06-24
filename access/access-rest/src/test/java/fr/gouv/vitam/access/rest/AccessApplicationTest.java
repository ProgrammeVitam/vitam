/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.eclipse.persistence.jaxb.rs.MOXyJsonProvider;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import fr.gouv.vitam.access.api.AccessResource;
import fr.gouv.vitam.access.model.UnitRequestDTO;

/**
 * AccessApplication Test class
 */
public class AccessApplicationTest extends JerseyTest {

    private final AccessApplication application = new AccessApplication();

    private Client client;
    private WebTarget webTarget;

    @Override
    public Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.CONTAINER_FACTORY);
        enable(TestProperties.CONTAINER_PORT);
        set(TestProperties.CONTAINER_PORT, "8082");
        final ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("fr.gouv.vitam.access.rest");
        resourceConfig.register(JacksonFeature.class);
        resourceConfig.register(AccessResourceMock.class);
        resourceConfig.register(AccessResource.class);
        resourceConfig.register(MOXyJsonProvider.class);

        return resourceConfig;
    }


    @Before
    public void setUpBeforeMethod() throws Exception {
        client = ClientBuilder.newClient();
    }


    @Test(expected = IllegalArgumentException.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithEmptyArgs() throws Exception {
        application.configure(new String[0]);
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionWhenConfigureApplicationWithFileNotFound() throws Exception {
        application.configure("src/test/resources/notFound.conf");
    }

    @Test
    public void shouldRunServerWhenConfigureApplicationWithFileExists() throws Exception {
        application.configure("src/test/resources/access.conf", "8088");
    }

    @Ignore
    @Test
    public void shouldUseDefaultPortToRunServerWhenConfigureApplicationWithPortNegative() throws Exception {
        application.configure("src/test/resources/access.conf", "-12");
    }

    @Test(expected = Exception.class)
    public void shouldRaiseAnExceptionUseDefaultPortToRunServerWhenConfigureApplicationWithNAN() throws Exception {
        application.configure("src/test/resources/access.conf", "AA");
    }

    @Ignore
    @Test
    public void shouldExecuteStatusServiceRest() throws URISyntaxException {
        webTarget = client.target(new URI(getBaseUri() + "accessMock/status"));
        final Invocation.Builder builder = webTarget.request();
        final Response response = builder.get();
        final String status = response.readEntity(String.class);
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }


    @Ignore
    @Test
    public void shouldExecuteGetUnitsServiceRest() throws URISyntaxException {
        webTarget = client.target(new URI(getBaseUri() + "accessMock/units"));
        final Invocation.Builder builder = webTarget.request();

        final UnitRequestDTO statusRequestDTO = new UnitRequestDTO("queryDsl");
        final Entity<UnitRequestDTO> entity = Entity.json(statusRequestDTO);
        final Response response = builder.post(entity);

        final String status = response.readEntity(String.class);
        assertNotNull(response);
        assertEquals(200, response.getStatus());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseAnException_WhenExecuteMainWithEmptyArgs() throws Exception {
        AccessApplication.main(new String[0]);
    }
}
