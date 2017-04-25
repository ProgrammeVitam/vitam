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
package fr.gouv.vitam.access.external.client;

import java.io.InputStream;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientNotFoundException;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientServerException;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.model.RequestResponse;

/**
 * Admin External Client Interface
 */
public interface AdminExternalClient extends BasicClient {

    /**
     * checkDocuments
     *
     * @param documentType
     * @param stream
     * @param tenantId
     * @return the status
     * @throws AccessExternalClientNotFoundException
     * @throws AccessExternalClientException
     * @throws AccessExternalClientServerException
     */
    Status checkDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws AccessExternalClientNotFoundException,
        AccessExternalClientException, AccessExternalClientServerException;


    /**
     * importDocuments
     *
     * @param documentType
     * @param stream
     * @param tenantId
     * @return the status
     * @throws AccessExternalClientNotFoundException
     * @throws AccessExternalClientException
     */
    Status createDocuments(AdminCollections documentType, InputStream stream, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException;

    /**
     * findDocuments
     *
     * @param documentType
     * @param select
     * @param tenantId
     * @return the JsonNode results
     * @throws AccessExternalClientNotFoundException
     * @throws AccessExternalClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse findDocuments(AdminCollections documentType, JsonNode select, Integer tenantId)
        throws AccessExternalClientNotFoundException, AccessExternalClientException, InvalidParseOperationException;

    /**
     * findDocumentById
     *
     * @param documentType
     * @param documentId
     * @param tenantId
     * @return the JsonNode results
     * @throws AccessExternalClientException
     * @throws InvalidParseOperationException
     */
    RequestResponse findDocumentById(AdminCollections documentType, String documentId, Integer tenantId)
        throws AccessExternalClientException, InvalidParseOperationException;


    /**
     * Import a set of contracts after passing the validation steps. If all the contracts are valid, they are stored in
     * the collection and indexed. </BR> The input is invalid in the following situations : </BR>
     * <ul>
     * <li>The json is invalid</li>
     * <li>The json contains 2 ore many contracts having the same name</li>
     * <li>One or more mandatory field is missing</li>
     * <li>A field has an invalid format</li>
     * <li>One or many contracts elready exist in the database</li>
     * </ul>
     * 
     * @param contracts as InputStream
     * @param tenantId
     * @param collection the collection name
     * @return Vitam response
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse importContracts(InputStream contracts, Integer tenantId, AdminCollections collection)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Update the given access contract by query dsl
     * 
     * @param queryDsl the given dsl query
     * @param tenantId
     * @return Response status ok or vitam error
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse updateAccessContract(JsonNode queryDsl, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException;

    /**
     * Update the given ingest contract by query dsl
     * 
     * @param queryDsl the given dsl query
     * @param tenantId
     * @return Response status ok or vitam error
     * @throws InvalidParseOperationException
     * @throws AccessExternalClientException
     */
    RequestResponse updateIngestContract(JsonNode queryDsl, Integer tenantId)
        throws InvalidParseOperationException, AccessExternalClientException;

}
