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

/**
 * update SP and SPS on Object group
 */
public class OriginatingAgencyReassignmentMainUnitsObjectGroupsUpdatePlugin extends StoreMetadataObjectActionHandler {

    public static final String REASSIGNMENT_MAIN_UNITS_OBJECT_GROUPS_UPDATE_PLUGIN_NAME =
        "ORIGINATING_AGENCY_REASSIGNMENT_UPDATE_OBJECT_GROUPS";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentMainUnitsObjectGroupsUpdatePlugin.class
    );

    private final OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    public OriginatingAgencyReassignmentMainUnitsObjectGroupsUpdatePlugin() {
        // Default constructor for workflow initialization by Worker
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        throw new IllegalStateException("UnsupportedOperation");
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        LOGGER.info("starting update sp and sps for object group by replacing SP ");

        List<String> objectGroupsIds = workerParameters.getObjectNameList();

        try (MetaDataClient mdClient = handler.getMetaDataClient()) {
            final OriginatingAgencyReassignmentRequest reassignmentRequest =
                originatingAgencyReassignmentService.loadRequestJsonFromWorkspace(handler);

            List<JsonNode> objectGroups = originatingAgencyReassignmentService.getObjectGroupsByIds(
                handler,
                objectGroupsIds
            );

            List<JsonNode> updateQueries =
                originatingAgencyReassignmentService.buildObjectGroupsOriginatingAgencyReassignmentUpdateQueries(
                    handler,
                    objectGroups,
                    reassignmentRequest.getSourceOriginatingAgency(),
                    reassignmentRequest.getTargetOriginatingAgency(),
                    handler.getContainerName()
                );

            RequestResponse<JsonNode> requestResponse = mdClient.objectGroupsAtomicUpdateBulk(updateQueries);

            return originatingAgencyReassignmentService.buildUpdateItemsStatusResponse(requestResponse, getPluginId());
        } catch (
            MetaDataExecutionException
            | MetaDataNotFoundException
            | MetaDataClientServerException
            | InvalidParseOperationException
            | InvalidCreateOperationException
            | MetaDataDocumentSizeException e
        ) {
            throw new ProcessingException(e.getMessage(), e);
        }
    }

    public static String getPluginId() {
        return REASSIGNMENT_MAIN_UNITS_OBJECT_GROUPS_UPDATE_PLUGIN_NAME;
    }
}
