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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.LifeCycleTraceabilitySecureFileObject;
import fr.gouv.vitam.common.model.ObjectGroupDocumentHash;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.TraceabilityEvent;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.common.traceability.TimeStampService;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyNotFoundException;
import fr.gouv.vitam.storage.engine.common.utils.StorageStrategyUtils;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineGenericIterator;
import fr.gouv.vitam.worker.core.exception.ProcessingStatusException;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.lfc_traceability.BuildTraceabilityActionPlugin;
import fr.gouv.vitam.worker.core.plugin.preservation.PreservationGenerateBinaryHash;
import fr.gouv.vitam.worker.core.plugin.preservation.PreservationStorageBinaryPlugin;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.OperationTraceabilityFilesBuilder;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationWithClosestPreviousOperation;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeCheck;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeOperation;
import fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ProbativeReportEntry;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.model.MetadataType.OBJECTGROUP;
import static fr.gouv.vitam.common.model.RequestResponseOK.TAG_RESULTS;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.currentHash;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.previousTimestampToken;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.previousTimestampTokenMinusOneMonth;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.previousTimestampTokenMinusOneYear;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.LOGBOOK_TRACEABILITY;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.OBJECTGROUP_LFC_TRACEABILITY;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.LOGBOOK;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.OBJECT;
import static fr.gouv.vitam.worker.core.plugin.BinaryEventData.MESSAGE_DIGEST;
import static fr.gouv.vitam.worker.core.plugin.CheckConformityActionPlugin.CALC_CHECK;
import static fr.gouv.vitam.worker.core.plugin.StoreObjectGroupActionPlugin.STORING_OBJECT_TASK_ID;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.EVENTS_OBJECT_GROUP_DIGEST_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.EVENTS_OPERATION_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.FILE_DIGEST_LFC_DATABASE_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.FILE_DIGEST_OFFER_DATABASE_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.MERKLE_OBJECT_GROUP_DIGEST_COMPUTATION_ADDITIONAL_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.MERKLE_OBJECT_GROUP_DIGEST_COMPUTATION_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.MERKLE_OBJECT_GROUP_DIGEST_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.MERKLE_OPERATION_DIGEST_COMPUTATION_ADDITIONAL_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.MERKLE_OPERATION_DIGEST_COMPUTATION_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.MERKLE_OPERATION_DIGEST_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.TIMESTAMP_OBJECT_GROUP_COMPUTATION_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.TIMESTAMP_OPERATION_COMPUTATION_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.ChecksInformation.TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.OperationTraceabilityFilesBuilder.anOperationTraceabilityFiles;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_ADDITIONAL_INFORMATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_COMPUTING_INFORMATION;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_DATA;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_FILES_COMPLETE;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_GENERAL_CHECKS;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_GENERAL_CHECKS_COMPLETE;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_MERKLE_TREE;
import static fr.gouv.vitam.worker.core.plugin.probativevalue.pojo.OperationTraceabilityFiles.TRACEABILITY_TOKEN;
import static fr.gouv.vitam.worker.core.plugin.traceability.VerifyMerkleTreeActionHandler.computeMerkleTree;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

