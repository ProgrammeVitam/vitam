/**
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
 */

package fr.gouv.vitam.common.format.identification.siegfried;

import java.nio.file.Path;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * HTTP implementation of siegfried client.
 */
public class SiegfriedClientRest extends DefaultClient implements SiegfriedClient {
    private static final String INTERNAL_ERROR_MSG = "Internal server error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SiegfriedClientRest.class);

    /**
     * Create a new Siegfried HTTP Client
     *
     * @param factory
     */
    SiegfriedClientRest(SiegfriedClientFactory factory) {
        super(factory);
    }

    @Override
    public JsonNode analysePath(Path filePath)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        LOGGER.debug("Path to analyze: " + filePath);
        final Response siegfriedResponse = callSiegfried(filePath);
        return handleCommonResponseIdentify(siegfriedResponse);
    }

    @Override
    public JsonNode status(Path filePath)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {

        final Response siegfriedResponse = callSiegfried(filePath);
        return handleCommonResponseStatus(siegfriedResponse);
    }

    private Response callSiegfried(Path filePath) throws FormatIdentifierTechnicalException {
        LOGGER.debug("Call siegfried server");
        final String encodedFilePath = BaseXx.getBase64Padding(filePath.toString().getBytes());
        Response response = null;
        try {
            response =
                performRequest(HttpMethod.GET, "/" + encodedFilePath, null, MediaType.APPLICATION_JSON_TYPE, false);
        } catch (final Exception e) {
            LOGGER.error("While call Siegfried HTTP Client", e);
            consumeAnyEntityAndClose(response);
            throw new FormatIdentifierTechnicalException(e);
        }

        return response;
    }

    private JsonNode handleCommonResponseIdentify(Response response)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        try {
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return response.readEntity(JsonNode.class);
                case NOT_FOUND:
                    throw new FormatIdentifierNotFoundException(status.getReasonPhrase());
                default:
                    LOGGER.error(INTERNAL_ERROR_MSG + status.getReasonPhrase());
                    throw new FormatIdentifierTechnicalException(INTERNAL_ERROR_MSG);
            }
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    private JsonNode handleCommonResponseStatus(Response response)
        throws FormatIdentifierTechnicalException, FormatIdentifierNotFoundException {
        try {
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    return response.readEntity(JsonNode.class);
                case NOT_FOUND:
                    throw new FormatIdentifierNotFoundException(status.getReasonPhrase());
                default:
                    LOGGER.error(INTERNAL_ERROR_MSG + status.getReasonPhrase());
                    throw new FormatIdentifierTechnicalException(INTERNAL_ERROR_MSG);
            }
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
