/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;


import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class CheckDistributionThreshold extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    private static final String CHECK_DISTRIBUTION_THRESHOLD = "CHECK_DISTRIBUTION_THRESHOLD";


    private final MetaDataClientFactory metaDataClientFactory;

    public CheckDistributionThreshold() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting CheckDistributionThreshold(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }


    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            String queryType = (String) handler.getInput(0);
            String queryUri = (String) handler.getInput(1);

            JsonNode queryNode = handler.getJsonFromWorkspace(queryUri);

            RequestMultiple multiQuery = getRequestMultiple(queryNode, queryType);

            SelectMultiQuery selectMultiQuery = getSelectCountFromQuery(multiQuery);

            ObjectNode finalSelect = selectMultiQuery.getFinalSelect();

            JsonNode response = client.selectUnits(finalSelect);

            RequestResponseOK<JsonNode> responseOK = RequestResponseOK.getFromJsonNode(response);

            long total = responseOK.getHits().getTotal();

            Long requestThreshold = multiQuery.getThreshold();

            long defaultThreshold = VitamConfiguration.getDistributionThreshold();

            long threshold = (requestThreshold != null) ? requestThreshold : defaultThreshold;

            if (total > threshold) {

                ObjectNode eventDetails = JsonHandler.createObjectNode();
                eventDetails.put("error", "Too many units found. Threshold=" + threshold + ", found=" + total);

                return buildItemStatus(CHECK_DISTRIBUTION_THRESHOLD, StatusCode.KO, eventDetails);
            }

            if (total > defaultThreshold) {
                ObjectNode eventDetails = JsonHandler.createObjectNode();
                String errorMessage = String.format("Unit count exceeds default threshold. Default threshold=%d, found=%d", defaultThreshold, total);
                eventDetails.put("warning", errorMessage);

                return buildItemStatus(CHECK_DISTRIBUTION_THRESHOLD, StatusCode.WARNING, eventDetails);
            }

            return buildItemStatus(CHECK_DISTRIBUTION_THRESHOLD, StatusCode.OK, null);

        } catch (InvalidCreateOperationException | InvalidParseOperationException | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            LOGGER.error(e);
            return buildItemStatus(CHECK_DISTRIBUTION_THRESHOLD, StatusCode.FATAL, null);
        }
    }

    private RequestMultiple getRequestMultiple(JsonNode queryNode, String queryTypeString)
        throws InvalidParseOperationException {

        QueryType queryType = QueryType.valueOf(queryTypeString);
        RequestParserMultiple parser;
        switch (queryType) {
            case SELECT:
                parser = new SelectParserMultiple();
                break;
            case UPDATE:
                parser = new UpdateParserMultiple();
                break;
            default:
                throw new IllegalArgumentException(queryType.name());
        }

        parser.parse(queryNode);

        return parser.getRequest();
    }

    SelectMultiQuery getSelectCountFromQuery(RequestMultiple multiQuery)
        throws InvalidParseOperationException, InvalidCreateOperationException {

        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();

        String[] roots = multiQuery.getRoots().toArray(new String[0]);
        selectMultiQuery.addRoots(roots);

        Query[] queries = multiQuery.getQueries().toArray(new Query[0]);
        selectMultiQuery.addQueries(queries);

        selectMultiQuery.setLimitFilter(0, 1);

        selectMultiQuery.setProjection(JsonHandler.getFromString("{\"$fields\": { \"#id\": 1}}"));

        return selectMultiQuery;
    }

}
