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
package fr.gouv.vitam.common.format.identification.siegfried;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.Response;
import java.nio.file.Path;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.format.identification.siegfried.SiegfriedQueryParams.BASE64;
import static fr.gouv.vitam.common.format.identification.siegfried.SiegfriedQueryParams.FORMAT;
import static fr.gouv.vitam.common.format.identification.siegfried.SiegfriedQueryParams.SCAN_ENTRIES_WITHIN_ZIP;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

public class SiegfriedClientRest extends DefaultClient implements SiegfriedClient {

    SiegfriedClientRest(SiegfriedClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> analysePath(Path filePath)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        String encodedFilePath = BaseXx.getBase64UrlWithPadding(filePath.toString().getBytes());
        VitamRequestBuilder request = get()
            .withPath("/" + encodedFilePath)
            .withQueryParam(FORMAT.getParameter(), FORMAT.getValue())
            .withQueryParam(BASE64.getParameter(), BASE64.getValue())
            .withQueryParam(SCAN_ENTRIES_WITHIN_ZIP.getParameter(), SCAN_ENTRIES_WITHIN_ZIP.getValue())
            .withJsonAccept();
        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK().addResult(response.readEntity(JsonNode.class));
        } catch (VitamClientInternalException e) {
            throw new FormatIdentifierTechnicalException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> status(Path filePath)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        return analysePath(filePath);
    }

    private void check(Response response) throws FormatIdentifierNotFoundException, FormatIdentifierTechnicalException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }
        String message = String.format(
            "Error with the response, get status: '%d' and reason '%s'.",
            response.getStatus(),
            fromStatusCode(response.getStatus()).getReasonPhrase()
        );
        if (NOT_FOUND.equals(status)) {
            throw new FormatIdentifierNotFoundException(message);
        }
        throw new FormatIdentifierTechnicalException(message);
    }
}
