/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.processing.ProcessDetail;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.service.ChainedFileWriter;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.collections4.SetUtils;
import org.apache.shiro.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.plugin.reclassification.ReclassificationPluginHelper.buildItemStatus;
import static org.apache.commons.collections4.SetUtils.intersection;

/**
 * Unit reclassification preparation plugin.
 *
 * This plugin handles request validation and reclassification preparation (request validation, cycle check...)
 */

public class ReclassificationPreparationPlugin extends ActionHandler {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(ReclassificationPreparationPlugin.class);

    private static final String RECLASSIFICATION_PREPARATION = "RECLASSIFICATION_PREPARATION";

    private static final String UNITS_TO_DETACH_DIR = "UnitsToDetach";
    private static final String UNITS_TO_ATTACH_DIR = "UnitsToAttach";
    private static final String UNITS_TO_UPDATE_DIR = "UnitsToUpdate";
    private static final String OG_TO_UPDATE_DIR = "ObjectGroupsToUpdate";
    private static final String ACCESS_CONTRACT_ACTIVE_STATUS = "ACTIVE";
    static final String RECLASSIFICATION_WORKFLOW_IDENTIFIER = "RECLASSIFICATION";
    static final String CONCURRENT_RECLASSIFICATION_PROCESS = "Concurrent reclassification process(es) found";
    static final String INVALID_JSON = "Invalid Json";
    static final String NO_ACCESS_CONTRACT_PROVIDED = "No access contract provided";
    static final String ACCESS_CONTRACT_NOT_FOUND_OR_NOT_ACTIVE = "Access contract not found or not active";
    static final String ACCESS_DENIED_OR_MISSING_UNITS = "Access denied or missing units.";

    // FIXME (use conf)
    private static final int MAX_BULK_THRESHOLD = 1000;
    private static final int MAX_UNITS_THRESHOLD = 10000;
    private static final int MAX_GUID_LIST_SIZE_IN_LOGBOOK_OPERATION = 1000;
    private static final int CHAINED_FILE_BATCH_SIZE = 10000;

    private final int maxBulkThreshold;
    private final int maxUnitsThreshold;
    private final int maxGuildListSizeInLogbookOperation;
    private final int chainedFileBatchSize;
    private final AdminManagementClientFactory adminManagementClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final LightweightWorkflowLock lightweightWorkflowLock;
    private final UnitGraphInfoLoader unitGraphInfoLoader;

