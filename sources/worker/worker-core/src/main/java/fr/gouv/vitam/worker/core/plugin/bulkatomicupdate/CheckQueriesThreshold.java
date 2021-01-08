/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.worker.core.plugin.bulkatomicupdate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

/**
 * Check is the threshold value (provides in query.json or default platform) is
 * less of equal than number of queries in query.json file ($queries)
 */
public class CheckQueriesThreshold extends ActionHandler {

    private final long DEFAULT_THRESHOLD = VitamConfiguration.getQueriesThreshold();
    private static final String CHECK_QUERIES_THRESHOLD_PLUGIN_NAME = "CHECK_QUERIES_THRESHOLD";
    private static final String QUERY_NAME_IN = "query.json";

    public CheckQueriesThreshold() {
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        JsonNode queryNode = handler.getJsonFromWorkspace(QUERY_NAME_IN);
        boolean hasThresholdParameter = queryNode.has(BulkAtomicUpdateModelUtils.THRESHOLD);
        ArrayNode queries = (ArrayNode) queryNode.get(BulkAtomicUpdateModelUtils.QUERIES);

        final long total = queries.size();
        final long threshold = hasThresholdParameter ? queryNode.get(BulkAtomicUpdateModelUtils.THRESHOLD).asLong() :
                DEFAULT_THRESHOLD;
        
        if (total > threshold) {
            ObjectNode eventDetails = JsonHandler.createObjectNode();
            eventDetails
                .put("error", String.format("Too many queries found. Threshold=%d, found=%d", threshold, total));
            return buildItemStatus(CHECK_QUERIES_THRESHOLD_PLUGIN_NAME, StatusCode.KO, eventDetails);
        }

        if (total > DEFAULT_THRESHOLD) {
            ObjectNode eventDetails = JsonHandler.createObjectNode();
            eventDetails
                .put("warning", String.format("Queries count exceeds default threshold. Default threshold=%d, found=%d",
                    DEFAULT_THRESHOLD, total));

            return buildItemStatus(CHECK_QUERIES_THRESHOLD_PLUGIN_NAME, StatusCode.WARNING, eventDetails);
        }

        return buildItemStatus(CHECK_QUERIES_THRESHOLD_PLUGIN_NAME, StatusCode.OK, null);
    }
}
