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
package fr.gouv.vitam.common.server.application.junit;

import com.google.common.base.Strings;
import org.mockito.Mockito;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.Map.Entry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Helper for getting Response as Outbound Response
 */
public class ResponseHelper {

    private ResponseHelper() {
        // Empty
    }

    /**
     * Helper to build an outbound Response (mocking remote client response object)
     *
     * @param status the status of response
     * @param entity could be null
     * @param contentType could be null
     * @param headers could be null
     * @return the mocked outbound Response
     */
    @SuppressWarnings("unchecked")
    public static Response getOutboundResponse(
        Status status,
        Object entity,
        String contentType,
        Map<String, String> headers
    ) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        final Response response = Mockito.mock(Response.class);
        when(response.getStatus()).thenReturn(status.getStatusCode());
        if (entity == null) {
            when(response.readEntity(any(Class.class))).thenReturn("");
        } else {
            when(response.readEntity(any(Class.class))).thenReturn(entity);
        }
        boolean contentTypeFound = false;
        if (!Strings.isNullOrEmpty(contentType)) {
            when(response.getHeaderString("Content-Type")).thenReturn(contentType);
            contentTypeFound = true;
        }
        if (headers != null) {
            for (final Entry<String, String> entry : headers.entrySet()) {
                when(response.getHeaderString(entry.getKey())).thenReturn(entry.getValue());
            }
        }
        if (!contentTypeFound) {
            when(response.getHeaderString("Content-Type")).thenReturn(MediaType.APPLICATION_JSON);
        }
        return response;
    }
}