public class ProbativeCreateReportEntry extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeCreateReportEntry.class);
    private static final String HANDLER_ID = "PROBATIVE_VALUE_CREATE_PROBATIVE_REPORT_ENTRY";

    public static final String NO_BINARY_ID = "NO_BINARY_ID";
    private static final int STRATEGIES_IN_RANK = 0;

    private static final TypeReference<List<LogbookOperation>> OPERATIONS_TYPE = new TypeReference<
        List<LogbookOperation>
    >() {};
    private static final TypeReference<LogbookOperation> OPERATION_TYPE = new TypeReference<LogbookOperation>() {};
    private static final TypeReference<LifeCycleTraceabilitySecureFileObject> LIFECYCLE_TYPE = new TypeReference<
        LifeCycleTraceabilitySecureFileObject
    >() {};
    private static final TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {};
    private static final TypeReference<List<ProbativeCheck>> LIST_PROBATIVE = new TypeReference<
        List<ProbativeCheck>
    >() {};

    private final MetaDataClientFactory metaDataClientFactory;
    private final LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;
    private final StorageClientFactory storageClientFactory;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;
    private final TimeStampService timeStampService;

    @VisibleForTesting
    public ProbativeCreateReportEntry(
        MetaDataClientFactory metaDataClientFactory,
        LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory,
        StorageClientFactory storageClientFactory,
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        TimeStampService timeStampService
    ) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.logbookLifeCyclesClientFactory = logbookLifeCyclesClientFactory;
        this.storageClientFactory = storageClientFactory;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.timeStampService = timeStampService;
    }

    public ProbativeCreateReportEntry() {
        this(
            MetaDataClientFactory.getInstance(),
            LogbookLifeCyclesClientFactory.getInstance(),
            StorageClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            new TimeStampService()
        );
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        String startEntryCreation = LocalDateUtil.nowFormatted();
        String objectGroupId = param.getObjectName();
        String usageVersion = param.getObjectMetadata().get("usageVersion").asText();

        try (
            MetaDataClient metaDataClient = metaDataClientFactory.getClient();
            LogbookLifeCyclesClient logbookLifeCyclesClient = logbookLifeCyclesClientFactory.getClient();
            StorageClient storageClient = storageClientFactory.getClient();
            LogbookOperationsClient logbookOperationsClient = logbookOperationsClientFactory.getClient()
        ) {
            List<String> unitIds = JsonHandler.getFromJsonNode(param.getObjectMetadata().get("unitIds"), LIST_STRING);

            RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupByIdRaw(objectGroupId);
            if (!requestResponse.isOk()) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(startEntryCreation, unitIds, objectGroupId, NO_BINARY_ID, usageVersion)
                );
                return buildItemStatus(
                    HANDLER_ID,
                    FATAL,
                    EventDetails.of(String.format("Cannot retrieve metadata for GOT %s.", objectGroupId))
                );
            }

            Optional<DbVersionsModel> versionOptional = getVersion(usageVersion, requestResponse);
            if (!versionOptional.isPresent()) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(startEntryCreation, unitIds, objectGroupId, NO_BINARY_ID, usageVersion)
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found version for GOT %s and with VERSION %s.",
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            List<StorageStrategy> storageStrategies = loadStorageStrategies(handler);
            DbVersionsModel dbVersionsModel = versionOptional.get();
            String strategyId = dbVersionsModel.getStorage().getStrategyId();
            List<String> offerIds = StorageStrategyUtils.loadOfferIds(strategyId, storageStrategies);
            List<String> offerDigests = getOfferDigests(storageClient, dbVersionsModel.getId(), strategyId, offerIds);
            if (offerDigests.isEmpty() || offerIds.size() != offerDigests.size()) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(
                        startEntryCreation,
                        unitIds,
                        objectGroupId,
                        dbVersionsModel.getId(),
                        usageVersion
                    )
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found storage offer digest for GOT %s and with VERSION %s.",
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            JsonNode rawLogbookObjectGroupLFC = logbookLifeCyclesClient.getRawObjectGroupLifeCycleById(objectGroupId);
            LogbookLifecycle logbookObjectGroupLFC = JsonHandler.getFromJsonNode(
                rawLogbookObjectGroupLFC,
                LogbookLifecycle.class
            );
            Set<String> lifecycleDigests = getLifeCycleDigests(logbookObjectGroupLFC, dbVersionsModel);
            if (lifecycleDigests.isEmpty()) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(
                        startEntryCreation,
                        unitIds,
                        objectGroupId,
                        dbVersionsModel.getId(),
                        usageVersion
                    )
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found lifecycle offer digest for GOT %s and with VERSION %s.",
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            JsonNode logbookOperationVersionModelResult = logbookOperationsClient.selectOperationById(
                dbVersionsModel.getOpi()
            );
            RequestResponseOK<JsonNode> logbookOperationVersionModelResponseOK = RequestResponseOK.getFromJsonNode(
                logbookOperationVersionModelResult
            );
            LogbookOperation logbookOperationVersionModel = JsonHandler.getFromJsonNode(
                logbookOperationVersionModelResponseOK.getFirstResult(),
                LogbookOperation.class
            );

            String objectGroupLFCLastPersistedDate = getObjectGroupLFCLastPersistedDate(
                logbookObjectGroupLFC,
                dbVersionsModel
            );
            List<LogbookOperation> traceabilityOperations = getTraceabilityLogbookOperation(
                logbookOperationsClient,
                logbookOperationVersionModel.getLastPersistedDate(),
                objectGroupLFCLastPersistedDate
            );
            if (traceabilityOperations.isEmpty()) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(
                        startEntryCreation,
                        unitIds,
                        objectGroupId,
                        dbVersionsModel.getId(),
                        usageVersion
                    )
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found traceability logbook operation id for GOT %s and with VERSION %s.",
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            List<OperationWithClosestPreviousOperation> operationsAndClosestPreviousOperations = traceabilityOperations
                .stream()
                .map(op -> getTraceabilityOperation(logbookOperationsClient, op))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
            if (operationsAndClosestPreviousOperations.isEmpty()) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(
                        startEntryCreation,
                        unitIds,
                        objectGroupId,
                        dbVersionsModel.getId(),
                        usageVersion
                    )
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found all traceability logbook operation ids for GOT %s and with VERSION %s.",
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            List<ProbativeOperation> probativeOperations = operationsAndClosestPreviousOperations
                .stream()
                .map(op -> logbookOperationTo(op.getOperation()))
                .collect(Collectors.toList());

            ProbativeOperation probativeOperationIngest = logbookOperationTo(logbookOperationVersionModel);
            probativeOperations.add(probativeOperationIngest);

            Map<String, Optional<OperationTraceabilityFiles>> traceabilityFiles = operationsAndClosestPreviousOperations
                .stream()
                .collect(
                    Collectors.toMap(
                        op -> op.getOperation().getEvType(),
                        op -> getTraceabilityFile(op.getOperation(), storageClient, handler, objectGroupId)
                    )
                );
            if (
                traceabilityFiles.size() != traceabilityOperations.size() ||
                !traceabilityFiles.values().stream().allMatch(Optional::isPresent)
            ) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(
                        startEntryCreation,
                        unitIds,
                        objectGroupId,
                        dbVersionsModel.getId(),
                        usageVersion,
                        probativeOperations
                    )
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found traceability logbook operation files for GOT %s and with VERSION %s.",
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            List<ProbativeCheck> probativeChecks = operationsAndClosestPreviousOperations
                .stream()
                .flatMap(
                    operationWithClosestPreviousOperation ->
                        doChecks(
                            traceabilityFiles
                                .get(operationWithClosestPreviousOperation.getOperation().getEvType())
                                .get(),
                            operationWithClosestPreviousOperation,
                            dbVersionsModel,
                            rawLogbookObjectGroupLFC,
                            logbookOperationVersionModel,
                            handler
                        )
                )
                .collect(Collectors.toList());

            ProbativeCheck databaseOfferFileDigestCheck = fileDigestComparison(
                FILE_DIGEST_OFFER_DATABASE_COMPARISON,
                dbVersionsModel.getMessageDigest(),
                new HashSet<>(offerDigests)
            );
            ProbativeCheck databaseGotLfcFileDigestCheck = fileDigestComparison(
                FILE_DIGEST_LFC_DATABASE_COMPARISON,
                dbVersionsModel.getMessageDigest(),
                lifecycleDigests
            );
            probativeChecks.add(databaseOfferFileDigestCheck);
            probativeChecks.add(databaseGotLfcFileDigestCheck);
            if (probativeChecks.size() != ChecksInformation.values().length) {
                transferReportEntryToWorkspace(
                    handler,
                    objectGroupId,
                    ProbativeReportEntry.koFrom(
                        startEntryCreation,
                        unitIds,
                        objectGroupId,
                        dbVersionsModel.getId(),
                        usageVersion,
                        probativeOperations,
                        probativeChecks
                    )
                );
                return buildItemStatus(
                    HANDLER_ID,
                    KO,
                    EventDetails.of(
                        String.format(
                            "Cannot found ALL %s checks for GOT %s and with VERSION %s.",
                            ChecksInformation.values().length,
                            objectGroupId,
                            usageVersion
                        )
                    )
                );
            }

            ProbativeReportEntry probativeReportEntry = new ProbativeReportEntry(
                startEntryCreation,
                unitIds,
                objectGroupId,
                dbVersionsModel.getId(),
                usageVersion,
                probativeOperations,
                probativeChecks
            );
            transferReportEntryToWorkspace(handler, objectGroupId, probativeReportEntry);

            return buildItemStatus(
                HANDLER_ID,
                OK,
                EventDetails.of(
                    String.format("Entry build for GOT %s and with VERSION %s.", objectGroupId, usageVersion)
                )
            );
        } catch (StorageStrategyNotFoundException | ProcessingStatusException e) {
            LOGGER.error(e);
            tryTransferReportEntryToWorkspace(
                handler,
                objectGroupId,
                ProbativeReportEntry.koFrom(
                    startEntryCreation,
                    Collections.emptyList(),
                    objectGroupId,
                    NO_BINARY_ID,
                    usageVersion
                )
            );
            return buildItemStatus(
                HANDLER_ID,
                FATAL,
                EventDetails.of(String.format("Error while using storage strategies for GOT %s.", objectGroupId))
            );
        } catch (Exception e) {
            LOGGER.error(e);
            tryTransferReportEntryToWorkspace(
                handler,
                objectGroupId,
                ProbativeReportEntry.koFrom(
                    startEntryCreation,
                    Collections.emptyList(),
                    objectGroupId,
                    NO_BINARY_ID,
                    usageVersion
                )
            );
            return buildItemStatus(
                HANDLER_ID,
                KO,
                EventDetails.of(
                    String.format(
                        "Error while building probative value for GOT %s and with VERSION %s.",
                        objectGroupId,
                        usageVersion
                    )
                )
            );
        }
    }

    private String getObjectGroupLFCLastPersistedDate(
        LogbookLifecycle logbookObjectGroupLFC,
        DbVersionsModel dbVersionsModel
    ) {
        return logbookObjectGroupLFC
            .getEvents()
            .stream()
            .filter(this::isOKEvent)
            .filter(event -> event.getOutDetail().contains(STORING_OBJECT_TASK_ID))
            .filter(e -> e.getObId().equals(dbVersionsModel.getId()))
            .map(LogbookEvent::getLastPersistedDate)
            .findFirst()
            .orElse(null);
    }

    private ProbativeOperation logbookOperationTo(LogbookOperation op) {
        return new ProbativeOperation(
            op.getId(),
            op.getEvType(),
            op.getEvIdAppSession(),
            op.getRightsStatementIdentifier(),
            op.getAgIdApp(),
            op.getEvDateTime()
        );
    }

    private void tryTransferReportEntryToWorkspace(
        HandlerIO handler,
        String objectGroupId,
        ProbativeReportEntry probativeReportEntry
    ) throws ProcessingException {
        try {
            transferReportEntryToWorkspace(handler, objectGroupId, probativeReportEntry);
        } catch (Exception e) {
            throw new ProcessingException(e);
        }
    }

    private void transferReportEntryToWorkspace(
        HandlerIO handler,
        String objectGroupId,
        ProbativeReportEntry probativeReportEntry
    ) throws InvalidParseOperationException, ProcessingException {
        File newLocalFile = handler.getNewLocalFile(objectGroupId);
        JsonHandler.writeAsFile(probativeReportEntry, newLocalFile);
        handler.transferFileToWorkspace(objectGroupId, newLocalFile, true, false);
    }

    private Stream<ProbativeCheck> doChecks(
        OperationTraceabilityFiles traceabilityFiles,
        OperationWithClosestPreviousOperation traceabilityLogbookOperations,
        DbVersionsModel objectModel,
        JsonNode logbookObjectGroupLFC,
        LogbookOperation logbookOperationVersionModel,
        HandlerIO handler
    ) {
        String evType = traceabilityLogbookOperations.getOperation().getEvType();
        if (OBJECTGROUP_LFC_TRACEABILITY.getEventType().equals(evType)) {
            return doObjectGroupChecks(
                traceabilityFiles,
                traceabilityLogbookOperations,
                logbookObjectGroupLFC,
                objectModel,
                handler
            );
        }
        if (LOGBOOK_TRACEABILITY.getEventType().equals(evType)) {
            return doOperationChecks(
                traceabilityFiles,
                traceabilityLogbookOperations,
                logbookOperationVersionModel,
                objectModel,
                handler
            );
        }
        throw new IllegalArgumentException(String.format("unknown operationKey %s", evType));
    }

    private Stream<ProbativeCheck> doOperationChecks(
        OperationTraceabilityFiles traceabilityFiles,
        OperationWithClosestPreviousOperation traceabilityLogbookOperations,
        LogbookOperation logbookOperationVersionModel,
        DbVersionsModel objectModel,
        HandlerIO handler
    ) {
        LogbookOperation traceabilityLogbookOperation = traceabilityLogbookOperations.getOperation();
        LogbookOperation closestTraceabilityLogbookOperation =
            traceabilityLogbookOperations.getClosestToReferenceOperation();

        try (
            InputStream secureData = new FileInputStream(traceabilityFiles.getData());
            JsonLineGenericIterator<LogbookOperation> securedOperations = new JsonLineGenericIterator<>(
                secureData,
                OPERATION_TYPE
            );
            InputStream computingInformation = new FileInputStream(traceabilityFiles.getComputingInformation())
        ) {
            LogbookOperation logbookOperationSecured = securedOperations
                .stream()
                .filter(op -> op.getId().equals(logbookOperationVersionModel.getId()))
                .findFirst()
                .orElse(new LogbookOperation());

            ProbativeCheck specificCheck = ProbativeCheck.from(
                EVENTS_OPERATION_DATABASE_TRACEABILITY_COMPARISON,
                logbookOperationVersionModel.getId(),
                logbookOperationSecured.getId(),
                logbookOperationVersionModel.equals(logbookOperationSecured) ? OK : KO
            );

            Stream<ProbativeCheck> generalChecks = getGeneralCheck(
                TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION,
                TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON,
                PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_VALIDATION,
                PREVIOUS_TIMESTAMP_OPERATION_DATABASE_TRACEABILITY_COMPARISON,
                MERKLE_OPERATION_DIGEST_DATABASE_TRACEABILITY_COMPARISON,
                MERKLE_OPERATION_DIGEST_COMPUTATION_TRACEABILITY_COMPARISON,
                MERKLE_OPERATION_DIGEST_COMPUTATION_ADDITIONAL_TRACEABILITY_COMPARISON,
                TIMESTAMP_OPERATION_COMPUTATION_TRACEABILITY_COMPARISON,
                traceabilityLogbookOperation,
                traceabilityFiles,
                computingInformation,
                closestTraceabilityLogbookOperation,
                handler,
                objectModel.getDataObjectGroupId(),
                traceabilityLogbookOperation.getEvType()
            );

            return Stream.concat(generalChecks, Stream.of(specificCheck));
        } catch (Exception e) {
            LOGGER.warn(e);
            return Stream.empty();
        }
    }

    private boolean isEventBeforeInSecured(LogbookOperation traceabilityLogbookOperation, JsonNode event) {
        LocalDateTime eventDateTime = LocalDateTime.parse(event.get("evDateTime").asText());
        LocalDateTime traceabilityDateTime = LocalDateTime.parse(traceabilityLogbookOperation.getEvDateTime());
        return eventDateTime.isBefore(traceabilityDateTime) || eventDateTime.isEqual(traceabilityDateTime);
    }

    private Stream<ProbativeCheck> doObjectGroupChecks(
        OperationTraceabilityFiles traceabilityFiles,
        OperationWithClosestPreviousOperation traceabilityLogbookOperations,
        JsonNode logbookObjectGroupLFC,
        DbVersionsModel objectModel,
        HandlerIO handler
    ) {
        LogbookOperation traceabilityLogbookOperation = traceabilityLogbookOperations.getOperation();
        LogbookOperation closestTraceabilityLogbookOperation =
            traceabilityLogbookOperations.getClosestToReferenceOperation();

        try (
            InputStream secureData = new FileInputStream(traceabilityFiles.getData());
            JsonLineGenericIterator<LifeCycleTraceabilitySecureFileObject> objectGroupsSecured =
                new JsonLineGenericIterator<>(secureData, LIFECYCLE_TYPE);
            InputStream computingInformation = new FileInputStream(traceabilityFiles.getComputingInformation())
        ) {
            List<JsonNode> events = StreamSupport.stream(logbookObjectGroupLFC.get("events").spliterator(), false)
                .filter(jsonNode -> isEventBeforeInSecured(traceabilityLogbookOperation, jsonNode))
                .collect(Collectors.toList());

            String digestFromDatabase = BuildTraceabilityActionPlugin.generateDigest(
                JsonHandler.toJsonNode(events),
                VitamConfiguration.getDefaultDigestType()
            );
            List<LifeCycleTraceabilitySecureFileObject> lifeCycleTraceabilitySecureFileObjectStream =
                objectGroupsSecured
                    .stream()
                    .filter(
                        l ->
                            Objects.nonNull(l) &&
                            OBJECTGROUP.equals(l.getMetadataType()) &&
                            objectModel.getDataObjectGroupId().equalsIgnoreCase(l.getLfcId())
                    )
                    .collect(Collectors.toList());

            String objectDigestFromSecuredFile = lifeCycleTraceabilitySecureFileObjectStream
                .stream()
                .flatMap(line -> line.getObjectGroupDocumentHashList().stream())
                .filter(o -> o.getId().equals(objectModel.getId()))
                .map(ObjectGroupDocumentHash::gethObject)
                .findFirst()
                .orElse("NO_DIGEST_FOUND");

            String eventDigestFromSecuredFile = lifeCycleTraceabilitySecureFileObjectStream
                .stream()
                .map(LifeCycleTraceabilitySecureFileObject::getHashLFCEvents)
                .findFirst()
                .orElse("NO_DIGEST_FOUND");

            ProbativeCheck specificCheck = ProbativeCheck.from(
                ChecksInformation.FILE_DIGEST_DATABASE_TRACEABILITY_COMPARISON,
                objectModel.getMessageDigest(),
                objectDigestFromSecuredFile,
                objectDigestFromSecuredFile.equals(objectModel.getMessageDigest()) ? OK : KO
            );

            ProbativeCheck specificCheckEvent = ProbativeCheck.from(
                EVENTS_OBJECT_GROUP_DIGEST_DATABASE_TRACEABILITY_COMPARISON,
                digestFromDatabase,
                eventDigestFromSecuredFile,
                eventDigestFromSecuredFile.equals(digestFromDatabase) ? OK : KO
            );

            Stream<ProbativeCheck> generalChecks = getGeneralCheck(
                TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION,
                TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON,
                PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_VALIDATION,
                PREVIOUS_TIMESTAMP_OBJECT_GROUP_DATABASE_TRACEABILITY_COMPARISON,
                MERKLE_OBJECT_GROUP_DIGEST_DATABASE_TRACEABILITY_COMPARISON,
                MERKLE_OBJECT_GROUP_DIGEST_COMPUTATION_TRACEABILITY_COMPARISON,
                MERKLE_OBJECT_GROUP_DIGEST_COMPUTATION_ADDITIONAL_TRACEABILITY_COMPARISON,
                TIMESTAMP_OBJECT_GROUP_COMPUTATION_TRACEABILITY_COMPARISON,
                traceabilityLogbookOperation,
                traceabilityFiles,
                computingInformation,
                closestTraceabilityLogbookOperation,
                handler,
                objectModel.getDataObjectGroupId(),
                traceabilityLogbookOperation.getEvType()
            );

            return Stream.concat(generalChecks, Stream.of(specificCheck, specificCheckEvent));
        } catch (Exception e) {
            LOGGER.warn(e);
            return Stream.empty();
        }
    }

    private Stream<ProbativeCheck> getGeneralCheck(
        ChecksInformation timeStampValidation,
        ChecksInformation timeStampComparison,
        ChecksInformation previousTimeStampValidation,
        ChecksInformation previousTimeStampComparison,
        ChecksInformation checkMerkleTreeInformation,
        ChecksInformation checkMerkleTreeInformationComputed,
        ChecksInformation checkMerkleComputingInformation,
        ChecksInformation checkComputedTimestamp,
        LogbookOperation traceabilityLogbookOperation,
        OperationTraceabilityFiles traceabilityFiles,
        InputStream computingInformation,
        LogbookOperation closestTraceabilityLogbookOperation,
        HandlerIO handlerIO,
        String objectGroupId,
        String evType
    ) throws Exception {
        List<ProbativeCheck> generalChecksFromWorkspace = getGeneralChecksFromWorkspace(
            handlerIO,
            objectGroupId,
            evType
        );
        if (!generalChecksFromWorkspace.isEmpty()) {
            return generalChecksFromWorkspace.stream();
        }

        String timeStampFromTraceabilityFile = new String(Files.readAllBytes(traceabilityFiles.getToken().toPath()));
        String timeStampFromLogbookOperation = getTimeStampFromLogbookOperation(traceabilityLogbookOperation);
        ProbativeCheck validateTimeStamp = validateTimeStamp(
            timeStampValidation,
            timeStampFromTraceabilityFile,
            timeStampFromLogbookOperation
        );
        ProbativeCheck compareTimeStamp = compare(
            timeStampComparison,
            timeStampFromTraceabilityFile,
            timeStampFromLogbookOperation
        );

        String digestFromDatabase = JsonHandler.getFromString(
            traceabilityLogbookOperation.getEvDetData(),
            TraceabilityEvent.class
        ).getHash();
        MerkleTreeAlgo merkleTreeAlgo = computeMerkleTree(
            new FileInputStream(traceabilityFiles.getData()),
            VitamConfiguration.getDefaultDigestType()
        );
        String digestRecalculated = BaseXx.getBase64(merkleTreeAlgo.generateMerkle().getRoot());

        String traceabilityMerkleFileMerkleTreeRootDigest = getTraceabilityMerkleFileMerkleTreeRootDigest(
            traceabilityFiles.getMerkleTree()
        );

        ProbativeCheck checkFromMerkleTree = compare(
            checkMerkleTreeInformation,
            traceabilityMerkleFileMerkleTreeRootDigest,
            digestFromDatabase
        );
        ProbativeCheck checkFromMerkleTreeComputed = compare(
            checkMerkleTreeInformationComputed,
            traceabilityMerkleFileMerkleTreeRootDigest,
            digestRecalculated
        );

        Properties computingProperties = new Properties();
        computingProperties.load(computingInformation);

        ProbativeCheck merkleDigestInAdditionalInformation = compare(
            checkMerkleComputingInformation,
            digestRecalculated,
            computingProperties.getProperty(currentHash)
        );
        ProbativeCheck computedTimeStampComparison = computeAndCompareTimeStamp(
            checkComputedTimestamp,
            timeStampFromTraceabilityFile,
            computingProperties
        );

        String previousTimeStampFromTraceabilityFile = computingProperties.getProperty(previousTimestampToken);
        if (previousTimeStampFromTraceabilityFile == null || previousTimeStampFromTraceabilityFile.equals("null")) {
            List<ProbativeCheck> probativeChecks = Arrays.asList(
                validateTimeStamp,
                compareTimeStamp,
                checkFromMerkleTree,
                checkFromMerkleTreeComputed,
                merkleDigestInAdditionalInformation,
                computedTimeStampComparison,
                ProbativeCheck.warnFrom(
                    previousTimeStampValidation,
                    "No previous secured file.",
                    "No previous secured file."
                ),
                ProbativeCheck.warnFrom(
                    previousTimeStampComparison,
                    "No previous secured file.",
                    "No previous secured file."
                )
            );
            transferGeneralChecksToWorkspace(handlerIO, objectGroupId, evType, probativeChecks);
            return probativeChecks.stream();
        }

        String previousTimeStampFromLogbookOperation = getTimeStampFromLogbookOperation(
            closestTraceabilityLogbookOperation
        );
        ProbativeCheck validatePreviousTimeStamp = validateTimeStamp(
            previousTimeStampValidation,
            previousTimeStampFromTraceabilityFile,
            previousTimeStampFromLogbookOperation
        );
        ProbativeCheck comparePreviousTimeStamp = compare(
            previousTimeStampComparison,
            previousTimeStampFromTraceabilityFile,
            previousTimeStampFromLogbookOperation
        );

        List<ProbativeCheck> probativeChecks = Arrays.asList(
            validateTimeStamp,
            compareTimeStamp,
            checkFromMerkleTree,
            checkFromMerkleTreeComputed,
            merkleDigestInAdditionalInformation,
            validatePreviousTimeStamp,
            computedTimeStampComparison,
            comparePreviousTimeStamp
        );
        transferGeneralChecksToWorkspace(handlerIO, objectGroupId, evType, probativeChecks);
        return probativeChecks.stream();
    }

    private ProbativeCheck computeAndCompareTimeStamp(
        ChecksInformation checkComputedTimestamp,
        String timeStampFromTraceabilityFile,
        Properties computingProperties
    ) {
        try {
            byte[] prevTimeStampToken = timeStampService.getDigestAsBytes(
                computingProperties.getProperty(previousTimestampToken)
            );
            byte[] prevTimestampTokenMinusOneMonth = timeStampService.getDigestAsBytes(
                computingProperties.getProperty(previousTimestampTokenMinusOneMonth)
            );
            byte[] prevTimestampTokenMinusOneYear = timeStampService.getDigestAsBytes(
                computingProperties.getProperty(previousTimestampTokenMinusOneYear)
            );
            byte[] rootMerkleTree = timeStampService.getDigestAsBytes(computingProperties.getProperty(currentHash));

            TimeStampToken timeStampToken = timeStampService.getTimeStampFrom(timeStampFromTraceabilityFile);

            byte[] timeStampDataFromFile = timeStampToken.getTimeStampInfo().getMessageImprintDigest();
            byte[] computedTimeStampData = timeStampService.getDigestFrom(
                rootMerkleTree,
                prevTimeStampToken,
                prevTimestampTokenMinusOneMonth,
                prevTimestampTokenMinusOneYear
            );

            return ProbativeCheck.from(
                checkComputedTimestamp,
                BaseXx.getBase64(timeStampDataFromFile),
                BaseXx.getBase64(computedTimeStampData),
                Arrays.equals(timeStampDataFromFile, computedTimeStampData) ? OK : KO
            );
        } catch (Exception e) {
            LOGGER.warn(e);
            return ProbativeCheck.koFrom(
                checkComputedTimestamp,
                timeStampFromTraceabilityFile,
                "NO_COMPUTED_TIMESTAMP_DATA"
            );
        }
    }

    private List<ProbativeCheck> getGeneralChecksFromWorkspace(
        HandlerIO handlerIO,
        String objectGroupId,
        String evType
    ) {
        if (isTraceabilityFilesAbsent(handlerIO, objectGroupId, evType, TRACEABILITY_GENERAL_CHECKS_COMPLETE)) {
            return Collections.emptyList();
        }

        try {
            File fileFromWorkspace = handlerIO.getFileFromWorkspace(
                traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + TRACEABILITY_GENERAL_CHECKS
            );
            return JsonHandler.getFromFileAsTypeReference(fileFromWorkspace, LIST_PROBATIVE);
        } catch (Exception e) {
            LOGGER.warn(e);
            return Collections.emptyList();
        }
    }

    private void transferGeneralChecksToWorkspace(
        HandlerIO handlerIO,
        String objectGroupId,
        String evType,
        List<ProbativeCheck> probativeChecks
    ) throws InvalidParseOperationException, ProcessingException, IOException {
        File sourceFile = File.createTempFile("tmp", null, new File(VitamConfiguration.getVitamTmpFolder()));
        JsonHandler.writeAsFile(probativeChecks, sourceFile);
        handlerIO.transferFileToWorkspace(
            traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + TRACEABILITY_GENERAL_CHECKS,
            sourceFile,
            false,
            false
        );
        handlerIO.transferInputStreamToWorkspace(
            traceabilityFilesDirectoryName(objectGroupId, evType) +
            File.separator +
            TRACEABILITY_GENERAL_CHECKS_COMPLETE,
            new ByteArrayInputStream(new byte[0]),
            null,
            false
        );
    }

    private ProbativeCheck compare(ChecksInformation information, String digest, String otherDigest) {
        return ProbativeCheck.from(information, otherDigest, digest, otherDigest.equals(digest) ? OK : KO);
    }

    private ProbativeCheck validateTimeStamp(
        ChecksInformation information,
        String timeStampFromTraceabilityFile,
        String timeStampFromLogbookOperation
    ) {
        return ProbativeCheck.from(
            information,
            timeStampFromLogbookOperation,
            timeStampFromTraceabilityFile,
            isTimeStampValid(timeStampFromLogbookOperation) && isTimeStampValid(timeStampFromTraceabilityFile) ? OK : KO
        );
    }

    private String getTimeStampFromLogbookOperation(LogbookOperation traceabilityLogbookOperation)
        throws InvalidParseOperationException {
        String evDetData = traceabilityLogbookOperation.getEvDetData();
        JsonNode eventDetail = JsonHandler.getFromString(evDetData);
        TraceabilityEvent traceabilityEvent = JsonHandler.getFromJsonNode(eventDetail, TraceabilityEvent.class);
        return BaseXx.getBase64(traceabilityEvent.getTimeStampToken());
    }

    private Optional<OperationTraceabilityFiles> getTraceabilityFile(
        LogbookOperation logbookEventOperation,
        StorageClient storageClient,
        HandlerIO handlerIO,
        String objectGroupId
    ) {
        Response response = null;
        try {
            TraceabilityEvent traceabilityEvent = JsonHandler.getFromString(
                logbookEventOperation.getEvDetData(),
                TraceabilityEvent.class
            );
            String evType = logbookEventOperation.getEvType();

            Optional<OperationTraceabilityFiles> operationTraceabilityFilesFromWorkspace =
                tryGetOperationTraceabilityFileFromWorkspace(handlerIO, objectGroupId, evType);
            if (operationTraceabilityFilesFromWorkspace.isPresent()) {
                return operationTraceabilityFilesFromWorkspace;
            }

            response = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                traceabilityEvent.getFileName(),
                LOGBOOK,
                AccessLogUtils.getNoLogAccessLog()
            );
            return Optional.of(extractZipFiles(response, handlerIO, objectGroupId, evType));
        } catch (Exception e) {
            LOGGER.error(e);
            return Optional.empty();
        } finally {
            StreamUtils.consumeAnyEntityAndClose(response);
        }
    }

    private Optional<OperationTraceabilityFiles> tryGetOperationTraceabilityFileFromWorkspace(
        HandlerIO handlerIO,
        String objectGroupId,
        String evType
    ) {
        if (isTraceabilityFilesAbsent(handlerIO, objectGroupId, evType, TRACEABILITY_FILES_COMPLETE)) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                new OperationTraceabilityFiles(
                    handlerIO.getFileFromWorkspace(
                        traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + TRACEABILITY_DATA
                    ),
                    handlerIO.getFileFromWorkspace(
                        traceabilityFilesDirectoryName(objectGroupId, evType) +
                        File.separator +
                        TRACEABILITY_MERKLE_TREE
                    ),
                    handlerIO.getFileFromWorkspace(
                        traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + TRACEABILITY_TOKEN
                    ),
                    handlerIO.getFileFromWorkspace(
                        traceabilityFilesDirectoryName(objectGroupId, evType) +
                        File.separator +
                        TRACEABILITY_COMPUTING_INFORMATION
                    ),
                    handlerIO.getFileFromWorkspace(
                        traceabilityFilesDirectoryName(objectGroupId, evType) +
                        File.separator +
                        TRACEABILITY_ADDITIONAL_INFORMATION
                    )
                )
            );
        } catch (Exception e) {
            LOGGER.warn(e);
            return Optional.empty();
        }
    }

    private boolean isTraceabilityFilesAbsent(
        HandlerIO handlerIO,
        String objectGroupId,
        String evType,
        String complete
    ) {
        try {
            handlerIO.getFileFromWorkspace(
                traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + complete
            );
            return false;
        } catch (Exception e) {
            LOGGER.warn(e);
            return true;
        }
    }

    private OperationTraceabilityFiles extractZipFiles(
        Response response,
        HandlerIO handlerIO,
        String objectGroupId,
        String evType
    ) throws IOException, ProcessingException {
        OperationTraceabilityFilesBuilder filesBuilder = anOperationTraceabilityFiles();
        try (
            InputStream inputStream = response.readEntity(InputStream.class);
            ZipInputStream zipInputStream = new ZipInputStream(inputStream)
        ) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File file = File.createTempFile("tmp", null, new File(VitamConfiguration.getVitamTmpFolder()));
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    IOUtils.copy(zipInputStream, fileOutputStream);
                }
                filesBuilder.with(entry.getName(), file);
                handlerIO.transferFileToWorkspace(
                    traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + entry.getName(),
                    file,
                    false,
                    false
                );
            }
            handlerIO.transferInputStreamToWorkspace(
                traceabilityFilesDirectoryName(objectGroupId, evType) + File.separator + TRACEABILITY_FILES_COMPLETE,
                new ByteArrayInputStream(new byte[0]),
                null,
                false
            );
        }
        return filesBuilder.build();
    }

    private Optional<OperationWithClosestPreviousOperation> getTraceabilityOperation(
        LogbookOperationsClient logbookOperationsClient,
        LogbookOperation operation
    ) {
        try {
            Select select = new Select();
            BooleanQuery query = and()
                .add(
                    eq(LogbookMongoDbName.eventType.getDbname(), operation.getEvType()),
                    in("events.outDetail", operation.getEvType() + "." + OK, operation.getEvType() + "." + WARNING),
                    exists("events.evDetData.FileName"),
                    ne("#id", operation.getId()),
                    lte(LogbookMongoDbName.eventDateTime.getDbname(), operation.getEvDateTime())
                );

            select.setQuery(query);
            select.setLimitFilter(0, 1);
            select.addOrderByDescFilter("evDateTime");

            JsonNode jsonNode = logbookOperationsClient.selectOperation(select.getFinalSelect());
            List<LogbookOperation> closestToReferenceOperations = JsonHandler.getFromJsonNode(
                jsonNode.get(TAG_RESULTS),
                OPERATIONS_TYPE
            );

            TraceabilityEvent emptyEvent = new TraceabilityEvent();
            emptyEvent.setTimeStampToken("NO_TIMESTAMP".getBytes());
            LogbookOperation closestToReferenceOperation = closestToReferenceOperations
                .stream()
                .findFirst()
                .orElse(LogbookOperation.emptyWithEvDetData(JsonHandler.writeAsString(emptyEvent)));

            return Optional.of(new OperationWithClosestPreviousOperation(operation, closestToReferenceOperation));
        } catch (InvalidParseOperationException | LogbookClientException | InvalidCreateOperationException e) {
            LOGGER.warn(e);
            return Optional.empty();
        }
    }

    private List<LogbookOperation> getTraceabilityLogbookOperation(
        LogbookOperationsClient logbookOperationsClient,
        String ingestEvDate,
        String lastPersistedGOTLFCDate
    ) throws LogbookClientException, InvalidParseOperationException, InvalidCreateOperationException {
        if (StringUtils.isBlank(ingestEvDate) || StringUtils.isBlank(lastPersistedGOTLFCDate)) {
            return Collections.emptyList();
        }
        Optional<LogbookOperation> logbook = getOperationId(
            logbookOperationsClient,
            ingestEvDate,
            LOGBOOK_TRACEABILITY.getEventType()
        );
        Optional<LogbookOperation> objectGroupLFC = getOperationId(
            logbookOperationsClient,
            lastPersistedGOTLFCDate,
            OBJECTGROUP_LFC_TRACEABILITY.getEventType()
        );
        if (logbook.isPresent() && objectGroupLFC.isPresent()) {
            return Arrays.asList(logbook.get(), objectGroupLFC.get());
        }
        return Collections.emptyList();
    }

    private Optional<LogbookOperation> getOperationId(
        LogbookOperationsClient logbookOperationsClient,
        String lastPersistedIngestOperationDate,
        String eventType
    ) throws InvalidCreateOperationException, InvalidParseOperationException, LogbookClientException {
        Select select = new Select();
        BooleanQuery query = and()
            .add(
                eq(LogbookMongoDbName.eventType.getDbname(), eventType),
                in("events.outDetail", eventType + "." + OK, eventType + "." + WARNING),
                exists("events.evDetData.FileName"),
                lte("events.evDetData.StartDate", lastPersistedIngestOperationDate),
                gte("events.evDetData.EndDate", lastPersistedIngestOperationDate)
            );

        select.setQuery(query);
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("events.evDateTime");

        RequestResponseOK<JsonNode> requestResponseOK = RequestResponseOK.getFromJsonNode(
            logbookOperationsClient.selectOperation(select.getFinalSelect())
        );
        if (requestResponseOK.getResults().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(JsonHandler.getFromJsonNode(requestResponseOK.getFirstResult(), LogbookOperation.class));
    }

    private Set<String> getLifeCycleDigests(LogbookLifecycle logbookLifeCycles, DbVersionsModel versionsModel) {
        if (Objects.isNull(logbookLifeCycles) || Objects.isNull(logbookLifeCycles.getEvents())) {
            return Collections.emptySet();
        }

        return logbookLifeCycles
            .getEvents()
            .stream()
            .filter(event -> isBinaryEvent(versionsModel, event))
            .filter(this::isDigestEvent)
            .filter(this::isOKEvent)
            .map(this::toJsonNode)
            .map(evDetData -> getDigest(evDetData, versionsModel.getId()))
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
    }

    private List<String> getOfferDigests(
        StorageClient storageClient,
        String objectGuid,
        String strategyId,
        List<String> offerIds
    ) throws StorageNotFoundClientException, StorageServerClientException {
        JsonNode information = storageClient.getInformation(strategyId, OBJECT, objectGuid, offerIds, false);
        return offerIds
            .stream()
            .map(information::get)
            .filter(Objects::nonNull)
            .filter(jsonNode -> !jsonNode.isMissingNode())
            .filter(jsonNode -> !jsonNode.isNull())
            .map(jsonNode -> jsonNode.get("digest").textValue())
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private ProbativeCheck fileDigestComparison(
        ChecksInformation information,
        String databaseDigest,
        Set<String> otherDigests
    ) {
        if (otherDigests.size() != 1 || !otherDigests.contains(databaseDigest)) {
            return ProbativeCheck.koFrom(information, String.join(", ", otherDigests), databaseDigest);
        }
        return ProbativeCheck.okFrom(information, String.join(", ", otherDigests), databaseDigest);
    }

    private String getDigest(JsonNode evDetData, String id) {
        boolean isDigestFromPreservationEvent =
            evDetData.path(MESSAGE_DIGEST).isNull() || evDetData.path(MESSAGE_DIGEST).isMissingNode();
        return isDigestFromPreservationEvent
            ? getDigestFromPreservation(evDetData, id)
            : getDigestFromIngest(evDetData);
    }

    private String getDigestFromIngest(JsonNode jsonNode) {
        return jsonNode.get(MESSAGE_DIGEST).asText();
    }

    private String getDigestFromPreservation(JsonNode jsonNode, String id) {
        return jsonNode.get(id).get(MESSAGE_DIGEST).asText();
    }

    private JsonNode toJsonNode(LogbookEvent event) {
        try {
            return JsonHandler.getFromString(event.getEvDetData());
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private boolean isOKEvent(LogbookEvent event) {
        return event.getOutDetail().toUpperCase().contains(OK.name());
    }

    private boolean isBinaryEvent(DbVersionsModel versionsModel, LogbookEvent event) {
        return event.getObId().equals(versionsModel.getId());
    }

    private boolean isDigestEvent(LogbookEvent event) {
        return (
            event.getOutDetail().contains(STORING_OBJECT_TASK_ID) ||
            event.getOutDetail().contains(CALC_CHECK) ||
            event.getOutDetail().contains(PreservationGenerateBinaryHash.ITEM_ID) ||
            event.getOutDetail().contains(PreservationStorageBinaryPlugin.ITEM_ID)
        );
    }

    private Optional<DbVersionsModel> getVersion(String usageVersion, RequestResponse<JsonNode> requestResponse)
        throws InvalidParseOperationException {
        DbObjectGroupModel objectGroup = JsonHandler.getFromJsonNode(
            ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult(),
            DbObjectGroupModel.class
        );

        return objectGroup
            .getQualifiers()
            .stream()
            .flatMap(qualifier -> qualifier.getVersions().stream())
            .filter(version -> usageVersion.equals(version.getDataObjectVersion()))
            .findFirst();
    }

    @JsonIgnore
    private boolean isTimeStampValid(String timeStampTokenAsString) {
        try {
            TimeStampToken timeStampToken = timeStampService.getTimeStampFrom(timeStampTokenAsString);
            CMSSignedData cmsSignedData = timeStampToken.toCMSSignedData();
            Store<X509CertificateHolder> certificates = cmsSignedData.getCertificates();

            Selector<X509CertificateHolder> selectX509Certificate = timeStampToken.getSID();
            Collection<X509CertificateHolder> x509Certificates = certificates.getMatches(selectX509Certificate);

            Optional<X509CertificateHolder> x509CertificateOptional = x509Certificates.stream().findFirst();
            if (!x509CertificateOptional.isPresent()) {
                return false;
            }

            X509CertificateHolder x509Certificate = x509CertificateOptional.get();
            SignerInformationVerifier sigVerifier = new JcaSimpleSignerInfoVerifierBuilder()
                .setProvider(PROVIDER_NAME)
                .build(x509Certificate);

            if (!isValid(timeStampToken, sigVerifier)) {
                return false;
            }

            AlgorithmIdentifier digestAlgorithmIdentifier = new AlgorithmIdentifier(
                timeStampToken.getTimeStampInfo().getMessageImprintAlgOID()
            );
            DigestCalculatorProvider digestCalculatorProvider = new BcDigestCalculatorProvider();
            DigestCalculator digestCalculator = digestCalculatorProvider.get(digestAlgorithmIdentifier);

            try (OutputStream dOut = digestCalculator.getOutputStream()) {
                dOut.write(x509Certificate.getEncoded());
                dOut.close();
                byte[] x509CertificateFromProvider = digestCalculator.getDigest();

                AttributeTable timeStampTokenSignedAttributes = timeStampToken.getSignedAttributes();
                if (timeStampTokenSignedAttributes == null) {
                    return false;
                }

                Attribute attribute = timeStampTokenSignedAttributes.get(
                    PKCSObjectIdentifiers.id_aa_signingCertificate
                );
                if (attribute == null) {
                    attribute = timeStampTokenSignedAttributes.get(PKCSObjectIdentifiers.id_aa_signingCertificateV2);
                }

                ASN1Encodable firstAsn1Encodable = attribute.getAttrValues().getObjectAt(0);
                SigningCertificateV2 signingCertificate = SigningCertificateV2.getInstance(firstAsn1Encodable);
                ESSCertIDv2 cert = signingCertificate.getCerts()[0];

                byte[] certificateTimeStampFile = cert.getCertHash();
                if (!Arrays.equals(certificateTimeStampFile, x509CertificateFromProvider)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            LOGGER.warn(e);
            return false;
        }
    }

    @JsonIgnore
    private boolean isValid(TimeStampToken timeStampToken, SignerInformationVerifier sigVerifier) {
        try {
            timeStampToken.validate(sigVerifier);
            return true;
        } catch (Exception e) {
            LOGGER.error(e);
            return false;
        }
    }

    private List<StorageStrategy> loadStorageStrategies(HandlerIO handler) throws ProcessingStatusException {
        try {
            return JsonHandler.getFromFileAsTypeReference(
                (File) handler.getInput(STRATEGIES_IN_RANK),
                new TypeReference<List<StorageStrategy>>() {}
            );
        } catch (InvalidParseOperationException e) {
            throw new ProcessingStatusException(StatusCode.FATAL, "Could not load storage strategies datas", e);
        }
    }

    private String getTraceabilityMerkleFileMerkleTreeRootDigest(File merkleFile)
        throws InvalidParseOperationException {
        return JsonHandler.getFromFile(merkleFile).get("Root").asText();
    }

    public static String traceabilityFilesDirectoryName(String objectGroupId, String evType) {
        return String.format("%s-%s", objectGroupId, evType);
    }
}
