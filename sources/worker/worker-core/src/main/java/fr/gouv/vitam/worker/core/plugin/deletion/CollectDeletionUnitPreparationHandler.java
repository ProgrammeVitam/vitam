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
package fr.gouv.vitam.worker.core.plugin.deletion;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.jsonl.JsonLineWriter;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.elimination.EliminationActionUnitPreparationHandlerBase;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationEventDetails;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationUtils.loadRequestJsonFromWorkspace;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Collect Deletion unit preparation handler.
 */
public class CollectDeletionUnitPreparationHandler extends EliminationActionUnitPreparationHandlerBase {

    private static final String COLLECT_DELETION_ACTION_UNIT_PREPARATION = "COLLECT_DELETION_ACTION_UNIT_PREPARATION";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        CollectDeletionUnitPreparationHandler.class
    );

    /**
     * Default constructor
     */
    public CollectDeletionUnitPreparationHandler() {
        super();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            EliminationRequestBody eliminationRequestBody = loadRequestJsonFromWorkspace(handler);

            ItemStatus itemStatus = process(eliminationRequestBody, handler);

            LOGGER.info("Deletion unit preparation succeeded");

            return itemStatus;
        } catch (ProcessingStatusException e) {
            LOGGER.error("Collect Deletion action unit preparation failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(COLLECT_DELETION_ACTION_UNIT_PREPARATION, e.getStatusCode(), e.getEventDetails());
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return COLLECT_DELETION_ACTION_UNIT_PREPARATION;
    }

    private ItemStatus process(EliminationRequestBody eliminationRequestBody, HandlerIO handler)
        throws ProcessingStatusException {
        SelectMultiQuery request = getRequest(eliminationRequestBody.getDslRequest());

        try (MetaDataClient client = handler.getMetaDataClient()) {
            ScrollSpliterator<JsonNode> unitScrollSpliterator = ScrollSpliteratorHelper.getUnitsScrollSpliterator(
                request,
                client
            );

            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(unitScrollSpliterator);

            int nbDestroyableUnits = 0;
            int nbNonDestroyableUnits = 0;

            File unitsToDelete = handler.getNewLocalFile(UNITS_TO_DELETE_FILE);

            try (
                FileOutputStream fileOutputStream = new FileOutputStream(unitsToDelete);
                JsonLineWriter unitsToDeleteWriter = new JsonLineWriter(fileOutputStream)
            ) {
                while (unitIterator.hasNext()) {
                    JsonNode unit = unitIterator.next();
                    String unitId = unit.get(VitamFieldsHelper.id()).asText();
                    unitsToDeleteWriter.addEntry(
                        new JsonLineModel(unitId, unit.get(VitamFieldsHelper.max()).asInt(), unit)
                    );
                    nbDestroyableUnits++;
                }
            }

            handler.transferFileToWorkspace(UNITS_TO_DELETE_FILE, unitsToDelete, true, false);

            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setExpirationDate(eliminationRequestBody.getDate())
                .setNbDestroyableUnits(nbDestroyableUnits)
                .setNbNonDestroyableUnits(nbNonDestroyableUnits);

            return buildItemStatus(COLLECT_DELETION_ACTION_UNIT_PREPARATION, StatusCode.OK, eventDetails);
        } catch (IOException | ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not generate unit distribution file", e);
        }
    }
}
