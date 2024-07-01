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
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import java.util.List;

/**
 * This plugin, add diff (some original object group data) to item status, so, WorkerImpl can create a GOT LFC with those information
 */
public class UpdateObjectGroupPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UpdateObjectGroupPlugin.class);

    private static final String OBJECT_GROUP_UPDATE = "OBJECT_GROUP_UPDATE";

    /**
     * @param params {@link WorkerParameters}
     * @param handlerIO
     * @return
     * @throws ProcessingException
     */
    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(OBJECT_GROUP_UPDATE);

        try {
            // Get objectGroup
            final JsonNode existingGot = handlerIO.getJsonFromWorkspace(
                IngestWorkflowConstants.UPDATE_OBJECT_GROUP_FOLDER + "/" + params.getObjectName()
            );

            List<String> diffList = VitamDocument.getConcernedDiffLines(
                VitamDocument.getUnifiedDiff(
                    " {}",
                    " " + existingGot.get(SedaConstants.PREFIX_WORK).get(SedaConstants.PREFIX_EXISTING).toString()
                )
            );

            ObjectNode diffObject = JsonHandler.createObjectNode();
            diffObject.put("diff", String.join("\n", diffList));
            diffObject.put("version", VitamConfiguration.getDiffVersion());

            try {
                itemStatus.setEvDetailData(JsonHandler.writeAsString(diffObject));
            } catch (InvalidParseOperationException e) {
                LOGGER.error(e);
                itemStatus.increment(StatusCode.FATAL);
            }

            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(OBJECT_GROUP_UPDATE).setItemsStatus(OBJECT_GROUP_UPDATE, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
}
