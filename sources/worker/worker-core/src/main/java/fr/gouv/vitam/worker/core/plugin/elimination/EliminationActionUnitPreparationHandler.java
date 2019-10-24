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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.entry.EliminationActionUnitReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationActionUnitStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationEventDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import fr.gouv.vitam.worker.core.plugin.elimination.report.EliminationActionReportService;
import fr.gouv.vitam.worker.core.utils.BufferedConsumer;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationUtils.loadRequestJsonFromWorkspace;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Elimination action unit preparation handler.
 */
public class EliminationActionUnitPreparationHandler extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(EliminationActionUnitPreparationHandler.class);

    private static final String ELIMINATION_ACTION_UNIT_PREPARATION = "ELIMINATION_ACTION_UNIT_PREPARATION";
    static final String DATE_REQUEST_IN_FUTURE = "The elimination date cannot be after current date";
    private static final String COULD_NOT_PARSE_DATE_FROM_REQUEST = "Could not not parse date from request";
    private static final String COULD_NOT_PARSE_DSL_REQUEST = "Could not parse DSL request";

    static final String REQUEST_JSON = "request.json";
    static final String UNITS_TO_DELETE_FILE = "units_to_delete.jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private final EliminationAnalysisService eliminationAnalysisService;
    private final EliminationActionReportService eliminationActionReportService;

    /**
     * Default constructor
     */
    public EliminationActionUnitPreparationHandler() {
        this(
            MetaDataClientFactory.getInstance(),
            new EliminationAnalysisService(),
            new EliminationActionReportService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationActionUnitPreparationHandler(
        MetaDataClientFactory metaDataClientFactory,
        EliminationAnalysisService eliminationAnalysisService,
        EliminationActionReportService eliminationActionReportService) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.eliminationAnalysisService = eliminationAnalysisService;
        this.eliminationActionReportService = eliminationActionReportService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try {

            EliminationRequestBody eliminationRequestBody = loadRequestJsonFromWorkspace(handler);

            ItemStatus itemStatus = process(eliminationRequestBody, param, handler);

            LOGGER.info("Elimination action unit preparation succeeded");

            return itemStatus;

        } catch (ProcessingStatusException e) {
            LOGGER.error("Elimination action unit preparation failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(ELIMINATION_ACTION_UNIT_PREPARATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private ItemStatus process(EliminationRequestBody eliminationRequestBody,
        WorkerParameters param, HandlerIO handler)
        throws ProcessingStatusException {

        LocalDate expirationDate = getAndValidateExpirationDate(eliminationRequestBody);

        SelectMultiQuery request = getRequest(eliminationRequestBody.getDslRequest());

        try (MetaDataClient client = metaDataClientFactory.getClient();
            BufferedConsumer<EliminationActionUnitReportEntry> reportAppender =
                createReportAppender(param.getContainerName())) {

            ScrollSpliterator<JsonNode> unitScrollSpliterator =
                ScrollSpliteratorHelper.getUnitWithInheritedRulesScrollSpliterator(request, client);

            Iterator<JsonNode> unitIterator =
                new SpliteratorIterator<>(unitScrollSpliterator);

            int nbDestroyableUnits = 0;
            int nbNonDestroyableUnits = 0;

            File unitsToDelete = handler.getNewLocalFile(UNITS_TO_DELETE_FILE);

            try (FileOutputStream fileOutputStream = new FileOutputStream(unitsToDelete);
                JsonLineWriter unitsToDeleteWriter = new JsonLineWriter(fileOutputStream)) {

                while (unitIterator.hasNext()) {

                    JsonNode unit = unitIterator.next();
                    String unitId = unit.get(VitamFieldsHelper.id()).asText();

                    EliminationAnalysisResult eliminationAnalysisResult = EliminationUtils
                        .computeEliminationAnalysisForUnitWithInheritedRules(unit, eliminationAnalysisService, param,
                            expirationDate);

                    switch (eliminationAnalysisResult.getGlobalStatus()) {

                        case DESTROY:
                            unitsToDeleteWriter
                                .addEntry(new JsonLineModel(unitId, unit.get(VitamFieldsHelper.max()).asInt(), unit));
                            nbDestroyableUnits++;
                            break;

                        case KEEP:
                        case CONFLICT:

                            EliminationActionUnitStatus status;
                            if (eliminationAnalysisResult.getGlobalStatus() == EliminationGlobalStatus.KEEP) {
                                status = EliminationActionUnitStatus.GLOBAL_STATUS_KEEP;
                            } else {
                                status = EliminationActionUnitStatus.GLOBAL_STATUS_CONFLICT;
                            }

                            reportAppender.appendEntry(new EliminationActionUnitReportEntry(
                                unitId,
                                getField(unit, VitamFieldsHelper.originatingAgency()),
                                getField(unit, VitamFieldsHelper.initialOperation()),
                                getField(unit, VitamFieldsHelper.object()),
                                status.name()));

                            nbNonDestroyableUnits++;

                            break;
                        default:
                            throw new IllegalStateException("Unknown elimination global status " +
                                eliminationAnalysisResult.getGlobalStatus());
                    }
                }
            }

            handler.transferFileToWorkspace(UNITS_TO_DELETE_FILE, unitsToDelete, true, false);

            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setExpirationDate(eliminationRequestBody.getDate())
                .setNbDestroyableUnits(nbDestroyableUnits)
                .setNbNonDestroyableUnits(nbNonDestroyableUnits);

            if (nbDestroyableUnits == 0 || nbNonDestroyableUnits > 0) {
                return buildItemStatus(ELIMINATION_ACTION_UNIT_PREPARATION, StatusCode.WARNING, eventDetails);
            } else {
                return buildItemStatus(ELIMINATION_ACTION_UNIT_PREPARATION, StatusCode.OK, eventDetails);
            }

        } catch (IOException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL,
                "Could not generate unit distribution file", e);
        }
    }

    private BufferedConsumer<EliminationActionUnitReportEntry> createReportAppender(String processId) {
        return new BufferedConsumer<>(VitamConfiguration.getBatchSize(), entries -> {
            try {
                eliminationActionReportService.appendUnitEntries(processId, entries);
            } catch (ProcessingStatusException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getField(JsonNode unit, String field) {
        return unit.has(field) ?
            unit.get(field).asText() :
            null;
    }

    private LocalDate getAndValidateExpirationDate(EliminationRequestBody eliminationRequestBody)
        throws ProcessingStatusException {
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(eliminationRequestBody.getDate());
        } catch (DateTimeParseException e) {
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setError(COULD_NOT_PARSE_DATE_FROM_REQUEST);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, COULD_NOT_PARSE_DATE_FROM_REQUEST, e);
        }

        LocalDate today = LocalDate.now();
        if (expirationDate.isAfter(today)) {
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setExpirationDate(eliminationRequestBody.getDate())
                .setError(DATE_REQUEST_IN_FUTURE);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails,
                DATE_REQUEST_IN_FUTURE + " " + expirationDate.toString());
        }

        return expirationDate;
    }

    private SelectMultiQuery getRequest(JsonNode dslRequest) throws ProcessingStatusException {

        try {

            SelectParserMultiple selectParser = new SelectParserMultiple();
            selectParser.parse(dslRequest);
            SelectMultiQuery request = selectParser.getRequest();

            // Order by _max descending (process child units before parents)
            request.resetOrderByFilter();
            request.addOrderByDescFilter(VitamFieldsHelper.max());

            // Update projection
            request.resetUsageProjection();
            request.addUsedProjection(
                VitamFieldsHelper.id(),
                VitamFieldsHelper.object(),
                VitamFieldsHelper.initialOperation(),
                VitamFieldsHelper.originatingAgency(),
                VitamFieldsHelper.max(),
                VitamFieldsHelper.storage());

            return request;

        } catch (InvalidParseOperationException e) {
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setError(COULD_NOT_PARSE_DSL_REQUEST);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, COULD_NOT_PARSE_DSL_REQUEST, e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ACTION_UNIT_PREPARATION;
    }
}
