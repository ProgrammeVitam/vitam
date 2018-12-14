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
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.SchemaValidationUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.MetadataStorageHelper;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.QueryProjection;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.StoreMetadataObjectActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.api.exception.WorkspaceClientServerException;
import org.apache.commons.collections.CollectionUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;

/**
 * Mass updating of archive units.
 */
public class MassUpdateUnitsProcess extends StoreMetadataObjectActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    /**
     * MASS_UPDATE_UNITS
     */
    private static final String MASS_UPDATE_UNITS = "MASS_UPDATE_UNITS";
    private static final String UNIT_METADATA_UPDATE = "UNIT_METADATA_UPDATE";

    /**
     * DISTRIBUTION_LOCAL_REPORTS_RANK
     */
    public static final int DISTRIBUTION_LOCAL_REPORTS_RANK = 0;

    private static final String JSON = ".json";
    private static final String EXTENSION = "json";
    private static final String REPORT = "report";
    private static final String SUCCESS = "success";
    private static final String WARNING = "warning";
    private static final String ERRORS = "errors";
    private static final String ID = "#id";
    private static final String STATUS = "#status";
    private static final String DIFF = "#diff";
    private static final String SEPARTOR = ",";

    /**
     * metaDataClientFactory
     */
    private MetaDataClientFactory metaDataClientFactory;

    /**
     * lfcClientFactory
     */
    private LogbookLifeCyclesClientFactory lfcClientFactory;

    /**
     * storageClientFactory
     */
    private StorageClientFactory storageClientFactory;

    /**
     * adminManagementClientFactory
     */
    private AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Constructor.
     */
    public MassUpdateUnitsProcess() {
        this(MetaDataClientFactory.getInstance(), LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance(), AdminManagementClientFactory.getInstance());
    }

    /**
     * Constructor.
     * @param metaDataClientFactory
     */
    @VisibleForTesting
    public MassUpdateUnitsProcess(MetaDataClientFactory metaDataClientFactory, LogbookLifeCyclesClientFactory lfcClientFactory,
        StorageClientFactory storageClientFactory, AdminManagementClientFactory adminManagementClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.lfcClientFactory = lfcClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
    }

    /**
     * Execute an action
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     * @throws ContentAddressableStorageServerException if a storage exception is encountered when executing the action
     */
    @Override public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {
        throw new IllegalStateException("UnsupporedOperation");
    }

    /**
     * executeList for bulk update units.
     * @param workerParameters
     * @param handler
     * @return
     * @throws ProcessingException
     */
    @Override public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler)
        throws ProcessingException {

        final List<ItemStatus> itemStatuses = new ArrayList<>();

        // Bulk update units && local reports generation
        try (MetaDataClient mdClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient lfcClient = lfcClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient()) {

            // get initial query string
            JsonNode queryNode = handler.getJsonFromWorkspace("query.json");

            // parse multi query
            UpdateParserMultiple parser = new UpdateParserMultiple();
            parser.parse(queryNode);

            // Add operationID to #operations
            parser.getRequest().addActions(UpdateActionHelper.push(
                VitamFieldsHelper.operations(), VitamThreadUtils.getVitamSession().getRequestId()));

            try {
                addOntologyFieldsToBeUpdated(parser);
            } catch (AdminManagementClientServerException | InvalidCreateOperationException |
                InvalidParseOperationException e) {
                throw new ProcessingException("Error while adding ontology information", e);
            }

            UpdateMultiQuery multiQuery = parser.getRequest();

            // remove search part (useless)
            multiQuery.resetQueries();

            // set the units to update
            List<String> units = workerParameters.getObjectNameList();
            multiQuery.resetRoots().addRoots(units.stream().toArray(String[]::new));

            // call update BULK service
            RequestResponse<JsonNode> requestResponse = mdClient.updateUnitBulk(multiQuery.getFinalUpdate());

            List<DistributionReportModel> reportModelOK = new ArrayList<>();
            List<DistributionReportModel> reportModelWARN = new ArrayList<>();
            List<DistributionReportModel> reportModelKO = new ArrayList<>();
            if (requestResponse != null && requestResponse.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
                requestResponseOK.getResults().stream().forEach(result -> {
                    final ItemStatus itemStatus = new ItemStatus(MASS_UPDATE_UNITS);
                    String unitId = result.get(ID).asText();
                    DistributionStatus status = DistributionStatus.valueOf(result.get(STATUS).asText());
                    String diff = result.get(DIFF).asText();
                    // TODO : if diff empty => update alerady executed => do not update LFC (and set status warning ??)

                    if (status.equals(DistributionStatus.OK) || status.equals(DistributionStatus.WARNING)) {
                        // write LFC
                        try {
                            writeLfcForUpdateUnit(lfcClient, workerParameters, unitId, diff);
                        } catch (LogbookClientServerException | LogbookClientNotFoundException |
                                InvalidParseOperationException | InvalidGuidOperationException e) {
                            LOGGER.error("Error while updating UNIT LFC ", e);
                            itemStatus.increment(StatusCode.FATAL);
                        } catch (LogbookClientBadRequestException e) {
                            LOGGER.error("Error while updating UNIT LFC ", e);
                            itemStatus.increment(StatusCode.KO);
                        }

                        // store unit & LFC
                        try {
                            saveUnitWithLfc(mdClient, lfcClient, storageClient, handler, workerParameters, unitId,
                                unitId + JSON);
                            itemStatus.increment(StatusCode.OK);
                        } catch (VitamException e) {
                            LOGGER.error("Error while storing UNIT with LFC ", e);

                            // update itemStatus
                            if (e instanceof StorageAlreadyExistsClientException) {
                                itemStatus.increment(StatusCode.KO);
                            } else {
                                itemStatus.increment(StatusCode.FATAL);
                            }

                            // rollback LFC
                            try {
                                lfcClient.rollBackUnitsByOperation(workerParameters.getContainerName());
                            } catch (LogbookClientNotFoundException | LogbookClientBadRequestException | LogbookClientServerException e1) {
                                // Unable to rollback LFC => force FATAL
                                LOGGER.error("Error while storing UNIT with LFC ", e);
                                itemStatus.increment(StatusCode.FATAL);
                            }
                        }
                    }

                    // prepare report part
                    if ("null".equals(diff) && status.equals(DistributionStatus.WARNING)) {
                        itemStatus.increment(StatusCode.WARNING);
                        reportModelWARN
                            .add(new DistributionReportModel(unitId, DistributionStatus.WARNING));
                    } if (itemStatus.getGlobalStatus().equals(StatusCode.OK) ||
                        itemStatus.getGlobalStatus().equals(StatusCode.WARNING)) {
                        reportModelOK
                            .add(new DistributionReportModel(unitId, DistributionStatus.OK));
                    } else {
                        itemStatus.increment(StatusCode.KO);
                        reportModelKO
                            .add(new DistributionReportModel(unitId, DistributionStatus.KO));
                    }

                    // populate itemStatuses 
                    itemStatuses.add(new ItemStatus(MASS_UPDATE_UNITS).setItemsStatus(MASS_UPDATE_UNITS, itemStatus));
                });
            } else {
                throw new ProcessingException("Error when trying to update units.");
            }

            // generate local reports
            final String distribReportsName = handler.getOutput(DISTRIBUTION_LOCAL_REPORTS_RANK).getPath();
            if (CollectionUtils.isNotEmpty(reportModelOK)) {
                storeFileToWorkspace(handler, workerParameters.getProcessId(), reportModelOK, distribReportsName,
                    SUCCESS);
            }
            if (CollectionUtils.isNotEmpty(reportModelWARN)) {
                storeFileToWorkspace(handler, workerParameters.getProcessId(), reportModelWARN, distribReportsName,
                    WARNING);
            }
            if (CollectionUtils.isNotEmpty(reportModelKO)) {
                storeFileToWorkspace(handler, workerParameters.getProcessId(), reportModelKO, distribReportsName,
                    ERRORS);
            }
        } catch (InvalidParseOperationException | MetaDataNotFoundException | MetaDataDocumentSizeException |
            MetaDataClientServerException | MetaDataExecutionException | InvalidCreateOperationException e) {
            // unable to process update for the entire bulk => FATAL
            throw new ProcessingException(e);
        }

        return itemStatuses;
    }

    /**
     * write LFC for update Unit
     *
     * @param lfcClient
     * @param param
     * @param unitId
     * @param diff
     * @throws LogbookClientNotFoundException
     * @throws LogbookClientBadRequestException
     * @throws LogbookClientServerException
     * @throws InvalidParseOperationException
     * @throws InvalidGuidOperationException
     */
    private void writeLfcForUpdateUnit(LogbookLifeCyclesClient lfcClient, WorkerParameters param, String unitId, String diff)
        throws LogbookClientNotFoundException, LogbookClientBadRequestException, LogbookClientServerException,
            InvalidParseOperationException, InvalidGuidOperationException {

        LogbookLifeCycleParameters logbookLfcParam =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters(
                GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                VitamLogbookMessages.getEventTypeLfc(UNIT_METADATA_UPDATE),
                GUIDReader.getGUID(param.getContainerName()),
                param.getLogbookTypeProcess(),
                StatusCode.OK,
                VitamLogbookMessages.getOutcomeDetailLfc(UNIT_METADATA_UPDATE, StatusCode.OK),
                VitamLogbookMessages.getCodeLfc(UNIT_METADATA_UPDATE, StatusCode.OK),
                GUIDReader.getGUID(unitId));
        logbookLfcParam.putParameterValue(LogbookParameterName.eventDetailData, getEvDetDataForDiff(diff));

        lfcClient.update(logbookLfcParam, LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);

    }

    /**
     * getEvDetDataForDiff
     *
     * @param diff
     * @return
     * @throws InvalidParseOperationException
     */
    private String getEvDetDataForDiff(String diff) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }

        ObjectNode diffObject = JsonHandler.createObjectNode();
        diffObject.put("diff", diff);
        return JsonHandler.writeAsString(diffObject);
    }

    /**
     * saveDocumentWithLfcInStorage
     *
     * @param handler
     * @param params
     * @param guid
     * @param fileName
     * @throws VitamException
     */
    private void saveUnitWithLfc(MetaDataClient mdClient, LogbookLifeCyclesClient lfcClient,
        StorageClient storageClient,
        HandlerIO handler, WorkerParameters params, String guid, String fileName) throws VitamException {

        //// get metadata
        JsonNode unit = selectMetadataDocumentRawById(guid, DataCategory.UNIT, mdClient);
        MetadataDocumentHelper.removeComputedFieldsFromUnit(unit);

        //// get lfc
        JsonNode lfc = getRawLogbookLifeCycleById(guid, DataCategory.UNIT, lfcClient);

        //// create file for storage (in workspace or temp or memory)
        JsonNode docWithLfc = MetadataStorageHelper.getUnitWithLFC(unit, lfc);

        // transfer json to workspace
        try {
            InputStream is = CanonicalJsonFormatter.serialize(docWithLfc);
            handler
                .transferInputStreamToWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + fileName, is,
                    null, false);
        } catch (ProcessingException e) {
            LOGGER.error(params.getObjectName(), e);
            throw new WorkspaceClientServerException(e);
        }

        // call storage (save in offers)
        // object Description
        final ObjectDescription description =
            new ObjectDescription(DataCategory.UNIT, params.getContainerName(),
                fileName, IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName);

        // store metadata object from workspace and set itemStatus
        storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY, description.getType(),
            description.getObjectName(),
            description);
    }

    /**
     * Store local reports in workspace
     *
     * @param handler
     * @param processId
     * @param reportModelOK
     * @param containerName
     * @param reportType
     * @throws ProcessingException
     */
    private void storeFileToWorkspace(HandlerIO handler, final String processId,
        List<DistributionReportModel> reportModelOK, String containerName, String reportType)
        throws ProcessingException {
        if (reportModelOK.isEmpty()) {
            return;
        }
        String reportFileName =
            String.format("%s/%s_%s_%s_%s.%s", containerName, processId, handler.getWorkerId(), REPORT,
                reportType,
                EXTENSION);
        final File reportFile = handler.getNewLocalFile(reportFileName);
        try (BufferedWriter bufferedOutput = new BufferedWriter(new FileWriter(reportFile))) {
            bufferedOutput.write(getJsonLineForItem(reportModelOK.get(0)));
            reportModelOK.stream().skip(1).forEach(l -> {
                try {
                    bufferedOutput.write(SEPARTOR + getJsonLineForItem(l));
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            });
        } catch (IOException e) {
            throw new ProcessingException("An exception has been thrown when trying to write file on the workspace",
                e);
        } finally {
            handler.transferFileToWorkspace(reportFileName, reportFile, true, false);
        }
    }



    /**
     * getJsonLineForItem
     *
     * @param item
     * @return
     */
    private String getJsonLineForItem(DistributionReportModel item) {
        ObjectNode itemUnit = createObjectNode();
        itemUnit.put("id", item.getId());
        itemUnit.put("status", String.valueOf(item.getStatus()));
        return JsonHandler.unprettyPrint(itemUnit);
    }

    private void addOntologyFieldsToBeUpdated(UpdateParserMultiple updateParser)
        throws InvalidCreateOperationException, AdminManagementClientServerException,
        InvalidParseOperationException {
        UpdateMultiQuery request = updateParser.getRequest();
        Select selectOntologies = new Select();
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            selectOntologies.setQuery(
                QueryHelper.in(OntologyModel.TAG_COLLECTIONS, MetadataType.UNIT.getName())
            );

            Map<String, Integer> projection = new HashMap<>();
            projection.put(OntologyModel.TAG_IDENTIFIER, 1);
            projection.put(OntologyModel.TAG_TYPE, 1);
            QueryProjection queryProjection = new QueryProjection();
            queryProjection.setFields(projection);
            selectOntologies
                .setProjection(JsonHandler.toJsonNode(queryProjection));
            RequestResponse<OntologyModel> responseOntologies =
                adminClient.findOntologies(selectOntologies.getFinalSelect());
            if (!responseOntologies.isOk() ||
                ((RequestResponseOK<OntologyModel>) responseOntologies).getResults().size() == 0) {
                // no external ontology, nothing to do
                return;
            }

            List<OntologyModel> ontologyModelList =
                ((RequestResponseOK<OntologyModel>) responseOntologies).getResults();

            Action action =
                new SetAction(SchemaValidationUtils.TAG_ONTOLOGY_FIELDS,
                    JsonHandler.unprettyPrint(ontologyModelList));
            request.addActions(action);
        }
    }
}
