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

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.PushAction;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationAnalysisResult;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Elimination analysis unit indexation plugin.
 */
public class EliminationAnalysisUnitIndexationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        EliminationAnalysisUnitIndexationPlugin.class
    );

    private static final String ELIMINATION_ANALYSIS_UNIT_INDEXATION = "ELIMINATION_ANALYSIS_UNIT_INDEXATION";

    private final MetaDataClientFactory metaDataClientFactory;

    /**
     * Default constructor
     */
    public EliminationAnalysisUnitIndexationPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    EliminationAnalysisUnitIndexationPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            EliminationAnalysisResult eliminationAnalysisResult = getUnitEliminationAnalysisResult(param);

            String unitId = param.getObjectName();
            indexUnit(unitId, eliminationAnalysisResult);

            LOGGER.info("Elimination analysis unit indexation succeeded");
            return buildItemStatus(ELIMINATION_ANALYSIS_UNIT_INDEXATION, StatusCode.OK, null);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Elimination analysis unit indexation failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(ELIMINATION_ANALYSIS_UNIT_INDEXATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    private void indexUnit(String unitId, EliminationAnalysisResult eliminationAnalysisResult)
        throws ProcessingStatusException {
        try (MetaDataClient client = this.metaDataClientFactory.getClient()) {
            UpdateMultiQuery updateMultiQuery = new UpdateMultiQuery();

            updateMultiQuery.addActions(
                new PushAction(VitamFieldsHelper.elimination(), JsonHandler.toJsonNode(eliminationAnalysisResult))
            );

            client.updateUnitById(updateMultiQuery.getFinalUpdateById(), unitId);
        } catch (
            InvalidParseOperationException
            | InvalidCreateOperationException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException
            | MetaDataNotFoundException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not index unit " + unitId, e);
        }
    }

    private EliminationAnalysisResult getUnitEliminationAnalysisResult(WorkerParameters params)
        throws ProcessingStatusException {
        try {
            return JsonHandler.getFromJsonNode(params.getObjectMetadata(), EliminationAnalysisResult.class);
        } catch (Exception e) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                "Could not retrieve unit elimination analysis information",
                e
            );
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return ELIMINATION_ANALYSIS_UNIT_INDEXATION;
    }
}
