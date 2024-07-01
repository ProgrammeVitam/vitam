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
package fr.gouv.vitam.worker.core.plugin.elimination;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.json.JsonHandler;
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
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationEventDetails;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationGlobalStatus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;

import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationUtils.loadRequestJsonFromWorkspace;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Elimination analysis preparation handler.
 */
public class EliminationAnalysisPreparationHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        EliminationAnalysisPreparationHandler.class
    );

    private static final String ELIMINATION_ANALYSIS_PREPARATION = "ELIMINATION_ANALYSIS_PREPARATION";
    private static final String COULD_NOT_PARSE_DATE_FROM_REQUEST = "Could not not parse date from request";
    private static final String COULD_NOT_PARSE_DSL_REQUEST = "Could not parse DSL request";

    private static final String UNITS_JSONL_FILE = "units.jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private final EliminationAnalysisService eliminationAnalysisService;

    /**
     * Default constructor
     */
    public EliminationAnalysisPreparationHandler() {
        this(MetaDataClientFactory.getInstance(), new EliminationAnalysisService());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationAnalysisPreparationHandler(
        MetaDataClientFactory metaDataClientFactory,
        EliminationAnalysisService eliminationAnalysisService
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.eliminationAnalysisService = eliminationAnalysisService;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            EliminationRequestBody eliminationRequestBody = loadRequestJsonFromWorkspace(handler);

            process(eliminationRequestBody, param, handler);

            LOGGER.info("Elimination analysis preparation succeeded");
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setExpirationDate(eliminationRequestBody.getDate());
            return buildItemStatus(ELIMINATION_ANALYSIS_PREPARATION, StatusCode.OK, eventDetails);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Elimination analysis preparation failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(ELIMINATION_ANALYSIS_PREPARATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void process(EliminationRequestBody eliminationRequestBody, WorkerParameters param, HandlerIO handler)
        throws ProcessingStatusException {
        LocalDate expirationDate = getExpirationDate(eliminationRequestBody);
        SelectMultiQuery request = getRequest(eliminationRequestBody.getDslRequest());

        File unitDistributionFile = null;

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            ScrollSpliterator<JsonNode> unitScrollSpliterator =
                ScrollSpliteratorHelper.getUnitWithInheritedRulesScrollSpliterator(request, client);

            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(unitScrollSpliterator);

            unitDistributionFile = handler.getNewLocalFile(UNITS_JSONL_FILE);

            try (JsonLineWriter unitWriter = new JsonLineWriter(new FileOutputStream(unitDistributionFile))) {
                while (unitIterator.hasNext()) {
                    JsonNode unit = unitIterator.next();
                    String unitId = unit.get(VitamFieldsHelper.id()).asText();

                    EliminationAnalysisResult eliminationAnalysisResult =
                        EliminationUtils.computeEliminationAnalysisForUnitWithInheritedRules(
                            unit,
                            eliminationAnalysisService,
                            param,
                            expirationDate
                        );

                    if (eliminationAnalysisResult.getGlobalStatus() != EliminationGlobalStatus.KEEP) {
                        JsonLineModel entry = new JsonLineModel(
                            unitId,
                            null,
                            JsonHandler.toJsonNode(eliminationAnalysisResult)
                        );
                        unitWriter.addEntry(entry);
                    }
                }
            }

            handler.transferFileToWorkspace(UNITS_JSONL_FILE, unitDistributionFile, true, false);
        } catch (IOException | ProcessingException | InvalidParseOperationException e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not generate unit and/or object group distributions",
                e
            );
        } finally {
            FileUtils.deleteQuietly(unitDistributionFile);
        }
    }

    private LocalDate getExpirationDate(EliminationRequestBody eliminationRequestBody)
        throws ProcessingStatusException {
        LocalDate expirationDate;
        try {
            expirationDate = LocalDate.parse(eliminationRequestBody.getDate());
        } catch (DateTimeParseException e) {
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setError(COULD_NOT_PARSE_DATE_FROM_REQUEST);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, COULD_NOT_PARSE_DATE_FROM_REQUEST, e);
        }
        return expirationDate;
    }

    private SelectMultiQuery getRequest(JsonNode dslRequest) throws ProcessingStatusException {
        try {
            SelectParserMultiple selectParser = new SelectParserMultiple();
            selectParser.parse(dslRequest);
            SelectMultiQuery request = selectParser.getRequest();

            // Update projection
            request.resetUsageProjection();
            request.addUsedProjection(
                VitamFieldsHelper.id(),
                VitamFieldsHelper.originatingAgency(),
                VitamFieldsHelper.unitType()
            );

            return request;
        } catch (InvalidParseOperationException e) {
            EliminationEventDetails eventDetails = new EliminationEventDetails().setError(COULD_NOT_PARSE_DSL_REQUEST);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, COULD_NOT_PARSE_DSL_REQUEST, e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ANALYSIS_PREPARATION;
    }
}
