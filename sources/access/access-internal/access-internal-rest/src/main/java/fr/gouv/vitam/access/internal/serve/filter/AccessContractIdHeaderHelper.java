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
package fr.gouv.vitam.access.internal.serve.filter;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.access.internal.serve.exception.MissingAccessContractIdException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;

import javax.ws.rs.core.MultivaluedMap;
import java.util.List;

/**
 * Helper class to manage the X_ACCESS_CONTRAT_ID and VitamSession links
 */
public class AccessContractIdHeaderHelper {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private AccessContractIdHeaderHelper() {
        throw new UnsupportedOperationException("Helper class");
    }

    /**
     * Extracts the X_ACCESS_CONTRACT_ID from the headers to save it through the VitamSession
     *
     * @param requestHeaders Complete list of HTTP message headers ; will not be changed.
     * @throws MissingAccessContractIdException
     */
    public static void manageAccessContractFromHeader(
        MultivaluedMap<String, String> requestHeaders,
        AdminManagementClientFactory adminManagementClientFactory
    ) throws MissingAccessContractIdException {
        try (final AdminManagementClient client = adminManagementClientFactory.getClient()) {
            String headerAccessContractId = requestHeaders.getFirst(GlobalDataRest.X_ACCESS_CONTRAT_ID);

            if (headerAccessContractId == null) {
                throw new MissingAccessContractIdException(
                    "Missing access contract header " + GlobalDataRest.X_ACCESS_CONTRAT_ID
                );
            }

            JsonNode queryDsl = getQueryDsl(headerAccessContractId);
            RequestResponse<AccessContractModel> response = client.findAccessContracts(queryDsl);

            if (!response.isOk()) {
                VitamError vitamError = (VitamError) response;
                throw new MissingAccessContractIdException(
                    vitamError.getMessage() + " : " + vitamError.getDescription()
                );
            }

            if (((RequestResponseOK<AccessContractModel>) response).getResults().size() == 0) {
                throw new MissingAccessContractIdException(headerAccessContractId + " not found in the system");
            }

            List<AccessContractModel> contracts = ((RequestResponseOK<AccessContractModel>) response).getResults();
            VitamThreadUtils.getVitamSession().setContract(contracts.get(0));
        } catch (
            final VitamThreadAccessException
            | AdminManagementClientServerException
            | InvalidParseOperationException
            | InvalidCreateOperationException e
        ) {
            throw new MissingAccessContractIdException(
                "Got an exception while trying to check the access contract in the current session ; exception was : {}",
                e
            );
        }
    }

    private static JsonNode getQueryDsl(String headerAccessContractId) throws InvalidCreateOperationException {
        Select select = new Select();
        Query query = QueryHelper.and()
            .add(
                QueryHelper.eq(AccessContract.IDENTIFIER, headerAccessContractId),
                QueryHelper.eq(AccessContract.STATUS, ACTIVE_STATUS)
            );
        select.setQuery(query);
        JsonNode queryDsl = select.getFinalSelect();

        return queryDsl;
    }
}
