/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.external.client;

import fr.gouv.vitam.common.external.client.AbstractMockClient.FakeInboundResponse;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response.Status;
import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class AbstractMockClientTest {

    /**
     * Test mock implementation.
     */
    @Test
    public void testCheckStatus() {
        final AbstractMockClient client = new AbstractMockClient();
        client.checkStatus();
        assertEquals("/", client.getResourcePath());
        assertEquals("http://localhost:8080", client.getServiceUrl());
        client.consumeAnyEntityAndClose(null);
        client.close();
    }

    @Test
    public void testFakeInboundResponse() {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("test", "test");
        final FakeInboundResponse fakeInboundResponse = new FakeInboundResponse(
            Status.OK,
            "test",
            MediaType.APPLICATION_JSON_TYPE,
            headers
        );
        assertEquals(Status.OK.getStatusCode(), fakeInboundResponse.getStatus());
        assertNotNull(fakeInboundResponse.getStatusInfo());
        assertNotNull(fakeInboundResponse.getEntity());
        assertNotNull(fakeInboundResponse.readEntity(String.class));
        assertNotNull(fakeInboundResponse.readEntity(GenericType.class));
        assertNotNull(fakeInboundResponse.readEntity(String.class, (Annotation[]) null));
        assertNotNull(fakeInboundResponse.readEntity(GenericType.class, (Annotation[]) null));
        assertTrue(fakeInboundResponse.hasEntity());
        assertFalse(fakeInboundResponse.bufferEntity());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, fakeInboundResponse.getMediaType());
        assertFalse(fakeInboundResponse.getLength() > 0);
        assertNotNull(fakeInboundResponse.getHeaders());
    }
}
