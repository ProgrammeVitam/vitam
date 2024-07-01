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
package fr.gouv.vitam.common.server.application.session;

import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.client.configuration.ClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamClientInternalException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

public class LocalhostClientFactory extends VitamClientFactory<LocalhostClientFactory.LocalhostClient> {

    public LocalhostClientFactory(final int port, final String rootResourcePath) {
        super(new ClientConfigurationImpl("localhost", port), rootResourcePath);
    }

    @Override
    public LocalhostClient getClient() {
        switch (getVitamClientType()) {
            case MOCK:
                throw new UnsupportedOperationException(
                    "No mock for this class is implemented (this class is for REST testing purpose only)."
                );
            case PRODUCTION:
                return new LocalhostClient(this);
            default:
                throw new IllegalArgumentException("Client type unknown.");
        }
    }

    public static class LocalhostClient extends DefaultClient {

        public LocalhostClient(VitamClientFactoryInterface<?> factory) {
            super(factory);
        }

        public String doRequest(final String subResource) {
            VitamRequestBuilder request = get().withPath(subResource).withAccept(MediaType.TEXT_PLAIN_TYPE);
            try (Response response = make(request)) {
                check(response);
                return response.readEntity(String.class);
            } catch (VitamClientInternalException e) {
                throw new IllegalStateException(e);
            }
        }

        public String doRequest(final String subResource, final MultivaluedHashMap<String, Object> headers) {
            VitamRequestBuilder request = get()
                .withPath(subResource)
                .withHeaders(headers)
                .withAccept(MediaType.TEXT_PLAIN_TYPE);
            try (Response response = make(request)) {
                check(response);
                return response.readEntity(String.class);
            } catch (VitamClientInternalException e) {
                throw new IllegalStateException(e);
            }
        }

        private void check(Response response) throws VitamClientInternalException {
            Response.Status status = response.getStatusInfo().toEnum();
            if (SUCCESSFUL.equals(status.getFamily())) {
                return;
            }
            throw new VitamClientInternalException(
                String.format(
                    "Error with the response, get status: '%d' and reason '%s'.",
                    response.getStatus(),
                    fromStatusCode(response.getStatus()).getReasonPhrase()
                )
            );
        }
    }
}
