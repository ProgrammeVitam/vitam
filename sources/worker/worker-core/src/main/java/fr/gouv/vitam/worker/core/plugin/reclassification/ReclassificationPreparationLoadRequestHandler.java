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
package fr.gouv.vitam.worker.core.plugin.reclassification;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
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
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.reclassification.dsl.ParsedReclassificationDslRequest;
import fr.gouv.vitam.worker.core.plugin.reclassification.dsl.ParsedReclassificationDslRequestEntry;
import fr.gouv.vitam.worker.core.plugin.reclassification.dsl.ReclassificationRequestDslParser;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationEventDetails;
import fr.gouv.vitam.worker.core.plugin.reclassification.model.ReclassificationOrders;
import fr.gouv.vitam.worker.core.plugin.reclassification.utils.UnitGraphInfoLoader;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static org.apache.commons.collections4.SetUtils.union;

/**
 * Reclassification request loading handler.
 */
public class ReclassificationPreparationLoadRequestHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        ReclassificationPreparationLoadRequestHandler.class
    );

    private static final String RECLASSIFICATION_PREPARATION_LOAD_REQUEST = "RECLASSIFICATION_PREPARATION_LOAD_REQUEST";
    private static final int RECLASSIFICATION_ORDERS_PARAMETER_RANK = 0;

    private static final String ACCESS_CONTRACT_ACTIVE_STATUS = "ACTIVE";
    private static final String COULD_NOT_LOAD_REQUEST_FROM_WORKSPACE = "Could not load request from workspace";
    static final String COULD_NOT_PARSE_RECLASSIFICATION_REQUEST = "Could not parse reclassification request";
    static final String NO_ACCESS_CONTRACT_PROVIDED = "No access contract provided";
    static final String ACCESS_CONTRACT_NOT_FOUND_OR_NOT_ACTIVE = "Access contract not found or not active";
    static final String ACCESS_DENIED_OR_MISSING_UNITS = "Access denied or missing units.";
    static final String NO_UNITS_TO_UPDATE = "No units to update.";
    static final String CANNOT_ATTACH_DETACH_SAME_PARENT_UNITS = "Cannot attach & detach same parent units";

    private final int maxBulkThreshold;
    private final int maxUnitsThreshold;
    private final int maxGuildListSizeInLogbookOperation;
    private final AdminManagementClientFactory adminManagementClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final UnitGraphInfoLoader unitGraphInfoLoader;
    private final ReclassificationRequestDslParser reclassificationRequestDslParser;

    /**
     * Default constructor
     */
    public ReclassificationPreparationLoadRequestHandler() {
        this(
            AdminManagementClientFactory.getInstance(),
            MetaDataClientFactory.getInstance(),
            new UnitGraphInfoLoader(),
            new ReclassificationRequestDslParser(),
            VitamConfiguration.getReclassificationMaxBulkThreshold(),
            VitamConfiguration.getReclassificationMaxUnitsThreshold(),
            VitamConfiguration.getReclassificationMaxGuildListSizeInLogbookOperation()
        );
    }

    /***
     * Test only constructor
     */
    @VisibleForTesting
    ReclassificationPreparationLoadRequestHandler(
        AdminManagementClientFactory adminManagementClientFactory,
        MetaDataClientFactory metaDataClientFactory,
        UnitGraphInfoLoader unitGraphInfoLoader,
        ReclassificationRequestDslParser reclassificationRequestDslParser,
        int maxBulkThreshold,
        int maxUnitsThreshold,
        int maxGuildListSizeInLogbookOperation
    ) {
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.metaDataClientFactory = metaDataClientFactory;
        this.unitGraphInfoLoader = unitGraphInfoLoader;
        this.reclassificationRequestDslParser = reclassificationRequestDslParser;
        this.maxBulkThreshold = maxBulkThreshold;
        this.maxUnitsThreshold = maxUnitsThreshold;
        this.maxGuildListSizeInLogbookOperation = maxGuildListSizeInLogbookOperation;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        try {
            // Load request from workspace
            JsonNode reclassificationDslJson = loadReclassificationRequestJsonFromWorkspace(handler);

            // Parse DSL
            ParsedReclassificationDslRequest parsedReclassificationDslRequest = parseReclassificationDslRequest(
                reclassificationDslJson
            );

            // Sanity checks for DSL query
            checkMaxRequestCount(parsedReclassificationDslRequest);

            // Check parent unit existence & accessibility through access contract
            AccessContractModel accessContractModel = getAccessContract();
            checkParentAccessContract(parsedReclassificationDslRequest, accessContractModel);

            // Select units to update
            ReclassificationOrders reclassificationOrders = selectReclassificationOrders(
                parsedReclassificationDslRequest,
                accessContractModel
            );

            // Sanity checks for reclassification orders
            checkMinUnitsToUpdate(reclassificationOrders);
            checkMaxDistinctUnits(reclassificationOrders);
            checkAttachmentAndDetachmentForSameParent(reclassificationOrders);

            // Store request
            storeReclassificationOrders(handler, reclassificationOrders);
        } catch (ProcessingStatusException e) {
            LOGGER.error("Reclassification request loading failed with status [" + e.getStatusCode() + "]", e);
            return buildItemStatus(RECLASSIFICATION_PREPARATION_LOAD_REQUEST, e.getStatusCode(), e.getEventDetails());
        }

        LOGGER.info("Reclassification request loading succeeded");

        return buildItemStatus(RECLASSIFICATION_PREPARATION_LOAD_REQUEST, StatusCode.OK, null);
    }

    private JsonNode loadReclassificationRequestJsonFromWorkspace(HandlerIO handler) throws ProcessingStatusException {
        try {
            return handler.getJsonFromWorkspace("request.json");
        } catch (ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, COULD_NOT_LOAD_REQUEST_FROM_WORKSPACE, e);
        }
    }

    private ParsedReclassificationDslRequest parseReclassificationDslRequest(JsonNode reclassificationDslJson)
        throws ProcessingStatusException {
        ParsedReclassificationDslRequest parsedReclassificationDslRequest;
        try {
            parsedReclassificationDslRequest = reclassificationRequestDslParser.parseReclassificationRequest(
                reclassificationDslJson
            );
        } catch (InvalidParseOperationException e) {
            String error = COULD_NOT_PARSE_RECLASSIFICATION_REQUEST;
            throw new ProcessingStatusException(
                StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error,
                e
            );
        }

        return parsedReclassificationDslRequest;
    }

    private void checkMaxRequestCount(ParsedReclassificationDslRequest parsedReclassificationDslRequest)
        throws ProcessingStatusException {
        if (parsedReclassificationDslRequest.getEntries().size() > maxBulkThreshold) {
            String error = String.format(
                "Too many reclassification requests (count= %d, max= %d)",
                parsedReclassificationDslRequest.getEntries().size(),
                maxBulkThreshold
            );
            throw new ProcessingStatusException(
                StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error
            );
        }
    }

    private AccessContractModel getAccessContract() throws ProcessingStatusException {
        String accessContractId = VitamThreadUtils.getVitamSession().getContractId();
        if (accessContractId == null) {
            throw new ProcessingStatusException(
                StatusCode.KO,
                new ReclassificationEventDetails().setError(NO_ACCESS_CONTRACT_PROVIDED),
                NO_ACCESS_CONTRACT_PROVIDED
            );
        }

        try (AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            Query query = QueryHelper.and()
                .add(
                    QueryHelper.eq(AccessContract.IDENTIFIER, accessContractId),
                    QueryHelper.eq(AccessContract.STATUS, ACCESS_CONTRACT_ACTIVE_STATUS)
                );
            select.setQuery(query);
            JsonNode queryDsl = select.getFinalSelect();

            RequestResponse<AccessContractModel> response = client.findAccessContracts(queryDsl);

            if (!response.isOk() || ((RequestResponseOK<AccessContractModel>) response).getResults().size() == 0) {
                throw new ProcessingStatusException(
                    StatusCode.KO,
                    new ReclassificationEventDetails().setError(ACCESS_CONTRACT_NOT_FOUND_OR_NOT_ACTIVE),
                    String.format("Access contract not found or not active '%s'", accessContractId)
                );
            }

            List<AccessContractModel> contracts = ((RequestResponseOK<AccessContractModel>) response).getResults();
            return contracts.get(0);
        } catch (
            InvalidParseOperationException | InvalidCreateOperationException | AdminManagementClientServerException e
        ) {
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                String.format("An error occurred during access contract loading '%s'", accessContractId),
                e
            );
        }
    }

    private void checkParentAccessContract(
        ParsedReclassificationDslRequest parsedReclassificationDslRequest,
        AccessContractModel accessContractModel
    ) throws ProcessingStatusException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            // Collect all parent unit ids
            Set<String> unitsToCheckIds = new HashSet<>();
            for (ParsedReclassificationDslRequestEntry entry : parsedReclassificationDslRequest.getEntries()) {
                unitsToCheckIds.addAll(entry.getAttachments());
                unitsToCheckIds.addAll(entry.getDetachments());
            }

            // Check unit existence & permissions (via access contract)
            Set<String> foundUnitIds = unitGraphInfoLoader.selectUnitsByIdsAndAccessContract(
                metaDataClient,
                unitsToCheckIds,
                accessContractModel
            );

            if (foundUnitIds.size() == unitsToCheckIds.size()) {
                // all right, all unit ids found
                return;
            }

            // Report failure & log not found ids in evDetData (truncated)
            Set<String> notFoundUnitIds = SetUtils.difference(unitsToCheckIds, foundUnitIds);

            Set<String> firstNotFoundUnitIds = notFoundUnitIds
                .stream()
                .limit(maxGuildListSizeInLogbookOperation)
                .collect(Collectors.toSet());

            String error = ACCESS_DENIED_OR_MISSING_UNITS;

            ReclassificationEventDetails eventDetails = new ReclassificationEventDetails()
                .setError(error)
                .setMissingOrForbiddenUnits(firstNotFoundUnitIds);
            throw new ProcessingStatusException(StatusCode.KO, eventDetails, error);
        } catch (
            InvalidCreateOperationException
            | InvalidParseOperationException
            | VitamDBException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not check unit ids", e);
        }
    }

    private ReclassificationOrders selectReclassificationOrders(
        ParsedReclassificationDslRequest parsedReclassificationDslRequest,
        AccessContractModel accessContractModel
    ) throws ProcessingStatusException {
        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            HashSetValuedHashMap<String, String> childToParentAttachments = new HashSetValuedHashMap<>();
            HashSetValuedHashMap<String, String> childToParentDetachments = new HashSetValuedHashMap<>();

            for (ParsedReclassificationDslRequestEntry entry : parsedReclassificationDslRequest.getEntries()) {
                Set<String> childIds = unitGraphInfoLoader.selectUnitsByQueryDslAndAccessContract(
                    metaDataClient,
                    entry.getSelectMultiQuery(),
                    accessContractModel
                );

                for (String childId : childIds) {
                    childToParentAttachments.putAll(childId, entry.getAttachments());
                    childToParentDetachments.putAll(childId, entry.getDetachments());
                }
            }

            return new ReclassificationOrders(childToParentAttachments, childToParentDetachments);
        } catch (InvalidParseOperationException | InvalidCreateOperationException e) {
            String error = "Could not parse reclassification request";
            throw new ProcessingStatusException(
                StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error,
                e
            );
        } catch (
            VitamDBException
            | MetaDataExecutionException
            | MetaDataDocumentSizeException
            | MetaDataClientServerException e
        ) {
            String error = "Could not select units to update";
            throw new ProcessingStatusException(
                StatusCode.FATAL,
                new ReclassificationEventDetails().setError(error),
                error,
                e
            );
        }
    }

    private void checkMinUnitsToUpdate(ReclassificationOrders reclassificationOrders) throws ProcessingStatusException {
        if (
            reclassificationOrders.getChildToParentAttachments().isEmpty() &&
            reclassificationOrders.getChildToParentDetachments().isEmpty()
        ) {
            String error = NO_UNITS_TO_UPDATE;
            throw new ProcessingStatusException(
                StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error
            );
        }
    }

    private void checkMaxDistinctUnits(ReclassificationOrders reclassificationUpdates)
        throws ProcessingStatusException {
        Set<String> childUnitIds = union(
            reclassificationUpdates.getChildToParentAttachments().keySet(),
            reclassificationUpdates.getChildToParentDetachments().keySet()
        );

        if (childUnitIds.size() > maxUnitsThreshold) {
            String error = String.format(
                "Too many units in reclassification request (count= %d, max=%d)",
                childUnitIds.size(),
                maxUnitsThreshold
            );
            throw new ProcessingStatusException(
                StatusCode.KO,
                new ReclassificationEventDetails().setError(error),
                error
            );
        }
    }

    private void checkAttachmentAndDetachmentForSameParent(ReclassificationOrders reclassificationOrders)
        throws ProcessingStatusException {
        Set<String> unitsWithBothAttachmentsAndDetachments = SetUtils.intersection(
            reclassificationOrders.getChildToParentAttachments().keySet(),
            reclassificationOrders.getChildToParentDetachments().keySet()
        );

        for (String unitId : unitsWithBothAttachmentsAndDetachments) {
            Set<String> attachments = reclassificationOrders.getChildToParentAttachments().get(unitId);
            Set<String> detachments = reclassificationOrders.getChildToParentDetachments().get(unitId);

            SetUtils.SetView<String> duplicateIds = SetUtils.intersection(attachments, detachments);

            if (!duplicateIds.isEmpty()) {
                String error = CANNOT_ATTACH_DETACH_SAME_PARENT_UNITS;
                throw new ProcessingStatusException(
                    StatusCode.KO,
                    new ReclassificationEventDetails().setError(error),
                    error
                );
            }
        }
    }

    private void storeReclassificationOrders(HandlerIO handler, ReclassificationOrders reclassificationOrders)
        throws ProcessingStatusException {
        try {
            handler.addOutputResult(RECLASSIFICATION_ORDERS_PARAMETER_RANK, reclassificationOrders, false);
        } catch (ProcessingException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not store reclassification orders", e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // NOP.
    }

    public static String getId() {
        return RECLASSIFICATION_PREPARATION_LOAD_REQUEST;
    }
}
