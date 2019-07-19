/*
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
*/

package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;

public class ComputeInheritedRuleProgenyIdentifierPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ComputeInheritedRuleProgenyIdentifierPlugin.class);

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_PROGENY_IDENTIFIER";
    private static final int DISTRIBUTION_FILE_RANK = 0;
    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<JsonLineModel>() {};
    private final MetaDataClientFactory metaDataClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;
    private final int bulkSize;

    public ComputeInheritedRuleProgenyIdentifierPlugin() {
        this(MetaDataClientFactory.getInstance(), BatchReportClientFactory.getInstance(), GlobalDatasDb.LIMIT_LOAD);
    }

    @VisibleForTesting
    ComputeInheritedRuleProgenyIdentifierPlugin(MetaDataClientFactory metaDataClientFactory, BatchReportClientFactory batchReportClientFactory, int bulkSize) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
        this.bulkSize = bulkSize;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) throws ProcessingException {
        String processId = workerParameters.getObjectNameList().get(0);

        if(StringUtils.isEmpty(processId)) {
            LOGGER.error("processId null or empty.");
            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.FATAL);
        }

        handler.setCurrentObjectId(processId);
        try(InputStream inputStream = new FileInputStream((File) handler.getInput(0));
            JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(inputStream, TYPE_REFERENCE);
            BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            AtomicInteger counter = new AtomicInteger();
            lines.stream()
                .collect(Collectors.groupingBy(line -> counter.getAndIncrement() / bulkSize))
                .forEach((it, unitsToBatch) -> findAndSaveUnitsProgeny(unitsToBatch, processId)); // don't care about "it", but "it" is mandatory...

            String distribFileName = handler.getOutput(DISTRIBUTION_FILE_RANK).getPath();
            File distribFile = handler.getNewLocalFile(distribFileName);

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitsToInvalidateScrollSpliterator(batchReportClient, processId, bulkSize);

            createDistributionFile(scrollRequest, distribFile);
            batchReportClient.deleteUnitsAndProgeny(processId);

            // move file to workspace
            handler.transferFileToWorkspace(distribFileName, distribFile, true, false);

        } catch (IOException | VitamClientInternalException e) {
            throw new ProcessingException(e);
        }

        return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.OK);
    }

    private JsonLineModel getJsonLineForItem(JsonNode item) {
        return new JsonLineModel(item.textValue(), null, null);
    }

    private void findAndSaveUnitsProgeny(List<JsonLineModel> unitsToBatch, String operationId) {
        SelectMultiQuery select = new SelectMultiQuery();

        JsonNode projection = JsonHandler.createObjectNode()
            .set("$fields", JsonHandler.createObjectNode().put(VitamFieldsHelper.id(), 1));

        select.addProjection(projection);

        String[] parentsIds = unitsToBatch.stream()
            .map(JsonLineModel::getId)
            .toArray(String[]::new);

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient();
             BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            InQuery childrenUnitsQuery = QueryHelper.in(VitamFieldsHelper.allunitups(), parentsIds);
            select.setQuery(childrenUnitsQuery);
            JsonNode response = metaDataClient.selectUnits(select.getFinalSelect());
            List<JsonNode> results = RequestResponseOK.getFromJsonNode(response).getResults();

            List<String> unitsIds = results.stream()
                .map(result -> Objects.requireNonNull(result.get(VitamFieldsHelper.id()).asText()))
                .collect(Collectors.toList());

            unitsIds.addAll(Arrays.asList(parentsIds));

            batchReportClient.saveUnitsAndProgeny(operationId, unitsIds);
        } catch (InvalidCreateOperationException | MetaDataException | InvalidParseOperationException | VitamClientInternalException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void createDistributionFile(final ScrollSpliterator<JsonNode> scrollRequest, File distribFile)
        throws ProcessingException {
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distribFile))) {

            StreamSupport.stream(scrollRequest, false).forEach(
                item -> {
                    try {
                        jsonLineWriter.addEntry(getJsonLineForItem(item));
                    } catch (IOException e) {
                        throw new VitamRuntimeException(e);
                    }
                }
            );

        } catch (IOException | VitamRuntimeException | IllegalStateException e) {
            throw new ProcessingException("Could not generate and save file", e);
        }
    }
}
