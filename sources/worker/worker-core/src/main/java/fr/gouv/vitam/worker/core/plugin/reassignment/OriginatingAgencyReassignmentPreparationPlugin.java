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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.OriginatingAgencyReassignmentRequest;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AgenciesModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * OriginatingAgencyReassignmentPreparationPlugin
 */
public class OriginatingAgencyReassignmentPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        OriginatingAgencyReassignmentPreparationPlugin.class
    );

    private static final String PLUGIN_NAME = "ORIGINATING_AGENCY_REASSIGNMENT_PREPARATION";

    private static final String UNITS_TO_UPDATE_FILE = "units_to_update.jsonl";

    private final OriginatingAgencyReassignmentService originatingAgencyReassignmentService;

    public OriginatingAgencyReassignmentPreparationPlugin() {
        // Default constructor for workflow initialization by Worker
        originatingAgencyReassignmentService = new OriginatingAgencyReassignmentService();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            final OriginatingAgencyReassignmentRequest reassignmentRequest =
                originatingAgencyReassignmentService.loadRequestJsonFromWorkspace(handler);
            if (
                Objects.equals(
                    reassignmentRequest.getSourceOriginatingAgency(),
                    reassignmentRequest.getTargetOriginatingAgency()
                )
            ) {
                throw new ProcessingStatusException(
                    StatusCode.KO,
                    "sourceOriginatingAgency should be different to targetOriginatingAgency"
                );
            }
            checkTargetOriginatingAgency(handler, reassignmentRequest.getTargetOriginatingAgency());
            checkUnitsAndGenerateDistributions(handler, reassignmentRequest);

            return buildItemStatus(PLUGIN_NAME, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error(
                "originating agencies reassignment preparation failed with status [" + e.getMessage() + "]",
                e
            );
            return buildItemStatus(PLUGIN_NAME, e.getStatusCode(), e);
        }
    }

    private void checkTargetOriginatingAgency(HandlerIO handler, String targetOriginatingAgency)
        throws ProcessingStatusException, ProcessingException {
        LOGGER.debug("Check originating agency check agency with id {}", targetOriginatingAgency);
        try (AdminManagementClient adminManagementClient = handler.getAdminManagementClient()) {
            final RequestResponse<AgenciesModel> agencyByIdResponse = adminManagementClient.getAgencyById(
                targetOriginatingAgency
            );

            List<AgenciesModel> agenciesModels = ((RequestResponseOK<AgenciesModel>) agencyByIdResponse).getResults();
            if (agenciesModels.isEmpty()) {
                throw new ReferentialNotFoundException("originating agency check failed, not found");
            }
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            throw new ProcessingException(e.getMessage(), e);
        } catch (ReferentialNotFoundException e) {
            ObjectNode evDetData = JsonHandler.createObjectNode();
            String message = "originating agency check failed, not found";
            evDetData.put("error", message);
            throw new ProcessingStatusException(StatusCode.KO, evDetData, "originating agency check failed, not found");
        }
    }

    private void checkUnitsAndGenerateDistributions(
        HandlerIO handler,
        OriginatingAgencyReassignmentRequest reassignmentRequest
    ) throws ProcessingStatusException {
        File unitToUpdateDistributionFile = handler.getNewLocalFile(
            handler.getWorkFlowExecutionContext(),
            UNITS_TO_UPDATE_FILE
        );
        try (MetaDataClient metadataClient = handler.getMetaDataClient()) {
            SelectMultiQuery selectMultiQuery = createSelectMultiple(reassignmentRequest.getDslRequest());
            ScrollSpliterator<JsonNode> unitScrollSpliterator = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                metadataClient,
                selectMultiQuery
            );

            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(unitScrollSpliterator);

            try (
                FileOutputStream fileOutputStream = new FileOutputStream(unitToUpdateDistributionFile);
                JsonLineWriter<JsonLineModel> unitsToUpdateWriter = new JsonLineWriter<>(fileOutputStream)
            ) {
                while (unitIterator.hasNext()) {
                    JsonNode unit = unitIterator.next();
                    validateAndAddUnitToDistributionFile(
                        reassignmentRequest.getSourceOriginatingAgency(),
                        reassignmentRequest.getTargetOriginatingAgency(),
                        unit,
                        unitsToUpdateWriter
                    );
                }
            }

            handler.transferFileToWorkspace(UNITS_TO_UPDATE_FILE, unitToUpdateDistributionFile, true, false);
        } catch (IOException | InvalidParseOperationException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not generate unit distributions", e);
        }
    }

    private void validateAndAddUnitToDistributionFile(
        String sourceOriginatingAgency,
        String targetOriginatingAgency,
        JsonNode unit,
        JsonLineWriter<JsonLineModel> unitsToUpdateWriter
    ) throws IOException, ProcessingStatusException {
        String unitId = unit.get(VitamFieldsHelper.id()).asText();

        if (!unit.has(VitamFieldsHelper.originatingAgency())) {
            throw new ProcessingStatusException(StatusCode.KO, "This action is not allowed for Holding units ");
        }
        String currentOriginatingAgency = unit.get(VitamFieldsHelper.originatingAgency()).asText();

        if (
            !Objects.equals(currentOriginatingAgency, sourceOriginatingAgency) &&
            !Objects.equals(currentOriginatingAgency, targetOriginatingAgency)
        ) {
            throw new ProcessingStatusException(
                StatusCode.KO,
                "OriginatingAgency does not match source neither target Originating Agency"
            );
        }
        if (!Objects.equals(currentOriginatingAgency, targetOriginatingAgency)) {
            unitsToUpdateWriter.addEntry(new JsonLineModel(unitId, unit.get(VitamFieldsHelper.max()).asInt(), unit));
        }
    }

    private SelectMultiQuery createSelectMultiple(JsonNode initialQuery) throws InvalidParseOperationException {
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(initialQuery);
        SelectMultiQuery selectMultiQuery = parser.getRequest();

        selectMultiQuery.resetUsedProjection();
        selectMultiQuery.addUsedProjection(
            VitamFieldsHelper.id(),
            VitamFieldsHelper.max(),
            VitamFieldsHelper.originatingAgency(),
            VitamFieldsHelper.originatingAgencies(),
            VitamFieldsHelper.unitups(),
            VitamFieldsHelper.allunitups(),
            VitamFieldsHelper.validComputedInheritedRules()
        );
        selectMultiQuery.addOrderByAscFilter(VitamFieldsHelper.max());
        return selectMultiQuery;
    }

    public static String getId() {
        return PLUGIN_NAME;
    }
}
