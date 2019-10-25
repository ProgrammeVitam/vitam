/*
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

package fr.gouv.vitam.client;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * Ihm recette http client
 */
public class IhmRecetteClient extends DefaultClient {
    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public IhmRecetteClient(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }


    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IhmRecetteClient.class);

    private static final String[] COLLECTION_TO_EMPTY = {
        "delete/logbook/operation",
        "delete/masterdata/accessContract",
        "delete/masterdata/ingestContract",
        "delete/logbook/lifecycle/unit",
        "delete/logbook/lifecycle/objectgroup",
        "delete/metadata/objectgroup",
        "delete/metadata/unit",
        "delete/accessionregisters"
    };

    IhmRecetteClient(IhmRecetteClientFactory factory) {
        super(factory);
    }

    /**
     * Use only with Testing
     * delete data on tenant tenantId
     *
     * @param tenantId
     * @throws VitamException
     */
    public void deleteTnrCollectionsTenant(String tenantId) throws VitamException {

        ParametersChecker.checkParameter("check tenant Parameter", tenantId);
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        List<String> list = Arrays.asList(COLLECTION_TO_EMPTY);
        list.stream().forEach((l) -> {
            try {
                delete(headers, l);
            } catch (VitamException e) {
                LOGGER.debug("Error when deleting collections");
            }
        });

    }

    private void delete(MultivaluedHashMap<String, Object> headers, String url) throws VitamException {
        Response response = null;
        try {
            response = performRequest(HttpMethod.DELETE, url, headers, MediaType.APPLICATION_JSON_TYPE);
            if (response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode()) {
                LOGGER.error("Deleting Error  : " + Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
                throw new VitamClientInternalException("Error when delleting collections");
            }
        } catch (Exception e) {
            LOGGER.debug("Error when deleting collections");
            throw new VitamException(e.getMessage());
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }
}
