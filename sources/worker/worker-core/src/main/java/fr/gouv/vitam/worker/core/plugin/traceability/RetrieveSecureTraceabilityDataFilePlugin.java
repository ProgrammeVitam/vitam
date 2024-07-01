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

package fr.gouv.vitam.worker.core.plugin.traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.batch.report.model.TraceabilityError;
import fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.HandlerUtils;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.WorkspaceConstants.ERROR_FLAG;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class RetrieveSecureTraceabilityDataFilePlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        RetrieveSecureTraceabilityDataFilePlugin.class
    );
    private static final String PLUGIN_NAME = "RETRIEVE_SECURE_TRACEABILITY_DATA_FILE";
    private static final int TRACEABILITY_EVENT_OUT_RANK = 0;
    private static final int DIGEST_OUT_RANK = 1;
    private static final String DIGEST = "digest";

    private final StorageClientFactory storageClientFactory;

    public RetrieveSecureTraceabilityDataFilePlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    RetrieveSecureTraceabilityDataFilePlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            JsonNode eventDetail = param.getObjectMetadata();
            TraceabilityEvent traceabilityEvent = JsonHandler.getFromJsonNode(eventDetail, TraceabilityEvent.class);
            DataCategory dataCategory = getDataCategory(traceabilityEvent);

            Map<String, String> digests = new HashMap<>();

            List<String> offerIds = storageClient.getOffers(VitamConfiguration.getDefaultStrategy());
            Map<String, Boolean> existsMap = new HashMap<>(
                storageClient.exists(
                    VitamConfiguration.getDefaultStrategy(),
                    dataCategory,
                    traceabilityEvent.getFileName(),
                    offerIds
                )
            );
            boolean exists = existsMap.values().stream().allMatch(e -> e);
            if (!exists) {
                existsMap.values().removeIf(Predicate.isEqual(Boolean.TRUE));
                ItemStatus result = buildItemStatusWithMessage(
                    PLUGIN_NAME,
                    StatusCode.KO,
                    String.format(
                        "Unable to find data file with name %s in all offers %s",
                        traceabilityEvent.getFileName(),
                        existsMap.keySet().toString()
                    )
                );
                saveReport(
                    param,
                    handler,
                    createTraceabilityReportEntry(
                        param,
                        traceabilityEvent,
                        digests,
                        result,
                        TraceabilityError.FILE_NOT_FOUND
                    )
                );
                HandlerUtils.save(handler, "", param.getObjectName() + File.separator + ERROR_FLAG);
                return result;
            }

            digests.putAll(
                getOfferDigests(
                    storageClient,
                    dataCategory,
                    traceabilityEvent.getFileName(),
                    VitamConfiguration.getDefaultStrategy(),
                    offerIds
                )
            );

            Set<String> hashes = new HashSet<>(digests.values());
            if (hashes.isEmpty()) {
                ItemStatus result = buildItemStatusWithMessage(
                    PLUGIN_NAME,
                    StatusCode.KO,
                    "Error: unable to retrive all hashes!"
                );
                saveReport(
                    param,
                    handler,
                    createTraceabilityReportEntry(
                        param,
                        traceabilityEvent,
                        digests,
                        result,
                        TraceabilityError.HASH_NOT_FOUND
                    )
                );
                HandlerUtils.save(handler, "", param.getObjectName() + File.separator + ERROR_FLAG);
                return result;
            }
            try {
                final String digest = Iterables.getOnlyElement(hashes);

                HandlerUtils.save(handler, traceabilityEvent, TRACEABILITY_EVENT_OUT_RANK);
                handler.addOutputResult(DIGEST_OUT_RANK, digest);
            } catch (IllegalArgumentException e) {
                ItemStatus result = buildItemStatusWithMessage(
                    PLUGIN_NAME,
                    StatusCode.KO,
                    "Error: hashes are not equals!"
                );
                saveReport(
                    param,
                    handler,
                    createTraceabilityReportEntry(
                        param,
                        traceabilityEvent,
                        digests,
                        result,
                        TraceabilityError.INEQUAL_HASHES
                    )
                );
                HandlerUtils.save(handler, "", param.getObjectName() + File.separator + ERROR_FLAG);
                return result;
            }
            ItemStatus result = buildItemStatus(PLUGIN_NAME, StatusCode.OK);
            saveReport(param, handler, createTraceabilityReportEntry(param, traceabilityEvent, digests, result, null));
            return result;
        } catch (
            InvalidParseOperationException
            | StorageServerClientException
            | StorageNotFoundClientException
            | IOException e
        ) {
            throw new ProcessingException(e);
        }
    }

    private TraceabilityReportEntry createTraceabilityReportEntry(
        WorkerParameters param,
        TraceabilityEvent traceabilityEvent,
        Map<String, String> digests,
        ItemStatus result,
        TraceabilityError error
    ) {
        return new TraceabilityReportEntry(
            param.getObjectName(),
            traceabilityEvent.getLogType().name(),
            result.getGlobalStatus().name(),
            result.getMessage(),
            error,
            null,
            digests,
            traceabilityEvent.getFileName(),
            null
        );
    }

    private void saveReport(
        WorkerParameters param,
        HandlerIO handlerIO,
        TraceabilityReportEntry traceabilityReportEntry
    ) throws IOException, ProcessingException {
        HandlerUtils.save(
            handlerIO,
            traceabilityReportEntry,
            param.getObjectName() + File.separator + WorkspaceConstants.REPORT
        );
    }

    // TODO : make it a static method in a shared class
    private DataCategory getDataCategory(TraceabilityEvent traceabilityEvent) {
        if (traceabilityEvent.getLogType() == null) {
            throw new IllegalStateException("Missing traceability event type");
        }

        switch (traceabilityEvent.getLogType()) {
            case OPERATION:
            case UNIT_LIFECYCLE:
            case OBJECTGROUP_LIFECYCLE:
                return DataCategory.LOGBOOK;
            case STORAGE:
                return DataCategory.STORAGETRACEABILITY;
            default:
                throw new IllegalStateException("Invalid traceability event type " + traceabilityEvent.getLogType());
        }
    }

    private Map<String, String> getOfferDigests(
        StorageClient storageClient,
        DataCategory dataCategory,
        String objectGuid,
        String strategyId,
        List<String> offerIds
    ) throws StorageNotFoundClientException, StorageServerClientException {
        JsonNode information = storageClient.getInformation(strategyId, dataCategory, objectGuid, offerIds, true);

        return offerIds
            .stream()
            .map(e -> new SimpleEntry<>(e, information.get(e)))
            .filter(e -> Objects.nonNull(e.getValue()))
            .filter(e -> !e.getValue().isMissingNode())
            .filter(e -> !e.getValue().isNull())
            .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().get(DIGEST).textValue()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
}
