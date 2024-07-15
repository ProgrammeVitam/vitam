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
package fr.gouv.vitam.worker.core.plugin.preservation;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.json.Difference;
import fr.gouv.vitam.common.json.Differences;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.objectgroup.DbFileInfoModel;
import fr.gouv.vitam.common.model.objectgroup.DbFormatIdentificationModel;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbStorageModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;
import fr.gouv.vitam.common.model.preservation.OtherMetadata;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ExtractedMetadata;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.CONTENT_FOLDER;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.IDENTIFY;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationGenerateBinaryHash.digestPreservationGeneration;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatusSubItems;

public class PreservationUpdateObjectGroupPlugin extends ActionHandler {

    private static final String PLUGIN_NAME = "PRESERVATION_INDEXATION_METADATA";
    private static final String RAW_METADATA = "RawMetadata";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationUpdateObjectGroupPlugin.class);
    private final MetaDataClientFactory metaDataClientFactory;

    public PreservationUpdateObjectGroupPlugin() {
        this(MetaDataClientFactory.getInstance());
    }

    @VisibleForTesting
    PreservationUpdateObjectGroupPlugin(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    @Override
    public List<ItemStatus> executeList(WorkerParameters workerParameters, HandlerIO handler) {
        LOGGER.info("Starting {}", PLUGIN_NAME);

        WorkflowBatchResults results = (WorkflowBatchResults) handler.getInput(0);
        List<WorkflowBatchResult> workflowBatchResults = results.getWorkflowBatchResults();

        try (MetaDataClient metaDataClient = metaDataClientFactory.getClient()) {
            return workflowBatchResults
                .stream()
                .map(w -> updateObjectGroupModel(w, metaDataClient))
                .collect(Collectors.toList());
        }
    }

    private ItemStatus updateObjectGroupModel(WorkflowBatchResult batchResult, MetaDataClient metaDataClient) {
        List<OutputExtra> outputExtras = batchResult
            .getOutputExtras()
            .stream()
            .filter(outputExtra -> !outputExtra.isInError())
            .collect(Collectors.toList());

        List<OutputExtra> generateOkActions = outputExtras
            .stream()
            .filter(OutputExtra::isOkAndGenerated)
            .collect(Collectors.toList());

        List<OutputExtra> identifyOkActions = outputExtras
            .stream()
            .filter(OutputExtra::isOkAndIdentify)
            .collect(Collectors.toList());

        List<OutputExtra> extractedOkActions = outputExtras
            .stream()
            .filter(OutputExtra::isOkAndExtractedGot)
            .collect(Collectors.toList());

        if (generateOkActions.isEmpty() && identifyOkActions.isEmpty() && extractedOkActions.isEmpty()) {
            return buildItemStatus(PLUGIN_NAME, OK, EventDetails.of("No changes")).disableLfc();
        }

        Stream<String> subItemIds = Stream.of(
            identifyOkActions.stream().map(OutputExtra::getBinaryGUID),
            generateOkActions.stream().map(OutputExtra::getBinaryGUID),
            extractedOkActions.stream().map(OutputExtra::getBinaryGUID)
        )
            .flatMap(Function.identity())
            .distinct();

        try {
            RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupByIdRaw(batchResult.getGotId());
            if (!requestResponse.isOk()) {
                return buildItemStatus(
                    PLUGIN_NAME,
                    FATAL,
                    EventDetails.of(String.format("ObjectGroup not found %s", batchResult.getGotId()))
                );
            }
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);

            List<Difference> differences = new ArrayList<>();
            List<DbQualifiersModel> qualifiersWithIdentification = dbObjectGroupModel
                .getQualifiers()
                .stream()
                .map(qualifier -> mapQualifierBinary(batchResult, qualifier, identifyOkActions, differences))
                .map(qualifier -> mapQualifierBinary(batchResult, qualifier, extractedOkActions, differences))
                .collect(Collectors.toList());

            List<DbQualifiersModel> finalQualifiersToUpdate = addGeneratedQualifiersIfUpdate(
                batchResult,
                generateOkActions,
                qualifiersWithIdentification
            );
            List<Difference> differencesFiltered = differences
                .stream()
                .filter(Difference::hasDifference)
                .collect(Collectors.toList());

            if (differencesFiltered.isEmpty() && generateOkActions.isEmpty()) {
                ItemStatus itemStatus = new ItemStatus(PLUGIN_NAME);
                return itemStatus.disableLfc();
            }

            final Optional<Integer> totalBinarySize = finalQualifiersToUpdate
                .stream()
                .map(DbQualifiersModel::getNbc)
                .reduce(Integer::sum);

            if (totalBinarySize.isEmpty()) {
                throw new IllegalStateException("total binaries for objectGroup nbc is absent");
            }

            Map<String, JsonNode> action = new HashMap<>();
            action.put(QUALIFIERS.exactToken(), JsonHandler.toJsonNode(finalQualifiersToUpdate));
            SetAction setQualifier = new SetAction(action);

            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addHintFilter(OBJECTGROUPS.exactToken());
            query.addActions(
                UpdateActionHelper.push(VitamFieldsHelper.operations(), batchResult.getRequestId()),
                UpdateActionHelper.set(VitamFieldsHelper.nbobjects(), totalBinarySize.get()),
                setQualifier
            );

            metaDataClient.updateObjectGroupById(query.getFinalUpdate(), batchResult.getGotId());

            return buildItemStatusSubItems(PLUGIN_NAME, subItemIds, OK, new Differences(differencesFiltered));
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatusSubItems(
                PLUGIN_NAME,
                subItemIds,
                FATAL,
                EventDetails.of(String.format("FATAL ERROR IN %s ACTION.", PLUGIN_NAME))
            ).disableLfc();
        }
    }

    private List<DbQualifiersModel> addGeneratedQualifiersIfUpdate(
        WorkflowBatchResult batchResult,
        List<OutputExtra> generateOkActions,
        List<DbQualifiersModel> qualifiers
    ) {
        if (generateOkActions.isEmpty()) {
            return qualifiers;
        }

        DbQualifiersModel qualifierModelToUpdate = findQualifier(batchResult, qualifiers);
        int lastVersion = getLastVersion(qualifierModelToUpdate);

        List<DbVersionsModel> versionsModelToInsert = IntStream.range(0, generateOkActions.size())
            .mapToObj(i -> createVersion(generateOkActions.get(i), batchResult, lastVersion + i + 1))
            .collect(Collectors.toList());

        List<DbVersionsModel> versions = qualifierModelToUpdate.getVersions();
        versions.addAll(versionsModelToInsert);
        qualifierModelToUpdate.setNbc(versions.size());

        List<DbQualifiersModel> updatedQualifiers = qualifiers
            .stream()
            .filter(qualifier -> !qualifier.getQualifier().equals(qualifierModelToUpdate.getQualifier()))
            .collect(Collectors.toList());

        updatedQualifiers.add(qualifierModelToUpdate);

        return updatedQualifiers;
    }

    private int getLastVersion(DbQualifiersModel qualifierModelToUpdate) {
        return qualifierModelToUpdate
            .getVersions()
            .stream()
            .map(DbVersionsModel::getDataObjectVersion)
            .map(dataObjectVersion -> dataObjectVersion.split("_")[1])
            .map(Integer::parseInt)
            .max(Comparator.naturalOrder())
            .orElse(0);
    }

    private DbQualifiersModel findQualifier(WorkflowBatchResult batchResult, List<DbQualifiersModel> qualifiers) {
        if (batchResult.getSourceUse().equals(batchResult.getTargetUse())) {
            return qualifiers
                .stream()
                .filter(qualifier -> qualifier.getQualifier().equals(batchResult.getSourceUse()))
                .map(q -> new DbQualifiersModel(q.getQualifier(), q.getNbc(), new ArrayList<>(q.getVersions())))
                .findFirst()
                .orElseThrow(
                    () ->
                        new VitamRuntimeException(
                            String.format(
                                "No 'Qualifier' %s for 'ObjectGroup' %s.",
                                batchResult.getSourceUse(),
                                batchResult.getGotId()
                            )
                        )
                );
        }
        return qualifiers
            .stream()
            .filter(qualifier -> qualifier.getQualifier().equals(batchResult.getTargetUse()))
            .findFirst()
            .orElse(new DbQualifiersModel(batchResult.getTargetUse(), 0, new ArrayList<>()));
    }

    private DbQualifiersModel mapQualifierBinary(
        WorkflowBatchResult batchResult,
        DbQualifiersModel qualifier,
        List<OutputExtra> actions,
        List<Difference> differences
    ) {
        if (actions.isEmpty() || !qualifier.getQualifier().equals(batchResult.getSourceUse())) {
            return qualifier;
        }

        List<DbVersionsModel> versions = qualifier
            .getVersions()
            .stream()
            .map(v -> mapVersionModel(actions, v, differences))
            .collect(Collectors.toList());
        return new DbQualifiersModel(qualifier.getQualifier(), qualifier.getNbc(), versions);
    }

    private DbVersionsModel mapVersionModel(
        List<OutputExtra> actions,
        DbVersionsModel version,
        List<Difference> differences
    ) {
        return actions
            .stream()
            .filter(o -> o.getOutput().getInputPreservation().getName().equals(version.getId()))
            .findFirst()
            .map(
                outputExtra ->
                    IDENTIFY.equals(outputExtra.getOutput().getAction())
                        ? createNewVersionIdentified(outputExtra, version, differences)
                        : createNewVersionExtracted(outputExtra, version, differences)
            )
            .orElse(version);
    }

    private DbVersionsModel createNewVersionIdentified(
        OutputExtra outputExtra,
        DbVersionsModel version,
        List<Difference> differences
    ) {
        DbFormatIdentificationModel format = outputExtra.getOutput().getFormatIdentification();
        if (format == null) {
            throw new VitamRuntimeException("FormatIdentification cannot be null.");
        }
        Difference difference = format.difference(version.getFormatIdentificationModel());
        if (difference.hasDifference()) {
            differences.add(difference);
            return DbVersionsModel.newVersionsFrom(version, format);
        }
        return version;
    }

    private DbVersionsModel createNewVersionExtracted(
        OutputExtra outputExtra,
        DbVersionsModel version,
        List<Difference> differences
    ) {
        Optional<ExtractedMetadata> extractedMetadata = outputExtra.getExtractedMetadataGOT();
        if (extractedMetadata.isEmpty()) {
            throw new VitamRuntimeException("ExtractedMetadata cannot be null.");
        }
        Difference<List<Object>> diffOtherMetadataToAdd = new Difference<>(OtherMetadata.class.getSimpleName());

        OtherMetadata oldMetadata = version.getOtherMetadata();
        OtherMetadata newOtherMetadata = new OtherMetadata(oldMetadata);
        OtherMetadata extractedOtherMetadata = extractedMetadata.get().getOtherMetadata();
        String extractedRawMetadata = extractedMetadata.get().getRawMetadata();

        if (extractedRawMetadata != null) {
            if (extractedRawMetadata.length() > VitamConfiguration.getTextMaxLength()) {
                newOtherMetadata.put(
                    RAW_METADATA,
                    new ArrayList<>(
                        Splitter.fixedLength(VitamConfiguration.getTextMaxLength()).splitToList(extractedRawMetadata)
                    )
                );
            } else {
                newOtherMetadata.put(RAW_METADATA, Collections.singletonList(extractedRawMetadata));
            }
        }

        if (extractedOtherMetadata == null || extractedOtherMetadata.isEmpty()) {
            diffOtherMetadataToAdd.add(
                RAW_METADATA,
                Collections.emptyList(),
                Collections.singletonList(extractedRawMetadata)
            );
        } else {
            extractedOtherMetadata.forEach((key, value) -> {
                List<Object> oldValue = oldMetadata.get(key);
                if (oldValue != null) {
                    ArrayList<Object> union = new ArrayList<>(CollectionUtils.union(oldValue, value));
                    diffOtherMetadataToAdd.add(key, oldValue, union);
                    newOtherMetadata.put(key, union);
                } else {
                    diffOtherMetadataToAdd.add(key, Collections.emptyList(), value);
                    newOtherMetadata.put(key, value);
                }
            });
        }

        if (diffOtherMetadataToAdd.hasDifference()) {
            differences.add(diffOtherMetadataToAdd);
            return DbVersionsModel.newVersionsFrom(version, newOtherMetadata);
        }

        return version;
    }

    private DbVersionsModel createVersion(
        OutputExtra outputExtra,
        WorkflowBatchResult workflowBatchResult,
        Integer newDataObjectVersion
    ) {
        Optional<FormatIdentifierResponse> formatIdentifierResponse = outputExtra.getBinaryFormat();

        boolean allInformationNeeded =
            formatIdentifierResponse.isPresent() &&
            outputExtra.getBinaryHash().isPresent() &&
            outputExtra.getSize().isPresent() &&
            outputExtra.getStoredInfo().isPresent();

        if (!allInformationNeeded) {
            throw new VitamRuntimeException("Cannot update this version model, mission information.");
        }

        DbVersionsModel versionModel = new DbVersionsModel();
        versionModel.setId(outputExtra.getBinaryGUID());
        versionModel.setDataObjectGroupId(workflowBatchResult.getGotId());
        versionModel.setDataObjectVersion(workflowBatchResult.getTargetUse() + "_" + newDataObjectVersion);
        versionModel.setOpi(workflowBatchResult.getRequestId());
        versionModel.setAlgorithm(digestPreservationGeneration.getName());
        versionModel.setUri(CONTENT_FOLDER + File.separator + outputExtra.getOutput().getOutputName());

        DbFileInfoModel fileInfoModel = new DbFileInfoModel();
        fileInfoModel.setFilename(outputExtra.getOutput().getOutputName());
        fileInfoModel.setLastModified(LocalDateUtil.nowFormatted());
        versionModel.setFileInfoModel(fileInfoModel);

        StoredInfoResult storedInfoResult = outputExtra.getStoredInfo().get();
        DbStorageModel dbStorageModel = new DbStorageModel();
        dbStorageModel.setStrategyId(storedInfoResult.getStrategy());

        DbFormatIdentificationModel formatIdentificationModel = new DbFormatIdentificationModel();

        formatIdentificationModel.setFormatId(formatIdentifierResponse.get().getPuid());
        formatIdentificationModel.setFormatLitteral(formatIdentifierResponse.get().getFormatLiteral());
        formatIdentificationModel.setMimeType(formatIdentifierResponse.get().getMimetype());

        versionModel.setStorage(dbStorageModel);
        versionModel.setFormatIdentificationModel(formatIdentificationModel);
        versionModel.setMessageDigest(outputExtra.getBinaryHash().get());
        versionModel.setSize(outputExtra.getSize().get());

        return versionModel;
    }
}
