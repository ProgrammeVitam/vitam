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
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.elimination.EliminationRequestBody;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.elimination.model.EliminationEventDetails;

import static fr.gouv.vitam.worker.core.plugin.elimination.EliminationUtils.loadRequestJsonFromWorkspace;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Check distribution threshold.
 */
public abstract class EliminationCheckDistributionThresholdBase extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    private final MetaDataClientFactory metaDataClientFactory;

    EliminationCheckDistributionThresholdBase(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    protected ItemStatus checkThreshold(HandlerIO handler, long defaultThreshold, String action) {
        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            // get initial query string
            EliminationRequestBody eliminationRequestBody = loadRequestJsonFromWorkspace(handler);
            JsonNode dslRequest = eliminationRequestBody.getDslRequest();

            SelectParserMultiple selectParser = new SelectParserMultiple();
            selectParser.parse(dslRequest);
            SelectMultiQuery request = selectParser.getRequest();

            request.setLimitFilter(0, 1);
            // Enable trackTotalHits flag to compute total result set count
            request.trackTotalHits(true);

            // Update projection
            request.resetUsageProjection();
            request.addUsedProjection(VitamFieldsHelper.id());

            JsonNode response = client.selectUnits(request.getFinalSelect());
            RequestResponseOK<JsonNode> responseOK = RequestResponseOK.getFromJsonNode(response);

            // get total
            long total = responseOK.getHits().getTotal();

            // get threshold
            Long requestThreshold = request.getThreshold();

            long threshold = (requestThreshold != null) ? requestThreshold : defaultThreshold;

            if (total > threshold) {
                EliminationEventDetails eventDetails = new EliminationEventDetails()
                    .setError("Too many units found. Threshold=" + threshold + ", found=" + total);
                return buildItemStatus(action, StatusCode.KO, eventDetails);
            } else if (total > defaultThreshold) {
                EliminationEventDetails eventDetails = new EliminationEventDetails()
                    .setWarning(
                        "Unit count exceeds default threshold. Default threshold=" +
                        defaultThreshold +
                        ", found=" +
                        total
                    );
                return buildItemStatus(action, StatusCode.WARNING, eventDetails);
            } else {
                return buildItemStatus(action, StatusCode.OK, null);
            }
        } catch (
            InvalidParseOperationException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            LOGGER.error(e);
            EliminationEventDetails eventDetails = new EliminationEventDetails()
                .setError("An error occurred during elimination distribution check");
            return buildItemStatus(action, StatusCode.FATAL, eventDetails);
        } catch (ProcessingStatusException e) {
            return buildItemStatus(action, e.getStatusCode(), e.getEventDetails());
        }
    }
}
