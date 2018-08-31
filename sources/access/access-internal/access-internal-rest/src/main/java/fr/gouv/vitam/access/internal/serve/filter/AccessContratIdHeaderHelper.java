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
package fr.gouv.vitam.access.internal.serve.filter;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.internal.serve.exception.MissingAccessContractIdException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;

/**
 * Helper class to manage the X_ACCESS_CONTRAT_ID and VitamSession links
 */
public class AccessContratIdHeaderHelper {
    
    private static final String ACTIVE_STATUS = "ACTIVE";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessContratIdHeaderHelper.class);

    /**
     * Helper class, so not instanciable.
     */
    private AccessContratIdHeaderHelper() {
        throw new UnsupportedOperationException("Helper class");
    }

    /**
     * Extracts the X_ACCESS_CONTRAT_ID from the headers to save it through the VitamSession
     *
     * @param requestHeaders Complete list of HTTP message headers ; will not be changed.
     * @throws MissingAccessContractIdException
     */
    public static void manageAccessContratFromHeader(MultivaluedMap<String, String> requestHeaders) throws
        MissingAccessContractIdException {
        try(final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            String headerAccessContratId = requestHeaders.getFirst(GlobalDataRest.X_ACCESS_CONTRAT_ID);

            if (headerAccessContratId== null){
                throw new MissingAccessContractIdException("Missing access contract header " + GlobalDataRest.X_ACCESS_CONTRAT_ID);
            }

            JsonNode queryDsl = getQueryDsl(headerAccessContratId);            
            RequestResponse<AccessContractModel> response = client.findAccessContracts(queryDsl);

            if (!response.isOk() || ((RequestResponseOK<AccessContractModel>)response).getResults().size() == 0){
                throw new MissingAccessContractIdException(headerAccessContratId);
            }

            List<AccessContractModel> contracts = ((RequestResponseOK<AccessContractModel>)response).getResults(); 
            VitamThreadUtils.getVitamSession().setContract(contracts.get(0));
        } catch (final VitamThreadAccessException | AdminManagementClientServerException |
            InvalidParseOperationException | InvalidCreateOperationException e) {
            LOGGER.warn(
                "Got an exception while trying to check the access contrat in the current session ; exception was : {}",
                requestHeaders, e);
        }
    }
    
    private static JsonNode getQueryDsl(String headerAccessContratId) 
        throws InvalidParseOperationException, InvalidCreateOperationException{

        Select select = new Select();        
        Query query = QueryHelper.and().add(QueryHelper.eq(AccessContract.IDENTIFIER, headerAccessContratId),
            QueryHelper.eq(AccessContract.STATUS, ACTIVE_STATUS));
        select.setQuery(query);        
        JsonNode queryDsl = select.getFinalSelect();
        
        return queryDsl;
    }
}
