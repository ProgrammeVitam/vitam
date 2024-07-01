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
package fr.gouv.vitam.client;

import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static fr.gouv.vitam.common.GlobalDataRest.X_TENANT_ID;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.delete;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class IhmRecetteClient extends DefaultClient {

    private static final List<String> COLLECTION_TO_EMPTY = Arrays.asList(
        "delete/logbook/operation",
        "delete/masterdata/accessContract",
        "delete/masterdata/ingestContract",
        "delete/logbook/lifecycle/unit",
        "delete/logbook/lifecycle/objectgroup",
        "delete/metadata/objectgroup",
        "delete/metadata/unit",
        "delete/accessionregisters"
    );

    public IhmRecetteClient(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    IhmRecetteClient(IhmRecetteClientFactory factory) {
        super(factory);
    }

    public void deleteTnrCollectionsTenant(String tenantId) throws VitamException {
        VitamRequestBuilder request = delete().withHeader(X_TENANT_ID, tenantId).withJsonAccept();

        for (String url : COLLECTION_TO_EMPTY) {
            try (Response response = make(request.withPath(url))) {
                check(response);
            }
        }
    }

    private void check(Response response) throws VitamClientInternalException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }
        String message = String.format(
            "Error with the response, get status: '%d' and reason '%s'.",
            response.getStatus(),
            status.getReasonPhrase()
        );
        throw new VitamClientInternalException(message);
    }
}
