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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.database.utils.MetadataDocumentHelper;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.CanonicalJsonFormatter;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.driver.model.StorageMetadataResult;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyNotFoundException;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyUtils;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceAuditException;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditParameters;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportLine;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditReportObject;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;

/**
 * Evidence Service class
 */
public class EvidenceService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceService.class);
    private static final String TMP = "tmp";
    private static final String FILE_NAME = "FileName";
    private static final String LAST_PERSISTED_DATE = "_lastPersistedDate";
    private static final String NO_TRACEABILITY_OPERATION_FOUND_MATCHING_DATE =
        "No traceability operation found matching date ";
    private static final String DIGEST_ALGORITHM = "DigestAlgorithm";
    private static final String DIGEST = "Hash";
    public static final String JSON = ".json";
    private static final String KO_DATABASE_VERSION_LOWER_THAN_TRACEABILITY_FILE =
        "Invalid version. Database version (%s) cannot be lower than secured one (%s)";
    private static final String WARN_DATABASE_VERSION_NET_YET_SECURED =
        "Invalid version. Database version (%s) not yet secured. Last secured version was (%s)";
    private static final String KO_UNSECURED_DATABASE_VERSION =
        "Invalid version. Database version (%s) has not been secured. Last secured version was (%s)";
    public static final String OK = ".OK";
    public static final String WARNING = ".WARNING";
    private static final String LOGBOOK_UNIT_LFC_TRACEABILITY = Contexts.UNIT_LFC_TRACEABILITY.getEventType();
    private static final String LOGBOOK_UNIT_LFC_TRACEABILITY_OK = LOGBOOK_UNIT_LFC_TRACEABILITY + OK;
    private static final String LOGBOOK_UNIT_LFC_TRACEABILITY_WARNING = LOGBOOK_UNIT_LFC_TRACEABILITY + WARNING;
    private static final String LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY =
        Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType();
    private static final String LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY_OK = LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY + OK;
    private static final String LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY_WARNING =
        LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY + WARNING;

    private static final String EVENTS_EVDETDATA_FILENAME = "events.evDetData.FileName";
    private static final String EVENTS_OUT_DETAIL = "events.outDetail";
    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    private final StorageClientFactory storageClientFactory;

    public EvidenceService() {
        storageClientFactory = StorageClientFactory.getInstance();
        logbookLifeCyclesClientFactory = LogbookLifeCyclesClientFactory.getInstance();
        metaDataClientFactory = MetaDataClientFactory.getInstance();
        logbookOperationsClientFactory = LogbookOperationsClientFactory.getInstance();
    }

    @VisibleForTesting
    EvidenceService(
        MetaDataClientFactory metaDataClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        StorageClientFactory storageClientFactory
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
    }

    /**
     * audit and generate
     *
     * @param parameters parameters
     * @param securedLines secured Lin
     * @param id identifier
     * @return EvidenceAuditReportLine
     */
    public EvidenceAuditReportLine auditAndGenerateReportIfKo(
        EvidenceAuditParameters parameters,
        List<String> securedLines,
        String id
    ) {
        EvidenceAuditReportLine evidenceAuditReportLine = new EvidenceAuditReportLine(id);
        LifeCycleTraceabilitySecureFileObject securedObject;
        List<String> errorsMessage = new ArrayList<>();
        evidenceAuditReportLine.setObjectType(parameters.getMetadataType());
        try {
            securedObject = loadInformationFromFile(securedLines, parameters.getMetadataType(), id);

            checkTraceabilityInformationVersion(parameters, securedObject);

            if (!parameters.getHashMdFromDatabase().equals(securedObject.getHashMetadata())) {
                errorsMessage.add(
                    String.format(
                        " Metadata hash '%s' mismatch secured metadata hash '%s' ",
                        parameters.getHashMdFromDatabase(),
                        securedObject.getHashMetadata()
                    )
                );
            }

            if (!parameters.getHashLfcFromDatabase().equals(securedObject.getHashLFC())) {
                errorsMessage.add(
                    String.format(
                        " Metadata hash '%s' mismatch secured lfc hash '%s' ",
                        parameters.getHashLfcFromDatabase(),
                        securedObject.getHashLFC()
                    )
                );
            }

            String securedGlobalHash = securedObject.getHashGlobalFromStorage();
            StoredInfoResult mdOptimisticStorageInfo = parameters.getMdOptimisticStorageInfo();
            // set hash for rectification
            evidenceAuditReportLine.setSecuredHash(securedGlobalHash);
            Map<String, String> offerHashes = new HashMap<>();
            for (String offerId : mdOptimisticStorageInfo.getOfferIds()) {
                String strategyId = mdOptimisticStorageInfo.getStrategy();
                JsonNode metadataResultJsonNode = parameters.getStorageMetadataResultListJsonNode().get(offerId);
                StorageMetadataResult storageMetadataResult = null;
                if (metadataResultJsonNode != null && metadataResultJsonNode.isObject()) {
                    try {
                        storageMetadataResult = JsonHandler.getFromJsonNode(
                            metadataResultJsonNode,
                            StorageMetadataResult.class
                        );
                    } catch (InvalidParseOperationException e) {
                        throw new EvidenceAuditException(
                            EvidenceStatus.FATAL,
                            "An error occurred during last traceability operation retrieval",
                            e
                        );
                    }

                    if (!securedGlobalHash.equals(storageMetadataResult.getDigest())) {
                        errorsMessage.add(
                            String.format(
                                " OfferId %s : Storage hash '%s' mismatch secured lfc hash '%s' ",
                                offerId,
                                storageMetadataResult.getDigest(),
                                securedGlobalHash
                            )
                        );
                    }
                } else {
                    errorsMessage.add("No storage metadata found for file in offer " + offerId);
                }
                String digest = null;
                if (storageMetadataResult != null) {
                    digest = storageMetadataResult.getDigest();
                }
                offerHashes.put(offerId, digest);
                evidenceAuditReportLine.setOffersHashes(offerHashes);
                evidenceAuditReportLine.setStrategyId(strategyId);
            }

            if (parameters.getMetadataType().equals(MetadataType.OBJECTGROUP)) {
                auditObjects(parameters, securedObject, errorsMessage, evidenceAuditReportLine);
            }

            if (errorsMessage.size() > 0) {
                String message =
                    "Traceability audit KO  Database check failure Errors are :  " +
                    JsonHandler.prettyPrint(errorsMessage);
                evidenceAuditReportLine.setEvidenceStatus(EvidenceStatus.KO);
                evidenceAuditReportLine.setMessage(message);
            }
        } catch (EvidenceAuditException | InvalidParseOperationException e) {
            LOGGER.error(e);
            evidenceAuditReportLine.setEvidenceStatus(EvidenceStatus.KO);
            evidenceAuditReportLine.setMessage(e.getMessage());
        }
        return evidenceAuditReportLine;
    }

    private void auditObjects(
        EvidenceAuditParameters parameters,
        LifeCycleTraceabilitySecureFileObject securedObject,
        List<String> errorsMessage,
        EvidenceAuditReportLine evidenceAuditReportLine
    ) throws InvalidParseOperationException {
        Map<String, JsonNode> objectStorageMetadataResultMap = parameters.getObjectStorageMetadataResultMap();

        List<String> errorsObjectMessages = new ArrayList<>();
        final List<ObjectGroupDocumentHash> objectGroupDocumentHashList =
            securedObject.getObjectGroupDocumentHashList();

        ArrayList<EvidenceAuditReportObject> reportList = new ArrayList<>();
        for (ObjectGroupDocumentHash objectGroupDocumentHash : objectGroupDocumentHashList) {
            EvidenceAuditReportObject evidenceAuditReportObject = new EvidenceAuditReportObject(
                objectGroupDocumentHash.getId()
            );

            String objectVersionId = objectGroupDocumentHash.getId();
            String objectVersionHash = objectGroupDocumentHash.gethObject();
            JsonNode objectVersionStorageMetadataResult = objectStorageMetadataResultMap.get(objectVersionId);

            final StoredInfoResult storedInfoResult = parameters.getMdOptimisticStorageInfoMap().get(objectVersionId);
            List<String> errorsObjectMessage = new ArrayList<>();

            Map<String, String> offerHashes = new HashMap<>();
            for (String offerId : storedInfoResult.getOfferIds()) {
                final JsonNode objectVersionResultJsonNode = objectVersionStorageMetadataResult.get(offerId);

                if (objectVersionResultJsonNode == null) {
                    errorsObjectMessage.add(
                        String.format("The digest '%s' for the offer '%s' is null ", objectVersionHash, offerId)
                    );
                    offerHashes.put(offerId, null);
                    continue;
                }
                final StorageMetadataResult storageMetadataResult = JsonHandler.getFromJsonNode(
                    objectVersionResultJsonNode,
                    StorageMetadataResult.class
                );
                String digestForOffer = storageMetadataResult.getDigest();

                if (!objectVersionHash.equals(digestForOffer)) {
                    String message = String.format(
                        "The digest '%s' for the offer '%s' is null or not equal to the  Offer globalDigest expected (%s)",
                        objectVersionHash,
                        offerId,
                        digestForOffer
                    );
                    errorsObjectMessage.add(message);
                }
                offerHashes.put(offerId, storageMetadataResult.getDigest());
            }
            evidenceAuditReportObject.setOffersHashes(offerHashes);
            evidenceAuditReportObject.setSecuredHash(objectGroupDocumentHash.gethObject());
            evidenceAuditReportObject.setStrategyId(storedInfoResult.getStrategy());

            if (!errorsObjectMessage.isEmpty()) {
                evidenceAuditReportObject.setEvidenceStatus(EvidenceStatus.KO);
                evidenceAuditReportObject.setMessage(JsonHandler.unprettyPrint(errorsObjectMessage));
                errorsObjectMessages.add(JsonHandler.unprettyPrint(errorsObjectMessage));
            }
            reportList.add(evidenceAuditReportObject);
        }
        evidenceAuditReportLine.setObjectsReports(reportList);
        if (!errorsObjectMessages.isEmpty()) {
            errorsMessage.add("There is an  error on the audit of the  linked  object");
        }
    }

    private void extractObjectStorageMetadataResultMap(
        JsonNode metadata,
        EvidenceAuditParameters auditParameters,
        List<StorageStrategy> storageStrategies
    ) throws StorageException {
        JsonNode qualifiers = metadata.get(SedaConstants.PREFIX_QUALIFIERS);

        Map<String, JsonNode> objectStorageMetadataResultMap = new HashMap<>();
        Map<String, StoredInfoResult> mdOptimisticStorageInfoMap = new HashMap<>();

        if (qualifiers != null && qualifiers.isArray() && qualifiers.size() > 0) {
            List<JsonNode> listQualifiers = JsonHandler.toArrayList((ArrayNode) qualifiers);
            for (final JsonNode qualifier : listQualifiers) {
                JsonNode versions = qualifier.get(SedaConstants.TAG_VERSIONS);
                if (versions.isArray() && versions.size() > 0) {
                    for (final JsonNode version : versions) {
                        if (version.get(SedaConstants.TAG_PHYSICAL_ID) != null) {
                            // Skip physical objects
                            continue;
                        }
                        String objectId = version.get(VitamDocument.ID).asText();

                        StoredInfoResult mdOptimisticStorageInfo = fromMetadataJson(version, storageStrategies);
                        mdOptimisticStorageInfoMap.put(objectId, mdOptimisticStorageInfo);

                        JsonNode storageMetadataResultListJsonNode = getStorageResultsJsonNode(
                            mdOptimisticStorageInfo,
                            DataCategory.OBJECT,
                            objectId
                        );

                        objectStorageMetadataResultMap.put(objectId, storageMetadataResultListJsonNode);
                    }
                }
            }
        }

        auditParameters.setMdOptimisticStorageInfoMap(mdOptimisticStorageInfoMap);
        auditParameters.setObjectStorageMetadataResultMap(objectStorageMetadataResultMap);
    }

    private JsonNode getStorageResultsJsonNode(
        StoredInfoResult mdOptimisticStorageInfo,
        DataCategory dataCategory,
        String uid
    ) throws StorageException {
        JsonNode storageMetadataResultListJsonNode;

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            // store binary data object
            storageMetadataResultListJsonNode = storageClient.getInformation(
                mdOptimisticStorageInfo.getStrategy(),
                dataCategory,
                uid,
                mdOptimisticStorageInfo.getOfferIds(),
                true
            );
        } catch (StorageServerClientException | StorageNotFoundClientException e) {
            throw new StorageException("An error occurred during last traceability operation retrieval", e);
        }
        return storageMetadataResultListJsonNode;
    }

    private void checkTraceabilityInformationVersion(
        EvidenceAuditParameters auditParameters,
        LifeCycleTraceabilitySecureFileObject lifeCycleTraceabilitySecureFileObject
    ) throws EvidenceAuditException {
        int lfcVersion = auditParameters.getLfcVersion();
        int securedVersion = lifeCycleTraceabilitySecureFileObject.getVersion();

        if (lfcVersion == securedVersion) {
            // Everything's fine
            return;
        }

        if (lfcVersion < securedVersion) {
            throw new EvidenceAuditException(
                EvidenceStatus.KO,
                String.format(KO_DATABASE_VERSION_LOWER_THAN_TRACEABILITY_FILE, lfcVersion, securedVersion)
            );
        }

        if (auditParameters.isLastSecurisation()) {
            throw new EvidenceAuditException(
                EvidenceStatus.WARN,
                String.format(WARN_DATABASE_VERSION_NET_YET_SECURED, lfcVersion, securedVersion)
            );
        }

        throw new EvidenceAuditException(
            EvidenceStatus.KO,
            String.format(KO_UNSECURED_DATABASE_VERSION, lfcVersion, securedVersion)
        );
    }

    /**
     * @param fileName the file name
     * @param fileToExtract
     * @param extension
     * @return LifeCycleTraceabilitySecureFileObject
     * @throws EvidenceAuditException the EvidenceAuditException
     */
    public File downloadAndExtractDataFromStorage(
        String fileName,
        String fileToExtract,
        String extension,
        boolean delele
    ) throws EvidenceAuditException {
        File traceabilityFile = downloadFileInTemporaryFolder(fileName, extension);
        return extractFileStreamFromZip(traceabilityFile, fileToExtract, extension, delele);
    }

    private File downloadFileInTemporaryFolder(String fileName, String extension) throws EvidenceAuditException {
        // Get zip file
        Response response = null;
        try (StorageClient storageClient = storageClientFactory.getClient()) {
            response = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                fileName,
                DataCategory.LOGBOOK,
                AccessLogUtils.getNoLogAccessLog()
            );
            try (InputStream inputStream = response.readEntity(InputStream.class)) {
                final File file = FileUtil.createFileInTempDirectoryWithPathCheck(TMP, extension);
                Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                response.close();
                return file;
            }
        } catch (
            StorageNotFoundException
            | StorageServerClientException
            | IOException
            | IllegalPathException
            | StorageUnavailableDataFromAsyncOfferClientException e
        ) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                String.format("Could not retrieve traceability zip file '%s'", fileName),
                e
            );
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    public static LifeCycleTraceabilitySecureFileObject loadInformationFromFile(
        List<String> securedLines,
        MetadataType metadataType,
        String id
    ) throws EvidenceAuditException {
        try {
            for (String line : securedLines) {
                if (StringUtils.isEmpty(line)) {
                    continue;
                }
                LifeCycleTraceabilitySecureFileObject traceabilityLine = JsonHandler.getFromString(
                    line,
                    LifeCycleTraceabilitySecureFileObject.class
                );
                boolean result = findLine(traceabilityLine, metadataType, id);
                if (result) {
                    return traceabilityLine;
                }
            }
            throw new EvidenceAuditException(
                EvidenceStatus.KO,
                "Could not find matching traceability info in the file"
            );
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not parse securised lines " + e);
        }
    }

    public File extractFileStreamFromZip(File file, String fileToExtract, String extension, boolean delete)
        throws EvidenceAuditException {
        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry dataEntry = zipFile.getEntry(fileToExtract);
            try (InputStream dataStream = zipFile.getInputStream(dataEntry)) {
                File dataFile = File.createTempFile(TMP, extension, new File(VitamConfiguration.getVitamTmpFolder()));
                Files.copy(dataStream, dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return dataFile;
            }
        } catch (IOException e) {
            throw new EvidenceAuditException(EvidenceStatus.FATAL, "Could not extract zip file " + file, e);
        } finally {
            if (delete && !file.delete()) {
                LOGGER.warn("Could not delete file " + file);
            }
        }
    }

    private static boolean findLine(
        LifeCycleTraceabilitySecureFileObject traceabilityLine,
        MetadataType metadataType,
        String id
    ) {
        if (traceabilityLine.getLfcId() == null) {
            throw new IllegalStateException("Missing lfc Id");
        }

        return traceabilityLine.getMetadataType() == metadataType && traceabilityLine.getLfcId().equals(id);
    }

    /**
     * evidenceAuditsChecks
     *
     * @param id the id
     * @param metadataType the metadataType
     * @param storageStrategies the storageStrategies
     */
    public EvidenceAuditParameters evidenceAuditsChecks(
        String id,
        MetadataType metadataType,
        List<StorageStrategy> storageStrategies
    ) {
        EvidenceAuditParameters auditParameters = new EvidenceAuditParameters();
        try {
            auditParameters.setId(id);
            auditParameters.setMetadataType(metadataType);

            // Get metadata from DB
            JsonNode metadata = getRawMetadata(id, metadataType);

            StoredInfoResult mdOptimisticStorageInfo = fromMetadataJson(metadata, storageStrategies);
            auditParameters.setMdOptimisticStorageInfo(mdOptimisticStorageInfo);

            // Get lifecycle from DB
            JsonNode lifecycle = getRawLifeCycle(id, metadataType);

            int lfcVersion = lifecycle.get(LogbookDocument.VERSION).asInt();
            auditParameters.setLfcVersion(lfcVersion);

            // get storage info
            JsonNode storageResultsJsonNode = getStorageResultsJsonNode(
                auditParameters.getMdOptimisticStorageInfo(),
                getDataCategory(auditParameters.getMetadataType()),
                auditParameters.getId() + JSON
            );

            // Get most recent traceability operation
            String logbookOperationSecurisationId = getLogbookSecureOperationId(lifecycle, metadataType);
            auditParameters.setSecurisationOperationId(logbookOperationSecurisationId);

            // Is the current operation the last lifecycle traceability
            boolean isLastLifeCycleTraceabilityOperation = isLastLifeCycleTraceabilityOperation(
                logbookOperationSecurisationId,
                metadataType
            );
            auditParameters.setLastSecurisation(isLastLifeCycleTraceabilityOperation);

            // Get filename, digest algorithm & digest from
            loadFileInfoFromLogbook(logbookOperationSecurisationId, auditParameters);

            if (metadataType.equals(MetadataType.OBJECTGROUP)) {
                extractObjectStorageMetadataResultMap(metadata, auditParameters, storageStrategies);
            }

            // calculate and store digests
            switch (metadataType) {
                case UNIT:
                    MetadataDocumentHelper.removeComputedFieldsFromUnit(metadata);
                    break;
                case OBJECTGROUP:
                    MetadataDocumentHelper.removeComputedFieldsFromObjectGroup(metadata);
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown metadata type " + metadataType);
            }
            final String hashMdFromDatabase = generateDigest(metadata, auditParameters.getDigestType());
            final String hashLfcFromDatabase = generateDigest(lifecycle, auditParameters.getDigestType());

            auditParameters.setHashMdFromDatabase(hashMdFromDatabase);
            auditParameters.setHashLfcFromDatabase(hashLfcFromDatabase);

            auditParameters.setStorageMetadataResultListJsonNode(storageResultsJsonNode);

            auditParameters.setEvidenceStatus(EvidenceStatus.OK);
        } catch (StorageException e) {
            LOGGER.error(e);
            auditParameters.setEvidenceStatus(EvidenceStatus.FATAL);
            auditParameters.setAuditMessage(e.getMessage());
        } catch (EvidenceAuditException e) {
            LOGGER.error(e);
            auditParameters.setEvidenceStatus(e.getStatus());
            auditParameters.setAuditMessage(e.getMessage());
        }
        return auditParameters;
    }

    private JsonNode getRawLifeCycle(String id, MetadataType metadataType) throws EvidenceAuditException {
        switch (metadataType) {
            case UNIT:
                return getRawUnitLifeCycle(id);
            case OBJECTGROUP:
                return getRawObjectGroupLifeCycle(id);
            default:
                throw new IllegalStateException("Unsupported metadata type " + metadataType);
        }
    }

    /**
     * @param unitId the unit id
     * @return unit lifecycle as json
     */
    private JsonNode getRawUnitLifeCycle(String unitId) throws EvidenceAuditException {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            return logbookLifeCyclesClient.getRawUnitLifeCycleById(unitId);
        } catch (LogbookClientNotFoundException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.KO,
                String.format("No such lifecycle unit found '%s'", unitId),
                e
            );
        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "An error occurred during unit lifecycle retrieval",
                e
            );
        }
    }

    /**
     * @param objectGroupId the object group id
     * @return Object group lifecycle as json
     */
    private JsonNode getRawObjectGroupLifeCycle(String objectGroupId) throws EvidenceAuditException {
        try (LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient()) {
            return logbookLifeCyclesClient.getRawObjectGroupLifeCycleById(objectGroupId);
        } catch (LogbookClientNotFoundException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.KO,
                String.format("No such lifecycle object group found '%s'", objectGroupId),
                e
            );
        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "An error occurred during object group lifecycle retrieval",
                e
            );
        }
    }

    private JsonNode getRawMetadata(String id, MetadataType metadataType) throws EvidenceAuditException {
        switch (metadataType) {
            case UNIT:
                return getRawUnitMetadata(id);
            case OBJECTGROUP:
                return getRawObjectGroupMetadata(id);
            default:
                throw new IllegalStateException("Unsupported metadata type " + metadataType);
        }
    }

    /**
     * @param unitId id
     * @return unit metadata as json
     */
    private JsonNode getRawUnitMetadata(String unitId) throws EvidenceAuditException {
        MetaDataClient metaDataClient = metaDataClientFactory.getClient();

        RequestResponse<JsonNode> requestResponse;
        try {
            requestResponse = metaDataClient.getUnitByIdRaw(unitId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            }

            throw new EvidenceAuditException(EvidenceStatus.KO, String.format("No such unit metadata '%s'", unitId));
        } catch (VitamClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "An error occurred during unit metadata retrieval",
                e
            );
        }
    }

    /**
     * @param objectGroupId id
     * @return object group metadata as json
     */
    private JsonNode getRawObjectGroupMetadata(String objectGroupId) throws EvidenceAuditException {
        MetaDataClient metaDataClient = metaDataClientFactory.getClient();

        RequestResponse<JsonNode> requestResponse;
        try {
            requestResponse = metaDataClient.getObjectGroupByIdRaw(objectGroupId);

            if (requestResponse.isOk()) {
                return ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            }

            throw new EvidenceAuditException(
                EvidenceStatus.KO,
                String.format("No such object group metadata '%s'", objectGroupId)
            );
        } catch (VitamClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "An error occurred during object group metadata retrieval",
                e
            );
        }
    }

    /**
     * Get logbook operation by unit lifecycle
     *
     * @param unitLifecycle the unit's lifeCycle
     * @param metadataType
     * @return the logbookOperationId
     */
    private String getLogbookSecureOperationId(JsonNode unitLifecycle, MetadataType metadataType)
        throws EvidenceAuditException {
        String lastPersistedDate = unitLifecycle.get(LAST_PERSISTED_DATE).asText();

        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            Select select;
            switch (metadataType) {
                case UNIT:
                    select = createLastSecureSelect(lastPersistedDate, LOGBOOK_UNIT_LFC_TRACEABILITY);
                    break;
                case OBJECTGROUP:
                    select = createLastSecureSelect(lastPersistedDate, LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY);
                    break;
                default:
                    throw new IllegalStateException("Unsupported metadata type " + metadataType);
            }

            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookOperationsClient.selectOperation(select.getFinalSelect())
            );

            if (requestResponseOK.getResults().isEmpty()) {
                throw new EvidenceAuditException(
                    EvidenceStatus.WARN,
                    NO_TRACEABILITY_OPERATION_FOUND_MATCHING_DATE + lastPersistedDate
                );
            }

            return requestResponseOK.getResults().get(0).get(LogbookMongoDbName.eventIdentifier.getDbname()).asText();
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "An error occurred during traceability operation retrieval",
                e
            );
        }
    }

    private Select createLastSecureSelect(String lastPersistedDate, String eventType)
        throws InvalidCreateOperationException, InvalidParseOperationException {
        Select select = new Select();
        BooleanQuery query = and()
            .add(
                eq(LogbookMongoDbName.eventType.getDbname(), eventType),
                in(EVENTS_OUT_DETAIL, eventType + OK, eventType + WARNING),
                exists(EVENTS_EVDETDATA_FILENAME),
                QueryHelper.lte("events.evDetData.StartDate", lastPersistedDate),
                gte("events.evDetData.EndDate", lastPersistedDate)
            );

        select.setQuery(query);
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("events.evDateTime");
        return select;
    }

    /**
     * Returns whether the traceability operation is the last traceability operation.
     *
     * @param operationId the operation Id
     * @param metadataType
     * @return true if the operationId is the last traceability operation. False otherwise.
     */
    private boolean isLastLifeCycleTraceabilityOperation(String operationId, MetadataType metadataType)
        throws EvidenceAuditException {
        try (LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()) {
            BooleanQuery query;
            switch (metadataType) {
                case UNIT:
                    query = and()
                        .add(
                            eq(LogbookMongoDbName.eventType.getDbname(), LOGBOOK_UNIT_LFC_TRACEABILITY),
                            in(
                                EVENTS_OUT_DETAIL,
                                LOGBOOK_UNIT_LFC_TRACEABILITY_OK,
                                LOGBOOK_UNIT_LFC_TRACEABILITY_WARNING
                            ),
                            exists(EVENTS_EVDETDATA_FILENAME)
                        );
                    break;
                case OBJECTGROUP:
                    query = and()
                        .add(
                            eq(LogbookMongoDbName.eventType.getDbname(), LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY),
                            in(
                                EVENTS_OUT_DETAIL,
                                EvidenceService.LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY_OK,
                                EvidenceService.LOGBOOK_OBJECTGROUP_LFC_TRACEABILITY_WARNING
                            ),
                            exists(EVENTS_EVDETDATA_FILENAME)
                        );
                    break;
                default:
                    throw new IllegalStateException("Unsupported metadata type " + metadataType);
            }
            Select select = new Select();
            select.setQuery(query);
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter("events.evDateTime");

            RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(
                logbookOperationsClient.selectOperation(select.getFinalSelect())
            );

            if (requestResponseOK.getResults().isEmpty()) {
                throw new EvidenceAuditException(
                    EvidenceStatus.FATAL,
                    "An error occurred during last traceability operation retrieval. At least one expected"
                );
            }

            String lastOperationId = requestResponseOK
                .getResults()
                .get(0)
                .get(LogbookMongoDbName.eventIdentifier.getDbname())
                .asText();
            return lastOperationId.equals(operationId);
        } catch (InvalidCreateOperationException | InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "An error occurred during last traceability operation retrieval",
                e
            );
        }
    }

    private DataCategory getDataCategory(MetadataType metadataType) {
        switch (metadataType) {
            case UNIT:
                return DataCategory.UNIT;
            case OBJECTGROUP:
                return DataCategory.OBJECTGROUP;
            default:
                throw new IllegalStateException("Unsupported metadata type " + metadataType);
        }
    }

    private void loadFileInfoFromLogbook(String id, EvidenceAuditParameters auditParameters)
        throws EvidenceAuditException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            JsonNode result = client.selectOperationById(id);

            String detailData = result
                .get(TAG_RESULTS)
                .get(0)
                .get(LogbookMongoDbName.eventDetailData.getDbname())
                .asText();
            JsonNode nodeEvDetData = JsonHandler.getFromString(detailData);

            auditParameters.setFileName(nodeEvDetData.get(FILE_NAME).asText());

            String digestText = nodeEvDetData.get(DIGEST_ALGORITHM).asText();

            DigestType digestType = DigestType.valueOf(digestText);
            auditParameters.setDigestType(digestType);

            String digestValue = nodeEvDetData.get(DIGEST).asText();
            auditParameters.setFileDigest(digestValue);
        } catch (InvalidParseOperationException | LogbookClientException e) {
            throw new EvidenceAuditException(
                EvidenceStatus.FATAL,
                "Could not retrieve logbook operation information",
                e
            );
        }
    }

    private String generateDigest(JsonNode jsonNode, DigestType digestType) {
        final Digest digest = new Digest(digestType);
        digest.update(CanonicalJsonFormatter.serializeToByteArray(jsonNode));
        return digest.digest64();
    }

    /**
     * Creates an instance of StoredInfoResult from metadata json _storage field
     *
     * @param metadataJsonNode input metadata JsonNode for the complete collection Tree
     * @param storageStrategies list of strategies deployed in storage engine
     * @return an object of type StoredInfoResult for wrapping basic storage info
     * @throws IllegalArgumentException thrown if storage info can't be parsed successfully
     */
    private StoredInfoResult fromMetadataJson(JsonNode metadataJsonNode, List<StorageStrategy> storageStrategies)
        throws IllegalArgumentException {
        StoredInfoResult metadataOptimisticBasicStorageInfos = new StoredInfoResult();

        JsonNode storageNode = metadataJsonNode.get(SedaConstants.STORAGE);

        if (storageNode != null && storageNode.isObject()) {
            JsonNode strategy = storageNode.get(SedaConstants.STRATEGY_ID);

            if (strategy == null || strategy.asText().isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("no strategy found in or jsonNode in '%s'", storageNode)
                );
            }

            List<String> offersIds = null;
            try {
                offersIds = StorageStrategyUtils.loadOfferIds(strategy.asText(), storageStrategies);
            } catch (StorageStrategyNotFoundException e) {
                throw new IllegalArgumentException(
                    String.format("strategy '%s' not found in storage engine", strategy.asText())
                );
            }

            if (offersIds == null || offersIds.isEmpty()) {
                throw new IllegalArgumentException(
                    String.format("no active OfferIds found in storage strategy '%s'", strategy.asText())
                );
            }
            metadataOptimisticBasicStorageInfos.setOfferIds(offersIds);
            metadataOptimisticBasicStorageInfos.setStrategy(strategy.asText());
        }

        return metadataOptimisticBasicStorageInfos;
    }
}
