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
package fr.gouv.vitam.worker.core.plugin.reassignment;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.StoreMetadataObjectActionHandler;

import java.util.List;

public class OriginatingAgencyReassignmentUnitsChildrenAgenciesComputePlugin extends StoreMetadataObjectActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentUnitsChildrenAgenciesComputePlugin.class
    );
    private static final String PLUGIN_NAME = "ORIGINATING_AGENCY_REASSIGNMENT_UPDATE_CHILDREN_UNITS";
    private final OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    public OriginatingAgencyReassignmentUnitsChildrenAgenciesComputePlugin() {
        // Default constructor for workflow initialization by Worker
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        LOGGER.debug(String.format("executeList from plugin '%s'", PLUGIN_NAME));

        List<String> unitsIds = workerParameters.getObjectNameList();
        try (MetaDataClient mdClient = handler.getMetaDataClient()) {
            final OriginatingAgencyReassignmentRequest reassignmentRequest =
                originatingAgencyReassignmentService.loadRequestJsonFromWorkspace(handler);
            List<JsonNode> units = originatingAgencyReassignmentService.findUnitsByIds(handler, unitsIds);

            List<JsonNode> updateQueries =
                originatingAgencyReassignmentService.buildUnitsOriginatingAgenciesComputingUpdateQueries(
                    handler,
                    units,
                    reassignmentRequest.getSourceOriginatingAgency(),
                    reassignmentRequest.getTargetOriginatingAgency(),
                    workerParameters.getContainerName()
                );

            RequestResponse<JsonNode> requestResponse = mdClient.atomicUpdateBulk(updateQueries);

            return originatingAgencyReassignmentService.buildUpdateItemsStatusResponse(requestResponse, getPluginId());
        } catch (
            InvalidCreateOperationException
            | MetaDataExecutionException
            | InvalidParseOperationException
            | MetaDataNotFoundException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            throw new ProcessingException(
                "originating agencies update reassignment failed with status [" + StatusCode.FATAL + "]",
                e
            );
        }
    }

    public static String getPluginId() {
        return PLUGIN_NAME;
    }
}
