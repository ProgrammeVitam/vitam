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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamFatalRuntimeException;
import fr.gouv.vitam.common.exception.VitamKoRuntimeException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PrepareObjectGroupLfcTraceabilityActionPlugin.
 */
public class PrepareObjectGroupLfcTraceabilityActionPlugin extends PrepareLfcTraceabilityActionPlugin {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        PrepareObjectGroupLfcTraceabilityActionPlugin.class
    );

    private static final String ACTION_HANDLER_ID = "PREPARE_OG_LFC_TRACEABILITY";
    private static final String STP_OG_LFC_TRACEABILITY = "STP_OG_LFC_TRACEABILITY";

    public PrepareObjectGroupLfcTraceabilityActionPlugin() {
        super();
    }

    @VisibleForTesting
    PrepareObjectGroupLfcTraceabilityActionPlugin(
        MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        int batchSize
    ) {
        super(metaDataClientFactory, logbookLifeCyclesClientFactory, batchSize);
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) throws ProcessingException {
        String temporizationDelayInSecondsStr = params
            .getMapParameters()
            .get(WorkerParameterName.lifecycleTraceabilityTemporizationDelayInSeconds);
        String lifecycleTraceabilityMaxEntriesStr = params
            .getMapParameters()
            .get(WorkerParameterName.lifecycleTraceabilityMaxEntries);
        int temporizationDelayInSeconds = Integer.parseInt(temporizationDelayInSecondsStr);
        int lifecycleTraceabilityMaxEntries = Integer.parseInt(lifecycleTraceabilityMaxEntriesStr);

        final ItemStatus itemStatus = new ItemStatus(ACTION_HANDLER_ID);
        try {
            selectAndExportLifecyclesWithMetadata(
                temporizationDelayInSeconds,
                lifecycleTraceabilityMaxEntries,
                Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType(),
                handler
            );
            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException | LogbookClientException | VitamFatalRuntimeException e) {
            LOGGER.error("Logbook exception", e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (InvalidParseOperationException | VitamKoRuntimeException e) {
            LOGGER.error("Processing exception", e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(ACTION_HANDLER_ID).setItemsStatus(ACTION_HANDLER_ID, itemStatus);
    }

    protected InputStream exportRawLifecyclesByLastPersistedDate(
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        LocalDateTime startDate,
        LocalDateTime endDate,
        int maxEntries
    ) throws LogbookClientException, InvalidParseOperationException, IOException {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            return logbookLifeCyclesClient.exportRawObjectGroupLifecyclesByLastPersistedDate(
                startDate,
                endDate,
                maxEntries
            );
        }
    }

    @Override
    protected Map<String, JsonNode> getRawMetadata(Set<String> ids, MetaDataClientFactory metaDataClientFactory)
        throws ProcessingException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            RequestResponse<JsonNode> rawMetadataResponse = metaDataClient.getObjectGroupsByIdsRaw(ids);

            if (!rawMetadataResponse.isOk()) {
                throw new ProcessingException(
                    "Could not retrieve raw metadata " +
                    rawMetadataResponse.getStatus() +
                    " " +
                    ((VitamError) rawMetadataResponse).getDescription()
                );
            }

            // Return raw metadata mapped by id
            return ((RequestResponseOK<JsonNode>) rawMetadataResponse).getResults()
                .stream()
                .collect(
                    Collectors.toMap(
                        rawMetadata -> rawMetadata.get(VitamDocument.ID).textValue(),
                        rawMetadata -> rawMetadata
                    )
                );
        } catch (VitamClientException e) {
            throw new ProcessingException("Could not retrieve raw metadata", e);
        }
    }

    @Override
    protected String stepName() {
        return STP_OG_LFC_TRACEABILITY;
    }

    @Override
    protected String actionName() {
        return ACTION_HANDLER_ID;
    }

    public static String getId() {
        return ACTION_HANDLER_ID;
    }
}
