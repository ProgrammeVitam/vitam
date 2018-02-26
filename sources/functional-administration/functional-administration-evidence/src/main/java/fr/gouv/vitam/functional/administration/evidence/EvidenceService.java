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
package fr.gouv.vitam.functional.administration.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleStatusCode;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.evidence.exception.EvidenceAuditException;
import fr.gouv.vitam.functional.administration.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.util.VisibleForTesting;

import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.parser.query.QueryParserHelper.lte;
import static fr.gouv.vitam.common.json.JsonHandler.unprettyPrint;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult.fromMetadataJson;

/**
 * Evidence Service class
 */
public class EvidenceService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceService.class);
    private static final String DEFAULT_STORAGE_STRATEGY = "default";
    private static final String TMP = "tmp";
    private static final String FILE_NAME = "FileName";

    private static final String ADMIN_MODULE = "ADMIN_MODULE";
    private static final String LAST_PERSISTED_DATE = "_lastPersistedDate";
    private static final String ZIP = ".zip";
    private static final String DATA_TXT = "data.txt";
    private static final String TXT = ".txt";
    private static final String NO_TRACEABILITY_OPERATION_FOUND_MATCHING_DATE =
        "No traceability operation found matching date ";
    private static final String DIGEST_ALGORITHM = "DigestAlgorithm";
    private static final String DIGEST = "Hash";
    private static final String KO_DATABASE_VERSION_LOWER_THAN_TRACEABILITY_FILE =
        "Invalid version. Database version (%s) cannot be lower than secured one (%s)";
    private static final String WARN_DATABASE_VERSION_NET_YET_SECURED =
        "Invalid version. Database version (%s) not yet secured. Last secured version was (%s)";
    private static final String KO_UNSECURED_DATABASE_VERSION =
        "Invalid version. Database version (%s) has not been secured. Last secured version was (%s)";
    public static final String JSON = ".json";
    private static final String EVIDENCE_AUDIT = "EVIDENCEAUDIT";
    private static final String EVIDENCE_AUDIT_DATABASE = "EVIDENCEAUDIT_DATABASE";
    private static final String EVIDENCE_AUDIT_STORAGE = "EVIDENCEAUDIT_STORAGE";
    private MetaDataClientFactory metaDataClientFactory = MetaDataClientFactory.getInstance();

    private LogbookOperationsClientFactory logbookOperationsClientFactory =
        LogbookOperationsClientFactory.getInstance();

    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory =
        LogbookLifeCyclesClientFactory.getInstance();
    private StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

    public EvidenceService() {
    }

    @VisibleForTesting
    public EvidenceService(MetaDataClientFactory metaDataClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        StorageClientFactory storageClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * launchEvidence
     *
     * @param id the id
     * @param metadataType the metadataType
     */
    public RequestResponse<JsonNode> launchEvidence(String id, LifeCycleTraceabilitySecureFileObject.MetadataType metadataType) {

        EvidenceAuditParameters auditParameters = new EvidenceAuditParameters();

        GUID eip = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

        try {
            createEvidenceAuditOperation(eip, id, metadataType);

            auditParameters.setId(id);
            auditParameters.setMetadataType(metadataType);

            // Get metadata from DB
            JsonNode metadata = getMetadata(id, metadataType);
            auditParameters.setMetadata(metadata);

            // Get lifecycle from DB
            JsonNode lifecycle = getLifeCycle(id, metadataType);
            auditParameters.setLifecycle(lifecycle);

            // Get most recent traceability operation
            String logbookOperationSecurisationId =
                getLogbookSecurisationOperationId(lifecycle);
            auditParameters.setSecurisationOperationId(logbookOperationSecurisationId);

            // Is the current operation the last lifecycle traceability
            boolean isLastLifeCycleTraceabilityOperation =
                isLastLifeCycleTraceabilityOperation(logbookOperationSecurisationId);
            auditParameters.setLastSecurisation(isLastLifeCycleTraceabilityOperation);

            // Get filename, digest algorithm & digest from
            loadFileInfoFromLogbook(logbookOperationSecurisationId, auditParameters);

            File traceabilityFile = downloadFileInTemporaryFolder(auditParameters);
            File dataFile = extractFileStreamFromZip(traceabilityFile);

            loadInformationFromFile(dataFile, auditParameters);

            checkTraceabilityInformationVersion(auditParameters);

            auditTraceabilityInformation(auditParameters, eip);

            createLogbookAuditEvents(eip, StatusCode.OK, JsonHandler.createObjectNode(), EVIDENCE_AUDIT);

            return new RequestResponseOK<JsonNode>()
                .setHttpCode(Response.Status.OK.getStatusCode());

        } catch (EvidenceAuditException e) {
            LOGGER.error(e);
            StatusCode statusCode = getStatus(e.getStatus());
            buildAuditReportReportFailure(eip, e.getMessage(), statusCode);
            return getErrorEntity(statusCode.getEquivalentHttpStatus(), e.getMessage(), null);
        }
    }

    private void createEvidenceAuditOperation(GUID eip, String id,
        LifeCycleTraceabilitySecureFileObject.MetadataType metadataType) throws EvidenceAuditException {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            final LogbookOperationParameters logbookParameters;
            logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eip, EVIDENCE_AUDIT, eip,
                    LogbookTypeProcess.AUDIT,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(EVIDENCE_AUDIT, StatusCode.STARTED), eip);
            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put("Id", id);
            evDetData.put("MetadataType", metadataType.getName());
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                unprettyPrint(evDetData));
            client.create(logbookParameters);
        } catch (LogbookClientBadRequestException | LogbookClientAlreadyExistsException | LogbookClientServerException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not create logbook operation", e);
        }
    }

    private void checkTraceabilityInformationVersion(EvidenceAuditParameters auditParameters)
        throws EvidenceAuditException {

        int lfcVersion = auditParameters.getLifecycle().get(LogbookDocument.VERSION).asInt();
        int securedVersion = auditParameters.getTraceabilityLine().getVersion();

        if (lfcVersion == securedVersion) {
            // Everything's fine
            return;
        }

        if (lfcVersion < securedVersion) {
            throw new EvidenceAuditException(EvidenceStatus.KO,
                String.format(KO_DATABASE_VERSION_LOWER_THAN_TRACEABILITY_FILE, lfcVersion, securedVersion));
        }

        if (auditParameters.isLastSecurisation()) {
            throw new EvidenceAuditException(EvidenceStatus.WARN,
                String.format(WARN_DATABASE_VERSION_NET_YET_SECURED, lfcVersion, securedVersion));
        }

        throw new EvidenceAuditException(EvidenceStatus.KO,
            String.format(KO_UNSECURED_DATABASE_VERSION, lfcVersion, securedVersion));
    }

    private JsonNode getLifeCycle(String id, LifeCycleTraceabilitySecureFileObject.MetadataType metadataType)
        throws EvidenceAuditException {

        switch (metadataType) {
            case UNIT:
                return getUnitLifeCycle(id);
            case OBJECTGROUP:
                return getObjectGroupLifeCycle(id);
            default:
                throw new IllegalStateException("Unsupported metadata type " + metadataType);
        }
    }

    /**
     * @param unitId the unit id
     * @return unit lifecycle as json
     */
    private JsonNode getUnitLifeCycle(String unitId) throws EvidenceAuditException {

        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {

            Select query = new Select();
            query.setQuery(eq(LogbookMongoDbName.objectIdentifier.getDbname(), unitId));

            JsonNode unitLFC = logbookLifeCyclesClient
                .selectUnitLifeCycleById(unitId, query.getFinalSelect(), LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);

            return unitLFC.get(TAG_RESULTS).get(0);

        } catch (LogbookClientNotFoundException e) {
            throw new EvidenceAuditException(EvidenceStatus.KO,
                String.format("No such lifecycle unit found '%s'", unitId), e);
        } catch (InvalidParseOperationException | InvalidCreateOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "An error occurred during unit lifecycle retrieval",
                e);
        }
    }

    /**
     * @param objectGroupId the object group id
     * @return Object group lifecycle as json
     */
    private JsonNode getObjectGroupLifeCycle(String objectGroupId) throws EvidenceAuditException {

        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {

            Select query = new Select();
            query.setQuery(eq(LogbookMongoDbName.objectIdentifier.getDbname(), objectGroupId));

            JsonNode objectGroupLFC = logbookLifeCyclesClient
                .selectObjectGroupLifeCycleById(objectGroupId, query.getFinalSelect(),
                    LifeCycleStatusCode.LIFE_CYCLE_COMMITTED);

            return objectGroupLFC.get(TAG_RESULTS).get(0);

        } catch (LogbookClientNotFoundException e) {
            throw new EvidenceAuditException(EvidenceStatus.KO,
                String.format("No such lifecycle object group found '%s'", objectGroupId), e);
        } catch (InvalidParseOperationException | InvalidCreateOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "An error occurred during object group lifecycle retrieval",
                e);
        }
    }

    private JsonNode getMetadata(String id, LifeCycleTraceabilitySecureFileObject.MetadataType metadataType)
        throws EvidenceAuditException {

        switch (metadataType) {
            case UNIT:
                return getUnitMetadata(id);
            case OBJECTGROUP:
                return getObjectGroupMetadata(id);
            default:
                throw new IllegalStateException("Unsupported metadata type " + metadataType);
        }
    }

    /**
     * @param unitId id
     * @return unit metadata as json
     */
    private JsonNode getUnitMetadata(String unitId) throws EvidenceAuditException {
        MetaDataClient metaDataClient = metaDataClientFactory.getClient();

        RequestResponse<JsonNode> requestResponse;
        try {
            requestResponse = metaDataClient.getUnitByIdRaw(unitId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            }

            throw new EvidenceAuditException(EvidenceStatus.KO, String.format("No such unit metadata '%s'", unitId));

        } catch (VitamClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "An error occurred during unit metadata retrieval",
                e);
        }
    }

    /**
     * @param objectGroupId id
     * @return object group metadata as json
     */
    private JsonNode getObjectGroupMetadata(String objectGroupId) throws EvidenceAuditException {
        MetaDataClient metaDataClient = metaDataClientFactory.getClient();

        RequestResponse<JsonNode> requestResponse;
        try {
            requestResponse = metaDataClient.getObjectGroupByIdRaw(objectGroupId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            }

            throw new EvidenceAuditException(EvidenceStatus.KO,
                String.format("No such object group metadata '%s'", objectGroupId));

        } catch (VitamClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "An error occurred during object group metadata retrieval",
                e);
        }
    }

    /**
     * Get logbook operation by unit lifecycle
     *
     * @param unitLifecycle the unit's lifeCycle
     * @return the logbookOperationId
     */
    private String getLogbookSecurisationOperationId(JsonNode unitLifecycle) throws EvidenceAuditException {

        String lastPersistedDate = unitLifecycle.get(LAST_PERSISTED_DATE).asText();

        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {

            Select select = new Select();
            BooleanQuery query = and().add(
                eq(LogbookMongoDbName.eventType.getDbname(), "LOGBOOK_LC_SECURISATION"),
                eq("events.outDetail", "LOGBOOK_LC_SECURISATION.OK"),
                lte("events.evDetData.StartDate", lastPersistedDate),
                gte("events.evDetData.EndDate", lastPersistedDate)
            );

            select.setQuery(query);
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter("events.evDateTime");

            JsonNode result = logbookOperationsClient.selectOperation(select.getFinalSelect());

            return result.get(TAG_RESULTS).get(0).get(LogbookMongoDbName.eventIdentifier.getDbname()).asText();

        } catch (LogbookClientNotFoundException e) {
            throw new EvidenceAuditException(EvidenceStatus.WARN,
                NO_TRACEABILITY_OPERATION_FOUND_MATCHING_DATE + lastPersistedDate, e);
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "An error occurred during traceability operation retrieval", e);
        }
    }

    /**
     * Returns whether the traceability operation is the last traceability operation.
     *
     * @param operationId the operation Id
     * @return true if the operationId is the last traceability operation. False otherwise.
     * @throws LogbookClientException on logbook access failure
     */
    private boolean isLastLifeCycleTraceabilityOperation(String operationId) throws EvidenceAuditException {

        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {

            Select select = new Select();

            BooleanQuery query = and().add(
                eq(LogbookMongoDbName.eventType.getDbname(), "LOGBOOK_LC_SECURISATION"),
                eq("events.outDetail", "LOGBOOK_LC_SECURISATION.OK")
            );

            select.setQuery(query);
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter("events.evDateTime");

            JsonNode result = logbookOperationsClient.selectOperation(select.getFinalSelect());

            String lastOperationId =
                result.get(TAG_RESULTS).get(0).get(LogbookMongoDbName.eventIdentifier.getDbname()).asText();
            return lastOperationId.equals(operationId);

        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "An error occurred during last traceability operation retrieval", e);
        }
    }

    private void auditTraceabilityInformation(EvidenceAuditParameters auditParameters, GUID eip)
        throws EvidenceAuditException {

        boolean databaseAuditSuccess = computeAuditForDatabaseInformations(auditParameters, eip);

        boolean storageAuditSuccess = computeAuditForStorage(auditParameters, eip);

        boolean auditSuccess = databaseAuditSuccess && storageAuditSuccess;

        if(!auditSuccess)
            throw new EvidenceAuditException(EvidenceStatus.KO, "Traceability check failed");
    }

    private boolean computeAuditForStorage(EvidenceAuditParameters auditParameters,
        GUID eip) throws EvidenceAuditException {


        StoredInfoResult mdOptimisticStorageInfo = fromMetadataJson(auditParameters.getMetadata());

        DataCategory dataCategory = getDataCategory(auditParameters);

        JsonNode storageMetadataResultListJsonNode;

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            // store binary data object
            storageMetadataResultListJsonNode = storageClient.
                getInformation(mdOptimisticStorageInfo.getStrategy(),
                    dataCategory,
                    auditParameters.getId() + JSON,
                    mdOptimisticStorageInfo.getOfferIds());


        } catch (StorageServerClientException | StorageNotFoundClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "An error occurred during last traceability operation retrieval", e);
        }

        String docWithLfcDigest = auditParameters.getTraceabilityLine().getHashGlobalFromStorage();

        ArrayNode errorMessages = JsonHandler.createArrayNode();

        if (mdOptimisticStorageInfo.getOfferIds().size() == 0) {
            errorMessages.add("No storage metadata found for file");
        }

        boolean checkStorageHashOk = true;

        ObjectNode objectOffersHash = JsonHandler.createObjectNode();
        for (String offerId : mdOptimisticStorageInfo.getOfferIds()) {

            JsonNode metadataResultJsonNode = storageMetadataResultListJsonNode.get(offerId);

            if (metadataResultJsonNode != null && metadataResultJsonNode.isObject()) {

                StorageMetadatasResult storageMetadataResult;
                try {
                    storageMetadataResult =
                        JsonHandler.getFromJsonNode(metadataResultJsonNode, StorageMetadatasResult.class);
                } catch (InvalidParseOperationException e) {
                    throw new EvidenceAuditException(EvidenceStatus.FATAL,
                        "An error occurred during last traceability operation retrieval", e);
                }
                objectOffersHash.put(offerId, storageMetadataResult.getDigest());

                if (!docWithLfcDigest.equals(storageMetadataResult.getDigest())) {
                    checkStorageHashOk = false;
                }

            } else {
                errorMessages.add("No storage metadata found for file in offer " + offerId);
            }
        }

        if (!checkStorageHashOk) {
            errorMessages.add("Storage hash mismatch");
        }

        ObjectNode evDetData = JsonHandler.createObjectNode();
        evDetData.put("TraceabilityFile", auditParameters.getFileName());
        evDetData.put("TraceabilityMetadataAndLifecycleDigest", docWithLfcDigest);
        evDetData.set("OfferMetadataAndLifeCycleDigests", objectOffersHash);

        boolean hasErrors = errorMessages.size() > 0;
        StatusCode status;
        if (hasErrors) {
            status = StatusCode.KO;
            evDetData.set("Errors", errorMessages);
            LOGGER.error(String
                .format("Traceability audit KO for %s %s. Storage check failure", auditParameters.getMetadataType(),
                    auditParameters.getId()));
        } else {
            status = StatusCode.OK;
        }

        createLogbookAuditEvents(eip, status, evDetData, EVIDENCE_AUDIT_STORAGE);

        return !hasErrors;
    }

    private DataCategory getDataCategory(EvidenceAuditParameters auditParameters) {
        switch (auditParameters.getMetadataType()) {
            case UNIT:
                return DataCategory.UNIT;
            case OBJECTGROUP:
                return DataCategory.OBJECTGROUP;
            default:
                throw new IllegalStateException("Unsupported metadata type " + auditParameters.getMetadataType());
        }
    }

    private boolean computeAuditForDatabaseInformations(EvidenceAuditParameters auditParameters,
        GUID eip) throws EvidenceAuditException {

        ObjectNode evDetData = JsonHandler.createObjectNode();
        evDetData.put("TraceabilityFile", auditParameters.getFileName());

        ArrayNode errorMessages = JsonHandler.createArrayNode();

        final DigestType digestType = auditParameters.getDigestType();
        Digest metadataDigest = new Digest(digestType);

        // compare Md digest
        final String hashMdFromDatabase =
            metadataDigest
                .update(unprettyPrint(auditParameters.getMetadata()).getBytes(StandardCharsets.UTF_8))
                .digest64();

        String hashMetadata = auditParameters.getTraceabilityLine().getHashMetadata();

        evDetData.put("DatabaseMetadataDigest", hashMdFromDatabase);
        evDetData.put("TraceabilityMetadataDigest", hashMetadata);

        if (!hashMdFromDatabase.equals(hashMetadata)) {
            errorMessages.add("Metadata hash mismatch");
        }

        Digest lifecycleDigest = new Digest(digestType);
        final String hashLfcFromDatabase =
            lifecycleDigest
                .update(unprettyPrint(auditParameters.getLifecycle()).getBytes(StandardCharsets.UTF_8))
                .digest64();
        String hashLfc = auditParameters.getTraceabilityLine().getHashLFC();

        evDetData.put("DatabaseLifecycleDigest", hashLfcFromDatabase);
        evDetData.put("TraceabilityLifecycleDigest", hashLfc);

        if (!hashLfcFromDatabase.equals(hashLfc)) {
            errorMessages.add("Lifecycle hash mismatch");
        }

        boolean hasErrors = errorMessages.size() > 0;
        StatusCode status;
        if (hasErrors) {
            status = StatusCode.KO;
            evDetData.set("Errors", errorMessages);
            LOGGER.error(String
                .format("Traceability audit KO for %s %s. Database check failure", auditParameters.getMetadataType(),
                    auditParameters.getId()));
        } else {
            status = StatusCode.OK;
        }

        createLogbookAuditEvents(eip, status, evDetData, EVIDENCE_AUDIT_DATABASE);

        return !hasErrors;
    }

    private void createLogbookAuditEvents(GUID eip, StatusCode status, ObjectNode evDetData,
        String evidenceAuditDatabase) throws EvidenceAuditException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            GUID eipEvent = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

            final LogbookOperationParameters logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipEvent, evidenceAuditDatabase, eip,
                    LogbookTypeProcess.AUDIT,
                    status,
                    VitamLogbookMessages.getCodeOp(evidenceAuditDatabase, status), eip);
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                unprettyPrint(evDetData));
            logbookParameters.putParameterValue(LogbookParameterName.masterData, unprettyPrint(evDetData));
            client.update(logbookParameters);

        } catch (LogbookClientNotFoundException | LogbookClientBadRequestException | LogbookClientServerException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "Could not update logbook operation " + eip + " with status " + status, e);
        }
    }

    private void loadInformationFromFile(File file, EvidenceAuditParameters auditParameters)
        throws EvidenceAuditException {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {

            String line;
            while ((line = br.readLine()) != null) {

                if (StringUtils.isEmpty(line)) {
                    continue;
                }

                boolean result = findLine(line, auditParameters);

                if (result) {
                    return;
                }
            }

            throw new EvidenceAuditException(EvidenceStatus.KO,
                "Could not find matching traceability info in the file");

        } catch (IOException e) {
            LOGGER.error(e);
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not open file " + file, e);

        } finally {
            if (!file.delete()) {
                LOGGER.warn("Could not delete file " + file);
            }
        }
    }

    private boolean findLine(String line, EvidenceAuditParameters auditParameters) throws EvidenceAuditException {

        try {

            LifeCycleTraceabilitySecureFileObject traceabilityLine =
                JsonHandler.getFromString(line, LifeCycleTraceabilitySecureFileObject.class);

            if (traceabilityLine.getLfcId() == null) {
                throw new IllegalStateException("Missing lfc Id");
            }

            if (traceabilityLine.getMetadataType() != auditParameters.getMetadataType()) {
                return false;
            }

            if (traceabilityLine.getLfcId().equals(auditParameters.getId())) {
                auditParameters.setTraceabilityLine(traceabilityLine);
                return true;
            }

            return false;

        } catch (InvalidParseOperationException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                "Could not parse traceability file information line '" + line + "'", e);
        }
    }

    private void buildAuditReportReportFailure(GUID eip, String message, StatusCode statusCode) {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            GUID eipEvent = GUIDFactory.newOperationLogbookGUID(ParameterHelper.getTenantParameter());

            final LogbookOperationParameters logbookParameters;

            logbookParameters = LogbookParametersFactory
                .newLogbookOperationParameters(eipEvent, EVIDENCE_AUDIT, eip,
                    LogbookTypeProcess.AUDIT,
                    statusCode,
                    VitamLogbookMessages.getCodeOp(EVIDENCE_AUDIT, statusCode), eip);
            ObjectNode evDetData = JsonHandler.createObjectNode();
            evDetData.put("Message", message);
            logbookParameters.putParameterValue(LogbookParameterName.eventDetailData,
                unprettyPrint(evDetData));
            client.update(logbookParameters);
        } catch (LogbookClientNotFoundException | LogbookClientBadRequestException | LogbookClientServerException e) {
            LOGGER.error("Could not update logbook operation " + eip + " with status " + statusCode, e);
        }
    }

    private StatusCode getStatus(EvidenceStatus status) {
        switch (status) {
            case KO:
                return StatusCode.KO;
            case WARN:
                return StatusCode.WARNING;
            case FATAL:
            default:
                return StatusCode.FATAL;
        }
    }

    private File extractFileStreamFromZip(File file) throws EvidenceAuditException {


        try (ZipFile zipFile = new ZipFile(file)) {

            ZipEntry dataEntry = zipFile.getEntry(DATA_TXT);

            try (InputStream dataStream = zipFile.getInputStream(dataEntry)) {

                File dataFile = File.createTempFile(TMP, TXT, new File(VitamConfiguration.getVitamTmpFolder()));
                Files.copy(dataStream, dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                return dataFile;
            }

        } catch (IOException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not extract zip file " + file, e);
        } finally {
            if (!file.delete()) {
                LOGGER.warn("Could not delete file " + file);
            }
        }
    }

    private void loadFileInfoFromLogbook(String id,
        EvidenceAuditParameters auditParameters) throws EvidenceAuditException {

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {

            JsonNode result = client.selectOperationById(id, new Select().getFinalSelect());

            String detailData =
                result.get(TAG_RESULTS).get(0).get(LogbookMongoDbName.eventDetailData.getDbname()).asText();
            JsonNode nodeEvDetData = JsonHandler.getFromString(detailData);

            auditParameters.setFileName(nodeEvDetData.get(FILE_NAME).asText());

            String digestText = nodeEvDetData.get(DIGEST_ALGORITHM).asText();

            DigestType digestType = DigestType.valueOf(digestText);
            auditParameters.setDigestType(digestType);

            String digestValue = nodeEvDetData.get(DIGEST).asText();
            auditParameters.setFileDigest(digestValue);

        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not retrieve logbook operation information",
                e);
        }
    }

    private File downloadFileInTemporaryFolder(EvidenceAuditParameters auditParameters) throws EvidenceAuditException {

        // Get zip file
        Response response = null;

        try (StorageClient storageClient = storageClientFactory.getClient()) {

            response = storageClient
                .getContainerAsync(DEFAULT_STORAGE_STRATEGY, auditParameters.getFileName(), DataCategory.LOGBOOK);

            Digest digest = new Digest(auditParameters.getDigestType());
            try (InputStream inputStream = digest.getDigestInputStream(response.readEntity(InputStream.class))) {

                final File file = File.createTempFile(TMP, ZIP, new File(VitamConfiguration.getVitamTmpFolder()));
                Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                response.close();

                return file;
            }

        } catch (StorageNotFoundException | StorageServerClientException | IOException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL,
                String.format("Could not retrieve traceability zip file '%s'", auditParameters.getFileName()), e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * Construct the error following input
     *
     * @param status  Http error status
     * @param message The functional error message, if absent the http reason phrase will be used instead
     * @param code    The functional error code, if absent the http code will be used instead
     * @return
     */
    private VitamError getErrorEntity(Response.Status status, String message, String code) {
        String aMessage =
            (message != null && !message.trim().isEmpty()) ? message
                : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        String aCode = (code != null) ? code : String.valueOf(status.getStatusCode());
        return new VitamError(aCode).setHttpCode(status.getStatusCode()).setContext(ADMIN_MODULE)
            .setState("code_vitam").setMessage(status.getReasonPhrase()).setDescription(aMessage);
    }
}
