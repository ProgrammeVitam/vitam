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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

/**
 *
 */
public abstract class StoreMetadataObjectActionHandler extends StoreObjectActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StoreMetadataObjectActionHandler.class);

    private static final String $RESULTS = "$results";
    private static final String DOCUMENT_NOT_FOUND = "Document not found";

    public StoreMetadataObjectActionHandler(StorageClientFactory storageClientFactory) {
        super(storageClientFactory);
    }

    /**
     * selectMetadataDocumentById, Retrieve Metadata Document from DB
     *
     * @param idDocument document uuid
     * @param dataCategory accepts UNIT or OBJECTGROUP
     * @param metaDataClient MetaDataClient to use
     * @return JsonNode from the found document
     * @throws ProcessingException if no result found or error during parsing response from metadata client
     */
    protected JsonNode selectMetadataDocumentRawById(
        String idDocument,
        DataCategory dataCategory,
        MetaDataClient metaDataClient
    ) throws VitamException {
        ParametersChecker.checkParameter("Data category ", dataCategory);
        ParametersChecker.checkParameter("idDocument is empty", idDocument);

        RequestResponse<JsonNode> requestResponse;
        JsonNode jsonResponse;
        try {
            switch (dataCategory) {
                case UNIT:
                    requestResponse = metaDataClient.getUnitByIdRaw(idDocument);
                    break;
                case OBJECTGROUP:
                    requestResponse = metaDataClient.getObjectGroupByIdRaw(idDocument);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported category " + dataCategory);
            }
            if (requestResponse.isOk()) {
                jsonResponse = requestResponse.toJsonNode();
            } else {
                throw new ProcessingException("Document not found");
            }
        } catch (VitamClientException e) {
            LOGGER.error(e);
            throw e;
        }

        return extractNodeFromResponse(jsonResponse, DOCUMENT_NOT_FOUND);
    }

    /**
     * retrieve the Raw LFC for the metadata document (Unit or Got)
     *
     * @param idDocument document uuid
     * @param dataCategory accepts UNIT or OBJECT_GROUP
     * @param logbookClient LogbookLifeCyclesClient to use
     * @return the raw LFC
     * @throws ProcessingException if no result found or error during parsing response from logbook client
     */
    protected JsonNode getRawLogbookLifeCycleById(
        String idDocument,
        DataCategory dataCategory,
        LogbookLifeCyclesClient logbookClient
    ) throws VitamException {
        switch (dataCategory) {
            case UNIT:
                return logbookClient.getRawUnitLifeCycleById(idDocument);
            case OBJECTGROUP:
                return logbookClient.getRawObjectGroupLifeCycleById(idDocument);
            default:
                throw new IllegalArgumentException("Unsupported category " + dataCategory);
        }
    }

    /**
     * extractNodeFromResponse, check response and extract single result
     *
     * @param jsonResponse
     * @param error message to throw if response is null or no result could be found
     * @return a single result from response
     * @throws ProcessingException if no result found
     */
    private JsonNode extractNodeFromResponse(JsonNode jsonResponse, final String error) throws VitamException {
        JsonNode jsonNode;
        // check response
        if (jsonResponse == null) {
            LOGGER.error(error);
            throw new ProcessingException(error);
        }
        jsonNode = jsonResponse.get($RESULTS);
        // if result = 0 then throw Exception
        if (jsonNode == null || jsonNode.size() == 0) {
            LOGGER.error(error);
            throw new VitamException(error);
        }

        // return a single node
        return jsonNode.get(0);
    }
}