    /**
     * Default constructor
     */
    public ReclassificationPreparationPlugin() {
        // FIXME (use conf)
        this(
            AdminManagementClientFactory.getInstance(),
            MetaDataClientFactory.getInstance(),
            new UnitGraphInfoLoader(),
            new LightweightWorkflowLock(),
            MAX_BULK_THRESHOLD,
            MAX_UNITS_THRESHOLD,
            MAX_GUID_LIST_SIZE_IN_LOGBOOK_OPERATION,
            CHAINED_FILE_BATCH_SIZE);
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    public ReclassificationPreparationPlugin(AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory, UnitGraphInfoLoader unitGraphInfoLoader,
        LightweightWorkflowLock lightweightWorkflowLock, int maxBulkThreshold,
        int maxUnitsThreshold, int maxGuildListSizeInLogbookOperation, int chainedFileBatchSize) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.unitGraphInfoLoader = unitGraphInfoLoader;
        this.lightweightWorkflowLock = lightweightWorkflowLock;
        this.maxBulkThreshold = maxBulkThreshold;
        this.maxUnitsThreshold = maxUnitsThreshold;
        this.maxGuildListSizeInLogbookOperation = maxGuildListSizeInLogbookOperation;
        this.chainedFileBatchSize = chainedFileBatchSize;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
        throws ProcessingException, ContentAddressableStorageServerException {

        try {

            // Check no concurrent reclassification workflows are active.
            checkConcurrentReclassificationWorkflows(param.getContainerName());

            // Load / parse request
            ReclassificationRequest reclassificationRequest = loadReclassficiationRequest(handler);

            // Validate request
            validateRequest(reclassificationRequest);

            // Check unit existence / accessibility via AccessContract
            checkAccessControl(reclassificationRequest);

            // Check graph (check unit types & graph cycles)
            checkGraphCoherence(reclassificationRequest);

            // Prepare distributions
            prepareUpdates(reclassificationRequest, handler);

        } catch (ReclassificationException e) {
            LOGGER.error("Unit attachment failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(RECLASSIFICATION_PREPARATION, e.getStatusCode(), e.getEventDetails());
        }

        LOGGER.info("Reclassification preparation succeeded");

        return buildItemStatus(RECLASSIFICATION_PREPARATION, StatusCode.OK, null);
    }

    private void checkConcurrentReclassificationWorkflows(String processId) throws ReclassificationException {

        try {

            List<ProcessDetail> concurrentReclassificationWorkflows = lightweightWorkflowLock
                .listConcurrentReclassificationWorkflows(RECLASSIFICATION_WORKFLOW_IDENTIFIER, processId);

            if (!concurrentReclassificationWorkflows.isEmpty()) {
                throw new ReclassificationException(
                    StatusCode.KO,
                    new ReclassificationEventDetails().setError(CONCURRENT_RECLASSIFICATION_PROCESS),
                    "Concurrent reclassification process(es) found " +
                        concurrentReclassificationWorkflows.stream().map(
                            i -> i.getOperationId() + "(" + i.getGlobalState() + "/" + i.getStepStatus() + ")")
                            .collect(Collectors.joining(", ", "[", "]")));
            }

        } catch (VitamClientException e) {
            throw new ReclassificationException(StatusCode.FATAL,
                "An error occurred during reclassification process listing", e);
        }
    }

    private ReclassificationRequest loadReclassficiationRequest(HandlerIO handler)
        throws ReclassificationException {

        // Load / parse reclassification request
        JsonNode reclassificationRequestJson = loadReclassificationRequestJson(handler);
        ReclassificationRequest reclassificationRequest = parseReclassificationRequest(reclassificationRequestJson);

        return reclassificationRequest;
    }

    private JsonNode loadReclassificationRequestJson(HandlerIO handler) throws ReclassificationException {

        try (InputStream inputStreamFromWorkspace = handler.getInputStreamFromWorkspace("request.json")) {
            JsonNode reclassificationRequestJson =
                JsonHandler.getFromInputStream(inputStreamFromWorkspace);
            return reclassificationRequestJson;
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException |
            IOException e) {
            throw new ReclassificationException(StatusCode.FATAL, "Could not load request Json", e);
        } catch (InvalidParseOperationException e) {
            String error = INVALID_JSON;
            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails().setError(error);
            throw new ReclassificationException(StatusCode.KO, eventDetails, error, e);
        }
    }

    private ReclassificationRequest parseReclassificationRequest(JsonNode reclassificationRequestJson)
        throws ReclassificationException {
        // FIXME (use DSL?)
        ReclassificationRequest reclassificationRequest;
        try {
            reclassificationRequest =
                JsonHandler.getFromJsonNode(reclassificationRequestJson, ReclassificationRequest.class);
        } catch (InvalidParseOperationException e) {
            String error = "Could not parse reclassification request";
            throw new ReclassificationException(StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error);
        }
        return reclassificationRequest;
    }

    private void validateRequest(ReclassificationRequest reclassificationRequest) throws ReclassificationException {

        checkMaxReclassificationUpdateCount(reclassificationRequest);

        checkMaxDistinctUnits(reclassificationRequest);

        checkAttachmentAndDetachmentForSameParent(reclassificationRequest);
    }

    private void checkMaxReclassificationUpdateCount(ReclassificationRequest reclassificationRequest)
        throws ReclassificationException {

        if (CollectionUtils.isEmpty(reclassificationRequest.getUpdatesByUnit())) {
            throw new ReclassificationException(StatusCode.KO, "Empty reclassification request");
        }

        if (reclassificationRequest.getUpdatesByUnit().size() > maxBulkThreshold) {
            String error = String.format("Too many reclassification orders (provided= %d, max=%d)",
                reclassificationRequest.getUpdatesByUnit().size(), maxBulkThreshold);
            throw new ReclassificationException(StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error);
        }
    }

    private void checkMaxDistinctUnits(ReclassificationRequest reclassificationRequest)
        throws ReclassificationException {

        Set<String> distinctUnitIds = getAllUnitIds(reclassificationRequest);
        if (distinctUnitIds.size() > maxUnitsThreshold) {
            String error = String.format(
                "Too many units in reclassification request (count= %d, max=%d)",
                distinctUnitIds.size(), maxUnitsThreshold);
            throw new ReclassificationException(StatusCode.KO, new ReclassificationEventDetails().setError(error),
                error);
        }
    }

    private void checkAttachmentAndDetachmentForSameParent(ReclassificationRequest reclassificationRequest)
        throws ReclassificationException {
        for (Map.Entry<String, ReclassificationUpdates> entry : reclassificationRequest.getUpdatesByUnit().entrySet()) {
            SetUtils.SetView<String> duplicateIds =
                intersection(entry.getValue().getAttachments(), entry.getValue().getDetachments());
            if (!duplicateIds.isEmpty()) {
                throw new ReclassificationException(StatusCode.KO,
                    new ReclassificationEventDetails().setError("Cannot attach & detach same parent units"),
                    "Cannot attach & detach same parent units. Child unit id" + entry.getKey());
            }
        }
    }

    private Set<String> getAllUnitIds(ReclassificationRequest reclassificationRequest) {
        Set<String> result = new HashSet<>();
        result.addAll(reclassificationRequest.getUpdatesByUnit().keySet());
        for (ReclassificationUpdates updates : reclassificationRequest.getUpdatesByUnit().values()) {
            result.addAll(updates.getAttachments());
            result.addAll(updates.getDetachments());
        }
        return result;
    }

    private void checkAccessControl(ReclassificationRequest reclassificationRequest) throws ReclassificationException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            // Check unit existence & permissions (via access contract)
            Set<String> unitsToCheckIds = getAllUnitIds(reclassificationRequest);

            AccessContractModel accessContractModel = getAccessContract();

            Set<String> foundUnitIds = unitGraphInfoLoader.selectUnitsByIdsAndAccessContract(metaDataClient,
                unitsToCheckIds, accessContractModel);

            if (foundUnitIds.size() == unitsToCheckIds.size()) {
                // all right, all unit ids found
                return;
            }

            // Report failure & log first not found ids in evDetData
            Set<String> notFoundUnitIds = unitsToCheckIds.stream().filter(id -> !foundUnitIds.contains(id)).collect(
                Collectors.toSet());

            Set<String> firstNotFoundUnitIds =
                notFoundUnitIds.stream().limit(maxGuildListSizeInLogbookOperation).collect(Collectors.toSet());

            String error = ACCESS_DENIED_OR_MISSING_UNITS;

            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails().setError(error)
                .setMissingOrForbiddenUnits(firstNotFoundUnitIds);
            throw new ReclassificationException(StatusCode.KO, eventDetails, error);

        } catch (InvalidCreateOperationException | InvalidParseOperationException | VitamDBException
            | MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new ReclassificationException(StatusCode.FATAL, "Could not check unit ids", e);
        }
    }

