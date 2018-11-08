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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.core.plugin.CreateSecureFileActionPlugin;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceAuditException;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.json.JsonHandler.getFromJsonNode;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.eventIdentifier;
import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.rightsStatementIdentifier;
import static fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus.FATAL;
import static fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus.KO;
import static fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus.OK;

/**
 * ProbativeService class
 */
public class ProbativeService {

    private static final String NO_OPERATION_FOUND_MATCHING_ID =
        "No traceability operation found matching the id ";

    private static final String FILE_NAME = "FileName";
    public static final String JSON = ".json";
    private static final String LAST_VERSION = "LAST";
    public static final String ID = "_id";
    public static final String EVENTS = "events";
    public static final String CHECK_LFC_STORAGE_EVENT = "checkLfcStorageEvent";

    private MetaDataClientFactory metaDataClientFactory;
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private StorageClientFactory storageClientFactory;
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeService.class);

    @VisibleForTesting
    public ProbativeService(MetaDataClientFactory metaDataClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        StorageClientFactory storageClientFactory) {

        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    ProbativeService() {
        this(MetaDataClientFactory.getInstance(), LogbookOperationsClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(), StorageClientFactory.getInstance());
    }


    ProbativeParameter probativeValueChecks(String objectGroupId, List<String> usages, String usageVersion) {

        ProbativeParameter parameter = new ProbativeParameter();
        try {

            JsonNode metadata = getRawObjectGroupMetadata(objectGroupId);

            parameter.setId(objectGroupId);

            JsonNode lifecycle = getRawObjectGroupLifeCycle(objectGroupId);

            for (String usage : usages) {

                Optional<ProbativeUsageParameter> usageInfo = extractQualifierVersion(metadata, usage, usageVersion);

                if (usageInfo.isPresent()) {

                    ProbativeUsageParameter usageParam = usageInfo.get();
                    getLogbookOperationInfo(usageParam);

                    checkStorageHash(usageParam);

                    checkStorageEvent(usageParam, lifecycle);

                    String hashEventsBeforeDate =
                        getHashEventsBeforeDate(usageParam.getStorageLogbookEvent().getLastPersistedDate(), lifecycle);

                    usageParam.setHashEvents(hashEventsBeforeDate);

                    checkStorageLogbookOperationInfo(usageParam);

                    loadFileInfoFromLogbooks(usageParam);

                    parameter.getUsageParameters().put(usage, usageParam);
                }
            }

            parameter.setEvidenceStatus(OK);

        } catch (InvalidParseOperationException | EvidenceAuditException | IOException e) {
            LOGGER.error(e);

            parameter.setEvidenceStatus(KO);
        }

        return parameter;

    }

    void getLogbookOperationInfo(ProbativeUsageParameter parameter) throws EvidenceAuditException {


        String checkName = "checkLogbookSecureInfoForOpi";

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            JsonNode node = client.selectOperationById(parameter.getVersionsModel().getOpi());

            JsonNode logbook = node.get(TAG_RESULTS).get(0);

            JsonNode result = createLastSecureObjectGroupSelect(logbook.get("_lastPersistedDate").textValue(),
                Contexts.LOGBOOK_TRACEABILITY.getEventType());

            String mostRecentLogbookSecureOperationId =
                result.get(TAG_RESULTS).get(0).get(eventIdentifier.getDbname()).asText();


            result = client.selectOperationById(mostRecentLogbookSecureOperationId);

            String detailData =
                result.get(TAG_RESULTS).get(0).get(LogbookMongoDbName.eventDetailData.getDbname()).asText();

            JsonNode nodeEvDetData = JsonHandler.getFromString(detailData);

            parameter.setSecureOperationIdForOpId(mostRecentLogbookSecureOperationId);


            parameter.setLogBookSecuredOpiFileName(nodeEvDetData.get(FILE_NAME).asText());

            parameter.getVersionLogbook().put(parameter.getVersionsModel().getOpi(), result.get(TAG_RESULTS).get(0));

            reportCheckOk(parameter, checkName);


        } catch (InvalidParseOperationException | LogbookClientException | EvidenceAuditException e) {
            LOGGER.error(e);
            reportCheckKo(parameter, checkName, e.getMessage());
            throw new EvidenceAuditException(EvidenceStatus.KO, e.getMessage());
        }
    }

    void checkStorageLogbookOperationInfo(ProbativeUsageParameter parameter) {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            JsonNode result = client.selectOperationById(parameter.getVersionsModel().getOpi());

            JsonNode logbookNode =
                result.get(TAG_RESULTS).get(0);

            parameter.setAgIdApp(logbookNode.get("agIdApp").textValue());

            parameter.setEvIdAppSession(logbookNode.get("evIdAppSession").textValue());

            parameter.setArchivalAgreement(
                JsonHandler.getFromString(logbookNode.get(rightsStatementIdentifier.getDbname()).textValue())
                    .get("ArchivalAgreement").textValue());

            reportCheckOk(parameter, "checkLogbookStorageEventContract");

        } catch (InvalidParseOperationException | LogbookClientException e) {
            LOGGER.error(e);

            reportCheckKo(parameter, "checkLogbookStorageEvent", "Could not retrieve logbook operation information");

        }
    }


    private JsonNode getRawObjectGroupMetadata(String objectGroupId) throws EvidenceAuditException {

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {

            RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupByIdRaw(objectGroupId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            }

            throw new EvidenceAuditException(KO,
                String.format("No such object group metadata '%s'", objectGroupId));

        } catch (VitamClientException e) {

            throw new EvidenceAuditException(FATAL,
                "An error occurred during object group metadata retrieval",
                e);
        }
    }

    private JsonNode getRawObjectGroupLifeCycle(String objectGroupId) throws EvidenceAuditException {

        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {

            return logbookLifeCyclesClient.getRawObjectGroupLifeCycleById(objectGroupId);

        } catch (LogbookClientNotFoundException e) {
            throw new EvidenceAuditException(KO,
                String.format("No such lifecycle object group found '%s'", objectGroupId), e);
        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(FATAL,
                "An error occurred during object group lifecycle retrieval",
                e);
        }
    }

    private JsonNode createLastSecureObjectGroupSelect(String lastPersistedDate, String eventType)
        throws EvidenceAuditException {
        try {

            Select select = new Select();
            BooleanQuery query = and().add(
                eq(LogbookMongoDbName.eventType.getDbname(), eventType),
                eq("events.outDetail", eventType + ".OK"),
                QueryHelper.lte("events.evDetData.StartDate", lastPersistedDate),
                gte("events.evDetData.EndDate", lastPersistedDate)
            );

            select.setQuery(query);
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter("events.evDateTime");

            return logbookOperationsClientFactory.getClient().selectOperation(select.getFinalSelect());
        } catch (Exception e) {
            throw new EvidenceAuditException(KO,
                NO_OPERATION_FOUND_MATCHING_ID + lastPersistedDate, e);
        }
    }

    String getHashEventsBeforeDate(String lastPersitedDate, JsonNode lfc) throws IOException {

        LocalDateTime parsedLastPersistedDate = LocalDateTime.parse(lastPersitedDate);

        ArrayNode events = (ArrayNode) lfc.get(EVENTS);
        ArrayNode target = JsonHandler.createArrayNode();

        for (JsonNode node : events) {
            LocalDateTime date = LocalDateTime.parse(node.get("_lastPersistedDate").textValue());

            if (date.isBefore(parsedLastPersistedDate) || date.isEqual(parsedLastPersistedDate)) {
                target.add(node);
            }
        }
        return CreateSecureFileActionPlugin.generateDigest(target, VitamConfiguration.getDefaultDigestType());

    }

    void checkStorageEvent(ProbativeUsageParameter parameter, JsonNode lfc)
        throws InvalidParseOperationException {

        ArrayList<LogbookEvent> eventsLists = new ArrayList<>();

        if (lfc.get(EVENTS) == null || !lfc.get(EVENTS).isArray()) {
            reportCheckKo(parameter, CHECK_LFC_STORAGE_EVENT, "No lfc Events found");
            return;
        }

        Iterator<JsonNode> events = lfc.get(EVENTS).elements();

        while (events.hasNext()) {

            JsonNode next = events.next();

            LogbookEvent logbookEvent = getFromJsonNode(next, LogbookEvent.class);

            boolean isStorageEvent = logbookEvent.getOutDetail().startsWith("LFC.OBJ_STORAGE.")
                &&
                logbookEvent.getEvIdProc().equals(parameter.getVersionsModel().getOpi())
                &&
                logbookEvent.getEvTypeProc().equals(LogbookTypeProcess.INGEST.name())
                &&
                logbookEvent.getObId().equals(parameter.getVersionsModel().getId());

            if (isStorageEvent) {

                eventsLists.add(logbookEvent);
            }
        }

        if (eventsLists.size() > 1) {
            reportCheckKo(parameter, CHECK_LFC_STORAGE_EVENT,
                String.format("More than one storage events: '%s '", unprettyPrint(events)));
            return;
        }

        parameter.setStorageLogbookEvent(eventsLists.get(0));

        reportCheckOk(parameter, CHECK_LFC_STORAGE_EVENT);
    }

    void checkStorageHash(ProbativeUsageParameter parameter) {

        String checkName = "CheckObjectHash";
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            VersionsModel versionsModel = parameter.getVersionsModel();

            JsonNode information = storageClient.
                getInformation("default",
                    DataCategory.OBJECT, versionsModel.getId(), versionsModel.getStorage().getOfferIds(), true);
            List<String> errorDetails = new ArrayList<>();
            for (String offerId : versionsModel.getStorage().getOfferIds()) {

                verifyOfferHashes(versionsModel, information, errorDetails, offerId);
            }
            if (errorDetails.isEmpty()) {

                reportCheckOk(parameter, checkName);
                return;
            }
            reportCheckKo(parameter, checkName, String.join(", ", errorDetails));

        } catch (StorageNotFoundClientException | StorageServerClientException e) {
            LOGGER.error(e);
            reportCheckKo(parameter, checkName, String.format("Error when getting Storage info %s ", e.getMessage()));
        }
    }


    private void loadFileInfoFromLogbooks(ProbativeUsageParameter parameter)
        throws EvidenceAuditException {

        String checkName = "Checking secured info from logbook";

        String lastPersistedDate = parameter.getStorageLogbookEvent().getLastPersistedDate();

        JsonNode result = createLastSecureObjectGroupSelect(lastPersistedDate,
            Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType());

        String mostRecentLogbookSecureOperationId =
            result.get(TAG_RESULTS).get(0).get(eventIdentifier.getDbname()).asText();

        parameter.setSecuredOperationId(mostRecentLogbookSecureOperationId);

        reportCheckOk(parameter, checkName);

    }

    private void verifyOfferHashes(VersionsModel versionsModel, JsonNode information, List<String> errorDetails,
        String offerId) {

        String offerHash = null;
        boolean offerOk;
        offerOk = information.get(offerId) != null;
        offerOk = offerOk && information.get(offerId).get("digest") != null;

        if (offerOk) {
            offerHash = information.get(offerId).get("digest").textValue();
        }

        if (!offerOk || !offerHash.equals(versionsModel.getMessageDigest())) {
            errorDetails.add(String
                .format("Hash : '%s' for offerId : %s is not equal of to %s", offerHash, offerId,
                    versionsModel.getMessageDigest()));

        }
    }



    Optional<ProbativeUsageParameter> extractQualifierVersion(JsonNode metadata, String usage, String usageVersion)
        throws InvalidParseOperationException {

        ProbativeUsageParameter probativeUsageParameter = new ProbativeUsageParameter(usage);

        JsonNode qualifiers = metadata.get(SedaConstants.PREFIX_QUALIFIERS);

        if (qualifiers == null || !qualifiers.isArray() || qualifiers.size() == 0) {
            throw new IllegalStateException("Metadata is not valid");
        }

        Iterator<JsonNode> elements = qualifiers.elements();
        Optional<VersionsModel> model = Optional.empty();

        while (elements.hasNext()) {

            JsonNode qualifier = elements.next();

            JsonNode versions = qualifier.get(SedaConstants.TAG_VERSIONS);

            if (usage.equals(qualifier.get(SedaConstants.PREFIX_QUALIFIER).textValue())) {

                if (versions.isArray() && versions.size() > 0) {
                    model = getRightVersionFromElementList(usage, usageVersion, versions);
                }
            }
        }
        if (!model.isPresent()) {
            return Optional.empty();
        }
        VersionsModel versionsModel = model.get();
        probativeUsageParameter.setVersionsModel(versionsModel);
        probativeUsageParameter.setId(versionsModel.getId());

        return Optional.of(probativeUsageParameter);
    }

    private void reportCheckOk(ProbativeUsageParameter parameters, String name) {

        parameters.getReports().add(new ProbativeCheckReport(OK, name));
    }

    private void reportCheckKo(ProbativeUsageParameter parameter, String name, String details) {

        parameter.getReports().add(new ProbativeCheckReport(KO, name, details));
    }

    private Optional<VersionsModel> getRightVersionFromElementList(String usage, String usageVersion, JsonNode versions)
        throws InvalidParseOperationException {

        JsonNode versionNode = null;
        int counter = 0;

        for (final JsonNode version : versions) {

            if (version.get(SedaConstants.TAG_PHYSICAL_ID) != null) {
                continue;
            }

            String dataObjectVersion = version.get(SedaConstants.TAG_DO_VERSION).asText();

            if (dataObjectVersion.equals(usage + "_" + usageVersion)) {
                return getVersionsModel(version);
            }

            if (LAST_VERSION.equals(usageVersion)) {
                try {

                    int versionNumber = getVersionNumberFromObjectVersion(usage, dataObjectVersion);

                    if (versionNumber > counter) {

                        versionNode = version;
                    }
                    counter = versionNumber;
                } catch (NumberFormatException e) {

                    throw new IllegalStateException("Wrong usageVersion ");
                }
            }
        }

        if (versionNode == null) {
            throw new IllegalStateException("Version Node cant be null");
        }

        return getVersionsModel(versionNode);
    }

    private Optional<VersionsModel> getVersionsModel(JsonNode versionNode)
        throws InvalidParseOperationException {

        ObjectNode objectVersion = (ObjectNode) versionNode;
        objectVersion.set("#storage", objectVersion.get("_storage"));
        objectVersion.remove("_storage");

        VersionsModel model = getFromJsonNode(objectVersion, VersionsModel.class);

        model.setId(versionNode.get("_id").textValue());
        model.setOpi(versionNode.get("_opi").textValue());

        return Optional.of(model);
    }


    private int getVersionNumberFromObjectVersion(String usage, String dataObjectVersion) {

        int length = (usage + "_").length();
        return Integer.parseInt(dataObjectVersion.substring(length));
    }

    void checkSecuredVersion(LifeCycleTraceabilitySecureFileObject secureFileObject,
        ProbativeUsageParameter parameter) {

        String hashLFCEvents = secureFileObject.getHashLFCEvents();
        String checkName = "Check Secure object  Hash And LFC Events";
        boolean isCheckSecuredObjectOk = false;

        for (ObjectGroupDocumentHash documentHash : secureFileObject.getObjectGroupDocumentHashList()) {

            if (documentHash.gethObject().equals(parameter.getVersionsModel().getMessageDigest())) {
                isCheckSecuredObjectOk = true;
                break;
            }
        }

        isCheckSecuredObjectOk = isCheckSecuredObjectOk && hashLFCEvents.equals(parameter.getHashEvents());

        String errorMessage ;
        if (!isCheckSecuredObjectOk) {
            errorMessage = "Hash for Lfc events  is not equal to secured one ";
            reportCheckKo(parameter, checkName, errorMessage);
            return;
        }
        reportCheckOk(parameter, checkName);


    }
}
