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
package fr.gouv.vitam.worker.core.plugin.computeinheritedrules;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.model.entry.UnitComputedInheritedRulesInvalidationReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.ExistsQuery;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.iterables.SpliteratorIterator;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildBulkItemStatus;

public class ComputeInheritedRuleProgenyIdentifierPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ComputeInheritedRuleProgenyIdentifierPlugin.class
    );

    private static final String PLUGIN_NAME = "COMPUTE_INHERITED_RULES_PROGENY_IDENTIFIER";
    private static final TypeReference<JsonLineModel> TYPE_REFERENCE = new TypeReference<>() {};
    static final String UNITS_JSONL_FILE_NAME = "unitsToInvalidate.jsonl";

    private final MetaDataClientFactory metaDataClientFactory;
    private final BatchReportClientFactory batchReportClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final int bulkSize;

    public ComputeInheritedRuleProgenyIdentifierPlugin() {
        this(
            MetaDataClientFactory.getInstance(),
            BatchReportClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance(),
            GlobalDatas.LIMIT_LOAD
        );
        // Default constructor for workflow initialization by Worker
    }

    @VisibleForTesting
    ComputeInheritedRuleProgenyIdentifierPlugin(
        MetaDataClientFactory metaDataClientFactory,
        BatchReportClientFactory batchReportClientFactory,
        WorkspaceClientFactory workspaceClientFactory,
        int bulkSize
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.batchReportClientFactory = batchReportClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
        this.bulkSize = bulkSize;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {
        String processId = handler.getContainerName();

        if (StringUtils.isEmpty(processId)) {
            LOGGER.error("processId null or empty.");
            return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.FATAL);
        }

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingObject(handler.getContainerName(), UNITS_JSONL_FILE_NAME)) {
                LOGGER.warn(UNITS_JSONL_FILE_NAME + " already exists. Will be overridden with fresh data");
                workspaceClient.deleteObject(handler.getContainerName(), UNITS_JSONL_FILE_NAME);
            }
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            throw new ProcessingException(e);
        }

        handler.setCurrentObjectId(workerParameters.getObjectNameList().get(0));
        try (
            InputStream inputStream = new FileInputStream((File) handler.getInput(0));
            JsonLineGenericIterator<JsonLineModel> lines = new JsonLineGenericIterator<>(inputStream, TYPE_REFERENCE);
            BatchReportClient batchReportClient = batchReportClientFactory.getClient();
            MetaDataClient metaDataClient = metaDataClientFactory.getClient()
        ) {
            Iterator<List<JsonLineModel>> bulkLines = Iterators.partition(lines, bulkSize);
            bulkLines.forEachRemaining(
                unitsToBatch -> findAndSaveUnitsProgeny(metaDataClient, batchReportClient, unitsToBatch, processId)
            );

            batchReportClient.exportUnitsToInvalidate(processId, new ReportExportRequest(UNITS_JSONL_FILE_NAME));
        } catch (IOException | VitamClientInternalException e) {
            throw new ProcessingException(e);
        }

        return buildBulkItemStatus(workerParameters, PLUGIN_NAME, StatusCode.OK);
    }

    private void findAndSaveUnitsProgeny(
        MetaDataClient metaDataClient,
        BatchReportClient batchReportClient,
        List<JsonLineModel> unitsToBatch,
        String operationId
    ) {
        String[] parentsIds = unitsToBatch.stream().map(JsonLineModel::getId).toArray(String[]::new);

        try {
            InQuery childrenUnitsQuery = QueryHelper.in(VitamFieldsHelper.allunitups(), parentsIds);
            InQuery parentsUnitsQuery = QueryHelper.in(VitamFieldsHelper.id(), parentsIds);
            BooleanQuery parentsAndProgenyUnits = QueryHelper.or().add(childrenUnitsQuery, parentsUnitsQuery);

            ExistsQuery unitsToIndex = QueryHelper.exists(VitamFieldsHelper.validComputedInheritedRules());
            BooleanQuery unitsToInvalidate = QueryHelper.and().add(unitsToIndex, parentsAndProgenyUnits);

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(unitsToInvalidate);
            select.addUsedProjection(VitamFieldsHelper.id());

            // Query against ES via cursor
            Iterator<JsonNode> unitIterator = new SpliteratorIterator<>(
                ScrollSpliteratorHelper.createUnitScrollSplitIterator(metaDataClient, select)
            );

            // Map to unit Ids
            Iterator<String> unitIdIterator = IteratorUtils.transformedIterator(
                unitIterator,
                result -> Objects.requireNonNull(result.get(VitamFieldsHelper.id()).asText())
            );

            // Process in chunks
            Iterators.partition(unitIdIterator, VitamConfiguration.getBatchSize()).forEachRemaining(
                unitsIds -> appendUnitIdsToBatchReport(batchReportClient, operationId, unitsIds)
            );
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private void appendUnitIdsToBatchReport(
        BatchReportClient batchReportClient,
        String operationId,
        List<String> unitsIds
    ) {
        try {
            List<UnitComputedInheritedRulesInvalidationReportEntry> entries = unitsIds
                .stream()
                .distinct()
                .map(UnitComputedInheritedRulesInvalidationReportEntry::new)
                .collect(Collectors.toList());
            ReportBody<UnitComputedInheritedRulesInvalidationReportEntry> report = new ReportBody<>(
                operationId,
                ReportType.UNIT_COMPUTED_INHERITED_RULES_INVALIDATION,
                entries
            );
            batchReportClient.appendReportEntries(report);
        } catch (VitamClientInternalException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