    private AccessContractModel getAccessContract()
        throws ReclassificationException {

        String accessContractId = VitamThreadUtils.getVitamSession().getContractId();
        if (accessContractId == null) {
            throw new ReclassificationException(StatusCode.KO,
                new ReclassificationEventDetails().setError(NO_ACCESS_CONTRACT_PROVIDED),
                NO_ACCESS_CONTRACT_PROVIDED);
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {

            Select select = new Select();
            Query query = QueryHelper.and().add(QueryHelper.eq(AccessContract.IDENTIFIER, accessContractId),
                QueryHelper.eq(AccessContract.STATUS, ACCESS_CONTRACT_ACTIVE_STATUS));
            select.setQuery(query);
            JsonNode queryDsl = select.getFinalSelect();

            RequestResponse<AccessContractModel> response = client.findAccessContracts(queryDsl);

            if (!response.isOk() || ((RequestResponseOK<AccessContractModel>) response).getResults().size() == 0) {
                throw new ReclassificationException(StatusCode.KO,
                    new ReclassificationEventDetails().setError(ACCESS_CONTRACT_NOT_FOUND_OR_NOT_ACTIVE),
                    String.format("Access contract not found or not active '%s'", accessContractId));
            }

            List<AccessContractModel> contracts = ((RequestResponseOK<AccessContractModel>) response).getResults();
            return contracts.get(0);
        } catch (InvalidParseOperationException | InvalidCreateOperationException | AdminManagementClientServerException e) {
            throw new ReclassificationException(StatusCode.FATAL,
                String.format("An error occurred during access contract loading '%s'", accessContractId));
        }
    }

    private void checkGraphCoherence(ReclassificationRequest reclassificationRequest) throws ReclassificationException {

        // Load all units & their parents recursively
        Map<String, UnitGraphInfo> unitGraphByIds = loadAllUnitGraphByIds(reclassificationRequest);

        // Check unit type coherence
        checkAttachmentUnitTypeCoherence(reclassificationRequest, unitGraphByIds);

        // Check cycles
        checkCycles(reclassificationRequest, unitGraphByIds);

    }

    private Map<String, UnitGraphInfo> loadAllUnitGraphByIds(ReclassificationRequest reclassificationRequest)
        throws ReclassificationException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            Set<String> allUnitIds = getAllUnitIds(reclassificationRequest);
            Map<String, UnitGraphInfo> result = unitGraphInfoLoader.selectAllUnitGraphByIds(metaDataClient, allUnitIds);


            if (result.size() < allUnitIds.size()) {
                Set<String> firstNotFoundUnits =
                    allUnitIds.stream()
                        .filter(id -> !result.containsKey(id))
                        .limit(maxGuildListSizeInLogbookOperation)
                        .collect(Collectors.toSet());

                String error = "Could not load units";
                ReclassificationEventDetails eventDetails = new ReclassificationEventDetails().setError(error)
                    .setNotFoundUnits(firstNotFoundUnits);
                throw new ReclassificationException(StatusCode.FATAL, eventDetails, error);
            }

            return result;

        } catch (InvalidCreateOperationException | InvalidParseOperationException | MetaDataExecutionException
            | VitamDBException | MetaDataDocumentSizeException | MetaDataClientServerException e) {
            throw new ReclassificationException(StatusCode.FATAL, "Could not load unit graph information", e);
        }
    }

    private void checkAttachmentUnitTypeCoherence(
        ReclassificationRequest reclassificationRequest,
        Map<String, UnitGraphInfo> unitGraphByIds) throws ReclassificationException {

        List<IllegalUnitTypeAttachment> illegalUnitTypeAttachments = new ArrayList<>();

        for (Map.Entry<String, ReclassificationUpdates> entry : reclassificationRequest.getUpdatesByUnit().entrySet()) {

            String childUnitId = entry.getKey();
            UnitType childUnitType = unitGraphByIds.get(childUnitId).getUnitType();

            for (String parentUnitId : entry.getValue().getAttachments()) {
                UnitType parentUnitType = unitGraphByIds.get(parentUnitId).getUnitType();

                if (childUnitType.ordinal() < parentUnitType.ordinal()) {
                    illegalUnitTypeAttachments.add(
                        new IllegalUnitTypeAttachment(childUnitId, childUnitType, parentUnitId, parentUnitType));
                }
            }
        }

        if (!illegalUnitTypeAttachments.isEmpty()) {
            String error = "Invalid unit type attachment(s)";

            List<IllegalUnitTypeAttachment> firstIllegalUnitTypeAttachments =
                illegalUnitTypeAttachments.stream().limit(maxGuildListSizeInLogbookOperation)
                    .collect(Collectors.toList());

            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails().setError(error)
                .setIllegalUnitTypeAttachments(firstIllegalUnitTypeAttachments);
            throw new ReclassificationException(StatusCode.KO, eventDetails, error);
        }
    }

    private void checkCycles(ReclassificationRequest reclassificationRequest,
        Map<String, UnitGraphInfo> unitGraphByIds) throws ReclassificationException {

        GraphCycleDetector graphCycleDetector = new GraphCycleDetector();

        for (UnitGraphInfo unitGraph : unitGraphByIds.values()) {
            String unitId = unitGraph.getId();

            // Add current graph parents
            graphCycleDetector.addRelations(unitId, unitGraph.getUp());

            // Add attachments / detachments
            if (reclassificationRequest.getUpdatesByUnit().containsKey(unitId)) {
                ReclassificationUpdates reclassificationUpdates =
                    reclassificationRequest.getUpdatesByUnit().get(unitId);
                graphCycleDetector.addRelations(unitGraph.getId(), reclassificationUpdates.getAttachments());
                graphCycleDetector.removeRelations(unitGraph.getId(), reclassificationUpdates.getDetachments());
            }
        }

        Set<String> graphCycles = graphCycleDetector.checkCycles();

        if (!graphCycles.isEmpty()) {

            Set<String> firstGraphCycles =
                graphCycles.stream().limit(maxGuildListSizeInLogbookOperation)
                    .collect(Collectors.toSet());

            String error = "Cannot apply reclassification request. Cycle detected";
            ReclassificationEventDetails eventDetails =
                new ReclassificationEventDetails().setError(error)
                    .setUnitsWithCycles(firstGraphCycles);

            throw new ReclassificationException(StatusCode.KO, eventDetails, error);
        }
    }

    private void prepareUpdates(ReclassificationRequest reclassificationRequest,
        HandlerIO handler) throws ReclassificationException {

        prepareDetachments(reclassificationRequest, handler);

        prepareAttachments(reclassificationRequest, handler);

        prepareUnitAndObjectGroupGraphUpdates(reclassificationRequest, handler);

    }

    private void prepareDetachments(ReclassificationRequest reclassificationRequest, HandlerIO handler)
        throws ReclassificationException {
        for (Map.Entry<String, ReclassificationUpdates> entry : reclassificationRequest
            .getUpdatesByUnit().entrySet()) {

            if (!CollectionUtils.isEmpty(entry.getValue().getDetachments())) {
                storeToWorkspace(handler, entry.getValue().getDetachments(),
                    UNITS_TO_DETACH_DIR + "/" + entry.getKey());
            }
        }
    }

    private void prepareAttachments(ReclassificationRequest reclassificationRequest, HandlerIO handler)
        throws ReclassificationException {
        for (Map.Entry<String, ReclassificationUpdates> entry : reclassificationRequest
            .getUpdatesByUnit().entrySet()) {

            if (!CollectionUtils.isEmpty(entry.getValue().getAttachments())) {
                storeToWorkspace(handler, entry.getValue().getAttachments(),
                    UNITS_TO_ATTACH_DIR + "/" + entry.getKey());
            }
        }
    }

    private void prepareUnitAndObjectGroupGraphUpdates(ReclassificationRequest reclassificationRequest,
        HandlerIO handler)
        throws ReclassificationException {

        try (
            ChainedFileWriter unitChainedFileWriter = new ChainedFileWriter(handler,
                UNITS_TO_UPDATE_DIR + "/chainedFile.json",
                chainedFileBatchSize);
            ChainedFileWriter objectGroupChainedFileWriter = new ChainedFileWriter(handler,
                OG_TO_UPDATE_DIR + "/chainedFile.json",
                chainedFileBatchSize);
            MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            String[] unitIds = reclassificationRequest.getUpdatesByUnit().keySet().toArray(new String[0]);

            Iterator<JsonNode> iterator = unitGraphInfoLoader.selectAllUnitsToUpdate(metaDataClient, unitIds);

            while (iterator.hasNext()) {
                JsonNode item = iterator.next();
                unitChainedFileWriter.addEntry(item.get(VitamFieldsHelper.id()).asText());
                if (item.get(VitamFieldsHelper.object()) != null) {
                    objectGroupChainedFileWriter.addEntry(item.get(VitamFieldsHelper.object()).asText());
                }
            }

        } catch (ProcessingException | InvalidParseOperationException | InvalidCreateOperationException e) {
            throw new ReclassificationException(StatusCode.FATAL,
                "Could not export the list of units and object groups to update", e);
        }
    }



    private void storeToWorkspace(HandlerIO handler, Object data, String filePath) throws ReclassificationException {
        try (InputStream inputStream = JsonHandler.writeToInpustream(data)) {
            handler.transferInputStreamToWorkspace(filePath, inputStream, null, false);
        } catch (InvalidParseOperationException | IOException | ProcessingException e) {
            throw new ReclassificationException(StatusCode.FATAL, "Could not store to workspace: " + filePath, e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }
}
