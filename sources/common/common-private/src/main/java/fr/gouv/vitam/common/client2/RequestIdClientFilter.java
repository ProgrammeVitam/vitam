/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.client2;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import fr.gouv.vitam.common.server2.RequestIdHeaderHelper;

/**
 * Manage the X_REQUEST_ID header from the client-side perspective.
 */
public class RequestIdClientFilter implements ClientRequestFilter, ClientResponseFilter {

    /**
     * Retrieves the request id from the VitamSession and add a X-REQUEST-ID header to the request
     *
     * @see ClientRequestFilter#filter(ClientRequestContext)
     * @param requestContext Cf. {@link ClientRequestFilter#filter(ClientRequestContext)}
     * @throws IOException Cf. {@link ClientRequestFilter#filter(ClientRequestContext)}
     */
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        RequestIdHeaderHelper.putRequestIdFromSessionInHeader(requestContext.getHeaders(),
            RequestIdHeaderHelper.Context.REQUEST);
    }

    /**
     * Extracts the X-REQUEST-ID from the headers to save it in the VitamSession
     *
     * @see ClientResponseFilter#filter(ClientRequestContext, ClientResponseContext)
     * @param requestContext Cf. {@link ClientResponseFilter#filter(ClientRequestContext, ClientResponseContext) }
     * @param responseContext Cf. {@link ClientResponseFilter#filter(ClientRequestContext, ClientResponseContext) }
     * @throws IOException
     */
    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        RequestIdHeaderHelper.putRequestIdFromHeaderInSession(responseContext.getHeaders(),
            RequestIdHeaderHelper.Context.RESPONSE);
    }
}

