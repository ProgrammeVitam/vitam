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
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.WorkspaceConstants;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.HandlerUtils;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry.FILE_ID;
import static fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry.OFFERS_HASHES;
import static fr.gouv.vitam.batch.report.model.entry.TraceabilityReportEntry.SECURED_HASH;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.WorkspaceConstants.DATA_FILE;
import static fr.gouv.vitam.common.model.WorkspaceConstants.ERROR_FLAG;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class ChecksSecureTraceabilityDataStoragelogPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ChecksSecureTraceabilityDataStoragelogPlugin.class
    );
    private static final String PLUGIN_NAME = "CHECKS_SECURE_TRACEABILITY_DATA_STORAGELOG";

    private static final int EVENT_DETAIL_DATA_IN_RANK = 0;

    private static final String DIGEST = "digest";

    private final StorageClientFactory storageClientFactory;

    public ChecksSecureTraceabilityDataStoragelogPlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    ChecksSecureTraceabilityDataStoragelogPlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        if (handler.isExistingFileInWorkspace(param.getObjectName() + File.separator + ERROR_FLAG)) {
            return buildItemStatus(PLUGIN_NAME, KO);
        }
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            File traceabilityEventJsonFile = (File) handler.getInput(EVENT_DETAIL_DATA_IN_RANK);
            TraceabilityEvent traceabilityEvent = JsonHandler.getFromFile(
                traceabilityEventJsonFile,
                TraceabilityEvent.class
            );
            if (traceabilityEvent.getLogType().equals(TraceabilityType.STORAGE)) {
                final String dataFilePath =
                    WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY +
                    File.separator +
                    param.getObjectName() +
                    File.separator +
                    DATA_FILE;
                // todo : add zip fingerprint to TraceabilityEvent
                TraceabilityEvent traceabilityDataEvent = JsonHandler.getFromJsonNode(
                    handler.getJsonFromWorkspace(dataFilePath),
                    TraceabilityEvent.class
                );

                Map<String, String> digests = new HashMap<>();

                List<String> offerIds = storageClient.getOffers(VitamConfiguration.getDefaultStrategy());
                Map<String, Boolean> existsMap = new HashMap<>(
                    storageClient.exists(
                        VitamConfiguration.getDefaultStrategy(),
                        DataCategory.STORAGELOG,
                        traceabilityDataEvent.getFileName(),
                        offerIds
                    )
                );
                boolean exists = existsMap.values().stream().allMatch(e -> e);
                if (!exists) {
                    existsMap.values().removeIf(Predicate.isEqual(Boolean.TRUE));
                    ItemStatus result = buildItemStatusWithMessage(
                        PLUGIN_NAME,
                        KO,
                        String.format(
                            "Cannot find storagelog data with filename %s in offers %s",
                            traceabilityDataEvent.getFileName(),
                            existsMap.keySet().toString()
                        )
                    );
                    updateReport(
                        param,
                        handler,
                        t ->
                            t
                                .setStatus(result.getGlobalStatus().name())
                                .setMessage(result.getMessage())
                                .setError(TraceabilityError.FILE_NOT_FOUND)
                                .appendExtraData(
                                    Map.of(FILE_ID, traceabilityDataEvent.getFileName(), OFFERS_HASHES, digests)
                                )
                    );
                    return result;
                }

                digests.putAll(
                    getOfferDigests(
                        storageClient,
                        traceabilityDataEvent.getFileName(),
                        VitamConfiguration.getDefaultStrategy(),
                        offerIds
                    )
                );

                Set<String> hashes = new HashSet<>(digests.values());
                if (hashes.isEmpty()) {
                    ItemStatus result = buildItemStatusWithMessage(
                        PLUGIN_NAME,
                        KO,
                        "Error: unable to retrive all hashes!"
                    );
                    updateReport(
                        param,
                        handler,
                        t ->
                            t
                                .setStatus(result.getGlobalStatus().name())
                                .setMessage(result.getMessage())
                                .setError(TraceabilityError.HASH_NOT_FOUND)
                                .appendExtraData(Map.of(OFFERS_HASHES, digests))
                    );
                    return result;
                }
                try {
                    final String digest = Iterables.getOnlyElement(hashes);

                    Response response = storageClient.getContainerAsync(
                        VitamConfiguration.getDefaultStrategy(),
                        traceabilityDataEvent.getFileName(),
                        DataCategory.STORAGELOG,
                        AccessLogUtils.getNoLogAccessLog()
                    );
                    DigestType digestType = (traceabilityDataEvent.getDigestAlgorithm() != null)
                        ? traceabilityDataEvent.getDigestAlgorithm()
                        : VitamConfiguration.getDefaultDigestType();

                    Digest eventDigest = new Digest(digestType);
                    eventDigest.update(response.readEntity(InputStream.class));

                    if (!eventDigest.digestHex().equals(digest)) {
                        ItemStatus result = buildItemStatusWithMessage(
                            PLUGIN_NAME,
                            KO,
                            "Storagelog data fingerprint invalid"
                        );
                        updateReport(
                            param,
                            handler,
                            t ->
                                t
                                    .setStatus(result.getGlobalStatus().name())
                                    .setMessage(result.getMessage())
                                    .setError(TraceabilityError.INVALID_FINGERPRINT)
                                    .appendExtraData(
                                        Map.of(OFFERS_HASHES, digests, SECURED_HASH, eventDigest.digestHex())
                                    )
                        );
                        return result;
                    }
                } catch (IllegalArgumentException e) {
                    ItemStatus result = buildItemStatusWithMessage(
                        PLUGIN_NAME,
                        KO,
                        "Error: Storagelog hashes are not equals!"
                    );
                    updateReport(
                        param,
                        handler,
                        t ->
                            t
                                .setStatus(result.getGlobalStatus().name())
                                .setMessage(result.getMessage())
                                .setError(TraceabilityError.INEQUAL_HASHES)
                                .appendExtraData(Map.of(OFFERS_HASHES, digests))
                    );
                    return result;
                }

                ItemStatus result = buildItemStatus(PLUGIN_NAME, StatusCode.OK);
                updateReport(
                    param,
                    handler,
                    t -> t.setStatus(result.getGlobalStatus().name()).setMessage(result.getMessage())
                );
                return result;
            }

            return buildItemStatus(PLUGIN_NAME, StatusCode.OK);
        } catch (
            InvalidParseOperationException
            | IOException
            | StorageServerClientException
            | StorageNotFoundClientException
            | StorageNotFoundException
            | StorageUnavailableDataFromAsyncOfferClientException e
        ) {
            throw new ProcessingException(e);
        }
    }

    private void updateReport(WorkerParameters param, HandlerIO handlerIO, Consumer<TraceabilityReportEntry> updater)
        throws IOException, ProcessingException, InvalidParseOperationException {
        String path = param.getObjectName() + File.separator + WorkspaceConstants.REPORT;
        TraceabilityReportEntry traceabilityReportEntry = JsonHandler.getFromJsonNode(
            handlerIO.getJsonFromWorkspace(path),
            TraceabilityReportEntry.class
        );
        updater.accept(traceabilityReportEntry);
        HandlerUtils.save(handlerIO, traceabilityReportEntry, path);
    }

    private Map<String, String> getOfferDigests(
        StorageClient storageClient,
        String objectGuid,
        String strategyId,
        List<String> offerIds
    ) throws StorageNotFoundClientException, StorageServerClientException {
        JsonNode information = storageClient.getInformation(
            strategyId,
            DataCategory.STORAGELOG,
            objectGuid,
            offerIds,
            true
        );

        return offerIds
            .stream()
            .map(e -> new SimpleEntry<>(e, information.get(e)))
            .filter(e -> Objects.nonNull(e.getValue()))
            .filter(e -> !e.getValue().isMissingNode())
            .filter(e -> !e.getValue().isNull())
            .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().get(DIGEST).textValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
