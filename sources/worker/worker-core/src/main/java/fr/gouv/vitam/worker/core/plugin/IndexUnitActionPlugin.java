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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.InsertMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertEntry;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IndexUnitAction Plugin
 */
public class IndexUnitActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IndexUnitActionPlugin.class);
    private static final String HANDLER_PROCESS = "INDEXATION";

    private static final String ARCHIVE_UNIT = "ArchiveUnit";
    private static final String TAG_WORK = "_work";
    private static final int SEDA_PARAMETERS_RANK = 0;

    private final MetaDataClientFactory metaDataClientFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public IndexUnitActionPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    /**
     * Useful for inject mock in test class
     *
     * @param metaDataClientFactory instance of metaDataClientFactory or mock
     */
    @VisibleForTesting
    IndexUnitActionPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    /**
     * Retrieve id of this plugin INDEXATION
     *
     * @return HANDLER_ID id of this plugin
     */
    public static String getId() {
        return HANDLER_PROCESS;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO param) {
        throw new RuntimeException();
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handlerIO) {
        try (MetaDataClient metadataClient = metaDataClientFactory.getClient()) {
            List<ItemStatus> itemStatuses = new ArrayList<>();

            List<QueryCache> queryCaches = new ArrayList<>();

            for (String objectId : workerParameters.getObjectNameList()) {
                workerParameters.setObjectName(objectId);
                handlerIO.setCurrentObjectId(objectId);

                checkMandatoryParameters(workerParameters);
                final ItemStatus itemStatus = new ItemStatus(HANDLER_PROCESS);
                QueryCache query = null;
                try {
                    query = indexArchiveUnit(
                        workerParameters,
                        workerParameters.getContainerName(),
                        workerParameters.getObjectName(),
                        handlerIO
                    );
                    if (!query.update) {
                        queryCaches.add(query);
                        itemStatus.increment(StatusCode.OK);
                        ItemStatus itemsStatus = new ItemStatus(HANDLER_PROCESS).setItemsStatus(
                            HANDLER_PROCESS,
                            itemStatus
                        );
                        itemStatuses.add(itemsStatus);
                    } else {
                        metadataClient.updateUnitById(
                            ((UpdateMultiQuery) query.requestMultiple).getFinalUpdate(),
                            query.unitId
                        );
                        itemStatus.increment(StatusCode.OK);
                        ItemStatus itemsStatus = new ItemStatus(HANDLER_PROCESS).setItemsStatus(
                            HANDLER_PROCESS,
                            itemStatus
                        );
                        itemStatuses.add(itemsStatus);
                    }
                } catch (final IllegalArgumentException | InvalidCreateOperationException e) {
                    LOGGER.error(e);
                    itemStatus.increment(StatusCode.KO);
                    ItemStatus itemsStatus = new ItemStatus(HANDLER_PROCESS).setItemsStatus(
                        HANDLER_PROCESS,
                        itemStatus
                    );
                    itemStatuses.add(itemsStatus);
                } catch (final ProcessingException e) {
                    LOGGER.error(e);
                    itemStatus.increment(StatusCode.FATAL);
                    ItemStatus itemsStatus = new ItemStatus(HANDLER_PROCESS).setItemsStatus(
                        HANDLER_PROCESS,
                        itemStatus
                    );
                    itemStatuses.add(itemsStatus);
                } catch (InvalidParseOperationException e) {
                    itemStatus.increment(StatusCode.FATAL);
                    ItemStatus itemsStatus = new ItemStatus(HANDLER_PROCESS).setItemsStatus(
                        HANDLER_PROCESS,
                        itemStatus
                    );
                    itemStatuses.add(itemsStatus);
                    throw new IllegalArgumentException(e);
                } catch (final MetaDataNotFoundException e) {
                    LOGGER.error("Unit references a non existing unit " + query.toString());
                    throw new IllegalArgumentException(e);
                } catch (final MetaDataException e) {
                    LOGGER.error(e);
                    itemStatus.increment(StatusCode.FATAL);
                }
            }

            List<BulkUnitInsertEntry> entries = queryCaches
                .stream()
                .map(query -> query.requestMultiple)
                .map(query -> ((InsertMultiQuery) query))
                .map(query -> new BulkUnitInsertEntry(query.getRoots(), query.getData()))
                .collect(Collectors.toList());
            StatusCode statusCode;
            try {
                metadataClient.insertUnitBulk(new BulkUnitInsertRequest(entries));
                statusCode = StatusCode.OK;
            } catch (final IllegalArgumentException e) {
                throw e;
            } catch (InvalidParseOperationException | MetaDataNotFoundException e) {
                throw new IllegalArgumentException(e);
            } catch (final MetaDataException e) {
                LOGGER.error(e);
                statusCode = StatusCode.FATAL;
            }

            if (statusCode == StatusCode.FATAL || statusCode == StatusCode.ALREADY_EXECUTED) {
                for (int i = 0; i < workerParameters.getObjectNameList().size(); i++) {
                    final ItemStatus itemStatus = new ItemStatus(HANDLER_PROCESS);
                    itemStatus.increment(statusCode);
                    ItemStatus itemsStatus = new ItemStatus(HANDLER_PROCESS).setItemsStatus(
                        HANDLER_PROCESS,
                        itemStatus
                    );
                    itemStatuses.set(i, itemsStatus);
                }
            }
            return itemStatuses;
        } finally {
            handlerIO.setCurrentObjectId(null);
        }
    }

    /**
     * Index archive unit
     *
     * @param params work parameters
     * @param operationId operation Id
     * @param unitId id unit
     * @param handlerIO handlerIO
     * @throws ProcessingException when error in execution
     * @throws InvalidCreateOperationException
     */
    private QueryCache indexArchiveUnit(
        WorkerParameters params,
        String operationId,
        String unitId,
        HandlerIO handlerIO
    ) throws ProcessingException, InvalidCreateOperationException {
        ParametersChecker.checkNullOrEmptyParameters(params);

        RequestMultiple query = null;
        InputStream input = null;
        try {
            input = handlerIO.getInputStreamFromWorkspace(
                IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + unitId
            );

            JsonNode archiveUnit = prepareArchiveUnitJson(input, operationId, unitId, handlerIO);
            final ObjectNode data = (ObjectNode) archiveUnit.get(ARCHIVE_UNIT);
            final JsonNode work = archiveUnit.get(TAG_WORK);
            Boolean existing = false;
            if (work != null && work.get("_existing") != null) {
                existing = work.get("_existing").asBoolean();
            }

            if (existing) {
                query = new UpdateMultiQuery();
            } else {
                query = new InsertMultiQuery();
            }
            // Add _up to archive unit json object
            if (work != null && work.get("_up") != null) {
                final ArrayNode parents = (ArrayNode) work.get("_up");
                query.addRoots(parents);
            }
            if (!Boolean.TRUE.equals(existing)) {
                ((InsertMultiQuery) query).addData(data);
                return new QueryCache(false, query, null);
            } else {
                ((UpdateMultiQuery) query).addActions(
                        UpdateActionHelper.push(VitamFieldsHelper.operations(), params.getContainerName())
                    );
                String existingAuGUID = data.get("_id").asText();
                return new QueryCache(true, query, existingAuGUID);
            }
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Internal Server Error", e);
            throw new ProcessingException(e);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error("Workspace Server Error", e);
            throw new ProcessingException(e);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Illegal Argument Exception for " + (query != null ? query.toString() : ""));
            throw e;
        } catch (IOException e) {
            LOGGER.error("Archive unit not found");
            throw new ProcessingException("Archive unit not found");
        } catch (InvalidCreateOperationException e) {
            LOGGER.error("InvalidCreateOperationException for " + (query != null ? query.toString() : ""));
            throw e;
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    private JsonNode prepareArchiveUnitJson(
        InputStream input,
        String containerId,
        String objectName,
        HandlerIO handlerIO
    ) throws InvalidParseOperationException, ProcessingException {
        try {
            ParametersChecker.checkParameter("Input stream is a mandatory parameter", input);
            ParametersChecker.checkParameter("Container id is a mandatory parameter", containerId);
            ParametersChecker.checkParameter("ObjectName id is a mandatory parameter", objectName);
        } catch (IllegalArgumentException e) {
            throw new ProcessingException(e);
        }
        JsonNode archiveUnit = JsonHandler.getFromInputStream(input);
        ObjectNode archiveUnitNode = (ObjectNode) archiveUnit.get(ARCHIVE_UNIT);

        final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK));
        if (
            sedaParameters
                .get(SedaConstants.TAG_ARCHIVE_TRANSFER)
                .get(SedaConstants.TAG_DATA_OBJECT_PACKAGE)
                .get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER) !=
            null
        ) {
            String prodService = sedaParameters
                .get(SedaConstants.TAG_ARCHIVE_TRANSFER)
                .get(SedaConstants.TAG_DATA_OBJECT_PACKAGE)
                .get(SedaConstants.TAG_ORIGINATINGAGENCYIDENTIFIER)
                .asText();

            ArrayNode originatingAgencies = JsonHandler.createArrayNode();
            originatingAgencies.add(prodService);

            archiveUnitNode.set(Unit.ORIGINATING_AGENCIES, originatingAgencies);
            archiveUnitNode.put(Unit.ORIGINATING_AGENCY, prodService);
        }
        return archiveUnit;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Handler without parameters input
    }

    static class QueryCache {

        boolean update;

        RequestMultiple requestMultiple;

        String unitId;

        public QueryCache(boolean update, RequestMultiple requestMultiple, String unitId) {
            this.update = update;
            this.requestMultiple = requestMultiple;
            this.unitId = unitId;
        }
    }
}
