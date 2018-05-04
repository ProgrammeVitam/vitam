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
package fr.gouv.vitam.functional.administration.ontologies.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.OntologyModel;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * AdminManagementOntologiesClientRest
 */
class AdminManagementOntologiesClientRest extends DefaultClient implements AdminManagementOntologiesClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementOntologiesClientRest.class);
    
    private static final String ONTOLOGY_CACHE_URI = "/ontologies/cache";
    
    AdminManagementOntologiesClientRest(AdminManagementOntologiesClientFactory factory) {
        super(factory);
    }

    @Override 
    public RequestResponse<OntologyModel> findOntologiesForCache(JsonNode query) throws InvalidParseOperationException, VitamClientException {
        ParametersChecker.checkParameter("The input queryDsl json is mandatory", query);
        Response response = null;
        try {
            response = performRequest(HttpMethod.GET, ONTOLOGY_CACHE_URI, null, query,
                MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);
            final Status status = Status.fromStatusCode(response.getStatus());
            if (status == Status.OK) {
                LOGGER.debug(Response.Status.OK.getReasonPhrase());
                return JsonHandler.getFromString(response.readEntity(String.class), RequestResponseOK.class,
                    OntologyModel.class);
            }

            return RequestResponse.parseFromResponse(response, OntologyModel.class);

        } catch (VitamClientInternalException e) {
            LOGGER.error("Internal Server Error", e);
            throw new VitamClientException("Internal Server Error", e);
        } catch (Exception e) {
            // TODO : remove (just for test)
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            throw e;
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
    
}
