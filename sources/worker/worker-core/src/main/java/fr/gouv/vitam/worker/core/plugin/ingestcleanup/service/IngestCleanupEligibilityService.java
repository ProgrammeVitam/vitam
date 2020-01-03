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

package fr.gouv.vitam.worker.core.plugin.ingestcleanup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.worker.core.plugin.ingestcleanup.report.CleanupReportManager;
import fr.gouv.vitam.worker.core.utils.BufferedConsumer;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;

public class IngestCleanupEligibilityService {
    private static final TypeReference<List<String>>
        STRING_LIST_TYPE_REFERENCE = new TypeReference<List<String>>() {
    };

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    public IngestCleanupEligibilityService(MetaDataClientFactory metaDataClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
    }

    public void checkChildUnitsFromOtherIngests(String ingestOperationId, CleanupReportManager cleanupReportManager)
        throws ProcessingStatusException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            SelectMultiQuery query = new SelectMultiQuery();
            query.addUsedProjection(VitamFieldsHelper.id());
            query.addQueries(eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createUnitScrollSplitIterator(client, query, VitamConfiguration.getBatchSize());

            try (BufferedConsumer<String> bufferedConsumer = new BufferedConsumer<>(VitamConfiguration.getBatchSize(),
                ids -> checkChildUnitsFromOtherIngests(ingestOperationId, cleanupReportManager, ids))) {

                scrollRequest.forEachRemaining(
                    entry -> bufferedConsumer.appendEntry(entry.get(VitamFieldsHelper.id()).asText()));
            }

        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not checks unit updates", e);
        }
    }

    private void checkChildUnitsFromOtherIngests(String ingestOperationId, CleanupReportManager cleanupReportManager,
        List<String> ids) {
        try {
            Set<String> unitsWithExternalAttachments = getUnitsWithExternalAttachments(ingestOperationId, ids);

            for (String unitWithExternalAttachments : unitsWithExternalAttachments) {
                cleanupReportManager.reportUnitError(unitWithExternalAttachments,
                    "Unit has child units from other ingest operations");
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException | MetaDataClientServerException | MetaDataDocumentSizeException | MetaDataExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getUnitsWithExternalAttachments(String ingestOperationId, Collection<String> unitIds)
        throws MetaDataExecutionException, MetaDataClientServerException,
        MetaDataDocumentSizeException, InvalidParseOperationException, InvalidCreateOperationException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            Set<String> unitsToFetch = new HashSet<>(unitIds);
            Set<String> result = new HashSet<>();

            while (!unitsToFetch.isEmpty()) {

                RequestResponseOK<JsonNode> responseOK =
                    selectExternalChildUnit(metaDataClient, ingestOperationId, unitsToFetch);

                Set<String> unitsWithChildren = parseUnitsWithChildren(responseOK.getResults(), unitsToFetch);
                if (unitsWithChildren.isEmpty()) {
                    break;
                }

                result.addAll(unitsWithChildren);
                unitsToFetch.removeAll(unitsWithChildren);

            }

            return result;

        }
    }

    private RequestResponseOK<JsonNode> selectExternalChildUnit(MetaDataClient metaDataClient,
        String ingestOperationId, Set<String> unitsToFetch)
        throws InvalidCreateOperationException, InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataClientServerException {

        SelectMultiQuery selectAllUnitsUp = new SelectMultiQuery();
        selectAllUnitsUp.addQueries(
            QueryHelper.and().add(
                QueryHelper.in(VitamFieldsHelper.unitups(), unitsToFetch.toArray(new String[0])),
                ne(VitamFieldsHelper.initialOperation(), ingestOperationId)
            ));
        selectAllUnitsUp.setLimitFilter(0, VitamConfiguration.getBatchSize());
        selectAllUnitsUp.addUsedProjection(VitamFieldsHelper.unitups());
        JsonNode response = metaDataClient.selectUnits(selectAllUnitsUp.getFinalSelect());
        return RequestResponseOK.getFromJsonNode(response);
    }

    private Set<String> parseUnitsWithChildren(List<JsonNode> results, Set<String> unitsToFetch) {
        Set<String> foundUnitIds = new HashSet<>();

        for (JsonNode childUnit : results) {
            childUnit.get(VitamFieldsHelper.unitups()).elements()
                .forEachRemaining(jsonNode -> {
                    String unitId = jsonNode.asText();
                    if (unitsToFetch.contains(unitId)) {
                        foundUnitIds.add(unitId);
                    }
                });
        }

        return foundUnitIds;
    }

    public void checkUnitUpdatesFromOtherOperations(String ingestOperationId,
        CleanupReportManager cleanupReportManager)
        throws ProcessingStatusException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            SelectMultiQuery query = new SelectMultiQuery();
            query.addUsedProjection(
                VitamFieldsHelper.id(),
                VitamFieldsHelper.operations());
            query.addQueries(eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createUnitScrollSplitIterator(client, query, VitamConfiguration.getBatchSize());

            scrollRequest.forEachRemaining(unitNode -> {
                try {

                    List<String> ops = JsonHandler
                        .getFromJsonNode(unitNode.get(VitamFieldsHelper.operations()), STRING_LIST_TYPE_REFERENCE);
                    if (ops.size() != 1) {

                        List<String> otherOperations = ops.stream().filter(op -> !op.equals(ingestOperationId))
                            .collect(Collectors.toList());

                        String id = unitNode.get(VitamFieldsHelper.id()).asText();
                        cleanupReportManager
                            .reportUnitWarning(id, "Updates occurred to unit by operations " + otherOperations);
                    }
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not checks updated units", e);
        }
    }

    public void checkObjectGroupUpdatesFromOtherOperations(String ingestOperationId,
        CleanupReportManager cleanupReportManager)
        throws ProcessingStatusException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            SelectMultiQuery query = new SelectMultiQuery();
            query.addQueries(eq(VitamFieldsHelper.initialOperation(), ingestOperationId));

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createObjectGroupScrollSplitIterator(client, query, VitamConfiguration.getBatchSize());

            MultiValuedMap<String, String> updatedObjectGroupsByOpi = new ArrayListValuedHashMap<>();

            scrollRequest.forEachRemaining(objectGroupNode -> {
                try {
                    List<String> ops = JsonHandler
                        .getFromJsonNode(objectGroupNode.get(VitamFieldsHelper.operations()),
                            STRING_LIST_TYPE_REFERENCE);

                    for (String op : ops) {
                        if (!op.equals(ingestOperationId)) {
                            String id = objectGroupNode.get(VitamFieldsHelper.id()).asText();
                            updatedObjectGroupsByOpi.put(op, id);
                        }
                    }
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            });

            if (!updatedObjectGroupsByOpi.isEmpty()) {

                Set<String> ingestOperations = checkIngestOperations(updatedObjectGroupsByOpi.keySet());
                Set<String> nonIngestOperations =
                    SetUtils.difference(updatedObjectGroupsByOpi.keySet(), ingestOperations);

                for (String nonIngestOperation : nonIngestOperations) {
                    for (String objectGroupId : updatedObjectGroupsByOpi.get(nonIngestOperation)) {
                        cleanupReportManager.reportObjectGroupWarning(objectGroupId,
                            "Updates occurred to object group by operation " + nonIngestOperations);
                    }
                }

                for (String nonIngestOperation : ingestOperations) {
                    for (String objectGroupId : updatedObjectGroupsByOpi.get(nonIngestOperation)) {
                        cleanupReportManager.reportObjectGroupError(objectGroupId,
                            "Updates occurred to object group by ingest operation " + nonIngestOperations);
                    }
                }
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not checks updated object groups", e);
        }
    }

    private Set<String> checkIngestOperations(Collection<String> operationIds)
        throws InvalidCreateOperationException, InvalidParseOperationException, LogbookClientException {

        Set<String> ingestOperations;
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {

            ingestOperations = new HashSet<>();
            Iterator<List<String>> operationsToCheckIterator =
                Iterators.partition(operationIds.iterator(), VitamConfiguration.getBatchSize());
            while (operationsToCheckIterator.hasNext()) {

                Select select = new Select();
                BooleanQuery query = and().add(
                    in("#id", operationsToCheckIterator.next().toArray(new String[0])),
                    eq(LogbookMongoDbName.eventTypeProcess.getDbname(), LogbookTypeProcess.INGEST.name())
                );

                select.setQuery(query);
                select.addUsedProjection("#id");

                JsonNode jsonNode = logbookOperationsClient.selectOperation(select.getFinalSelect());
                for (JsonNode logbook : jsonNode.get(TAG_RESULTS)) {
                    ingestOperations.add(logbook.get("#id").asText());
                }
            }
            return ingestOperations;
        }
    }

    public void checkObjectAttachmentsToExistingObjectGroups(String ingestOperationId,
        CleanupReportManager cleanupReportManager)
        throws ProcessingStatusException {

        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            SelectMultiQuery query = new SelectMultiQuery();
            query.addUsedProjection(VitamFieldsHelper.id());
            query.addQueries(and().add(
                ne(VitamFieldsHelper.initialOperation(), ingestOperationId),
                eq(VitamFieldsHelper.operations(), ingestOperationId)
            ));

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createObjectGroupScrollSplitIterator(client, query, VitamConfiguration.getBatchSize());

            scrollRequest.forEachRemaining(objectGroup -> {

                String objectGroupId = objectGroup.get("#id").asText();
                cleanupReportManager.reportObjectGroupError(objectGroupId,
                    "Existing object group updated by ingest operation " + ingestOperationId);
            });

        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not list attachment to existing object groups",
                e);
        }
    }
}
