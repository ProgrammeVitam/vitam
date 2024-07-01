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
package fr.gouv.vitam.access.external.client.v2;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.export.dip.DipRequest;

import javax.ws.rs.core.Response;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

class AccessExternalClientV2Rest extends DefaultClient implements AccessExternalClientV2 {

    AccessExternalClientV2Rest(AccessExternalClientV2Factory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> exportDIP(VitamContext vitamContext, DipRequest dipRequest)
        throws VitamClientException {
        VitamRequestBuilder request = post()
            .withPath(AccessExtAPI.DIP_API)
            .withHeaders(vitamContext.getHeaders())
            .withBody(dipRequest, "DipRequest cannot be null.")
            .withJson();
        try (Response response = make(request)) {
            return RequestResponse.parseFromResponse(check(response), JsonNode.class);
        } catch (IllegalStateException e) {
            throw new VitamClientException(e);
        }
    }

    private Response check(Response response) throws VitamClientException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return response;
        }

        throw new VitamClientException(
            String.format(
                "Error with the response, get status: '%d' and reason '%s'.",
                response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()
            )
        );
    }
}
