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
package fr.gouv.vitam.functional.administration.core.context;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.core.security.profile.SecurityProfileService;

import java.util.List;

/**
 * ContextService Interface declaring methods associated to contexts
 */
public interface ContextService extends VitamAutoCloseable {
    /**
     * Create a list of contexts
     *
     * @param contextModelList the context list to be created
     * @return a response as a RequestResponse<ContextModel> Object
     * @throws VitamException thrown if operation could not be done
     */
    RequestResponse<ContextModel> createContexts(List<ContextModel> contextModelList) throws VitamException;

    /**
     * Search for contexts
     *
     * @param queryDsl the query to be used for the search
     * @return the list of contexts as a DbRequestResult
     * @throws ReferentialException thrown if the query could not be executed
     * @throws InvalidParseOperationException thrown if query is incorrect
     */
    DbRequestResult findContexts(JsonNode queryDsl) throws ReferentialException, InvalidParseOperationException;

    /**
     * Update a context
     *
     * @param id the id of the context
     * @param queryDsl the update command as a query
     * @return a response as a RequestResponse<ContextModel> object
     * @throws VitamException thrown if operation could not be done
     */
    RequestResponse<ContextModel> updateContext(String id, JsonNode queryDsl) throws VitamException;

    /**
     * Find a context by its id
     *
     * @param id the id of the context
     * @return the context as a ContextModel
     * @throws ReferentialException thrown if the context could not be found
     * @throws InvalidParseOperationException thrown if the query is incorrect
     */
    ContextModel findOneContextById(String id) throws ReferentialException, InvalidParseOperationException;

    /**
     * Delete a context
     *
     * @param contextId the id of the context
     * @param forceDelete
     * @return a response as a RequestResponse<ContextModel> object
     * @throws VitamException thrown if operation could not be done
     */
    RequestResponse<ContextModel> deleteContext(String contextId, boolean forceDelete) throws VitamException;

    boolean securityProfileIsUsedInContexts(String securityProfileId)
        throws InvalidCreateOperationException, ReferentialException, InvalidParseOperationException;

    void setSecurityProfileService(SecurityProfileService securityProfileService);
}
