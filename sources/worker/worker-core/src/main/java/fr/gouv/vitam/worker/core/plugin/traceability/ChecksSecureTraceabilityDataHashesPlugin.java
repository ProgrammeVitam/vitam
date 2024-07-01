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

import com.google.common.annotations.VisibleForTesting;
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
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
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
import java.util.function.Consumer;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.WorkspaceConstants.ERROR_FLAG;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusWithMessage;

public class ChecksSecureTraceabilityDataHashesPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ChecksSecureTraceabilityDataHashesPlugin.class
    );
    private static final String PLUGIN_NAME = "CHECKS_SECURE_TRACEABILITY_DATA_HASHES";
    private static final int TRACEABILITY_EVENT_IN_RANK = 0;
    private static final int DIGEST_IN_RANK = 1;

    private StorageClientFactory storageClientFactory;

    public ChecksSecureTraceabilityDataHashesPlugin() {
        this(StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    protected ChecksSecureTraceabilityDataHashesPlugin(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        if (handler.isExistingFileInWorkspace(param.getObjectName() + File.separator + ERROR_FLAG)) {
            return buildItemStatus(PLUGIN_NAME, KO);
        }
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            File traceabilityEventJsonFile = (File) handler.getInput(TRACEABILITY_EVENT_IN_RANK);
            // todo : add zip fingerprint to TraceabilityEvent
            TraceabilityEvent traceabilityEvent = JsonHandler.getFromFile(
                traceabilityEventJsonFile,
                TraceabilityEvent.class
            );

            String digest = String.valueOf(handler.getInput(DIGEST_IN_RANK));

            DataCategory dataCategory = getDataCategory(traceabilityEvent);

            Response response = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                traceabilityEvent.getFileName(),
                dataCategory,
                AccessLogUtils.getNoLogAccessLog()
            );

            File traceabilityFile = response.readEntity(File.class);

            DigestType digestType = (traceabilityEvent.getDigestAlgorithm() != null)
                ? traceabilityEvent.getDigestAlgorithm()
                : VitamConfiguration.getDefaultDigestType();

            Digest eventDigest = new Digest(digestType);
            eventDigest.update(traceabilityFile);
            if (!eventDigest.digestHex().equals(digest)) {
                ItemStatus result = buildItemStatusWithMessage(PLUGIN_NAME, StatusCode.KO, "Invalid data fingerprint");
                updateReport(
                    param,
                    handler,
                    report ->
                        report
                            .setMessage(result.getMessage())
                            .setSecuredHash(eventDigest.digestHex())
                            .setError(TraceabilityError.INVALID_FINGERPRINT)
                );
                HandlerUtils.save(handler, "", param.getObjectName() + File.separator + ERROR_FLAG);
                return result;
            }

            handler.addOutputResult(0, traceabilityFile, false);

            ItemStatus result = buildItemStatus(PLUGIN_NAME, StatusCode.OK);
            updateReport(
                param,
                handler,
                report -> report.setMessage(result.getMessage()).setSecuredHash(eventDigest.digestHex())
            );
            return result;
        } catch (
            InvalidParseOperationException
            | StorageServerClientException
            | StorageNotFoundException
            | IOException
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
}
