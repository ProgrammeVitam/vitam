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

package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ingest.CheckSanityItem;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.util.Arrays;

import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SANITY_CHECK_RESULT_FILE;
import static fr.gouv.vitam.common.model.StatusCode.UNKNOWN;
import static fr.gouv.vitam.common.model.ingest.CheckSanityItem.CHECK_DIGEST_MANIFEST;

public class IngestPrepareActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestPrepareActionHandler.class);
    private static final String IGNORED = "IGNORED";

    public IngestPrepareActionHandler() {}

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        LOGGER.debug("Start IngestPrepareActionHandler");
        try {
            String ingestParam = handler.getInput(0, String.class);
            if (Arrays.stream(CheckSanityItem.values()).noneMatch(elmt -> elmt.getItemParam().equals(ingestParam))) {
                throw new IllegalStateException(String.format("The param %d is not recognized", ingestParam));
            }

            JsonNode externalJsonResults = handler.getJsonFromWorkspace(SANITY_CHECK_RESULT_FILE);
            ItemStatus result = JsonHandler.getFromJsonNode(externalJsonResults.get(ingestParam), ItemStatus.class);

            // Add Logbook only when manifest digest is NOT OK
            if (
                result.getItemId().equals(CHECK_DIGEST_MANIFEST.getItemValue()) &&
                !result.getGlobalStatus().isGreaterOrEqualToKo()
            ) {
                return new ItemStatus(IGNORED).increment(UNKNOWN);
            }

            return new ItemStatus(result.getItemId()).setItemsStatus(result.getItemId(), result);
        } catch (InvalidParseOperationException e) {
            throw new ProcessingException("An exception occured in IngestPrepareActionHandler :" + e.getMessage(), e);
        }
    }
}
