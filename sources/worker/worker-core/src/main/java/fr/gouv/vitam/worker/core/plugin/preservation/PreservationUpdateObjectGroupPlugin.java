/*
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
 */
package fr.gouv.vitam.worker.core.plugin.preservation;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationGenerateBinaryHash.digestPreservationGeneration;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;
import static java.util.Arrays.asList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
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
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;

public class PreservationUpdateObjectGroupPlugin extends ActionHandler {
    private final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PreservationUpdateObjectGroupPlugin.class);

    private static final String PLUGIN_NAME = "PRESERVATION_INDEXATION_METADATA";
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
            return workflowBatchResults.stream()
                .map(w -> updateObjectGroupModel(w, metaDataClient))
                .collect(Collectors.toList());
        }
    }

    private ItemStatus updateObjectGroupModel(WorkflowBatchResult batchResult, MetaDataClient metaDataClient) {
        List<OutputExtra> generateOkActions = batchResult.getOutputExtras()
            .stream()
            .filter(OutputExtra::isOkAndGenerated)
            .collect(Collectors.toList());

        if (generateOkActions.isEmpty()) {
            return new ItemStatus(PLUGIN_NAME).disableLfc();
        }

        try {
            String gotId = batchResult.getGotId();
            RequestResponse<JsonNode> requestResponse = metaDataClient.getObjectGroupByIdRaw(gotId);
            if (!requestResponse.isOk()) {
                return buildItemStatus(PLUGIN_NAME, FATAL, EventDetails.of(String.format("ObjectGroup not found %s", gotId)));
            }
            JsonNode firstResult = ((RequestResponseOK<JsonNode>) requestResponse).getFirstResult();
            DbObjectGroupModel dbObjectGroupModel = JsonHandler.getFromJsonNode(firstResult, DbObjectGroupModel.class);
            DbQualifiersModel qualifierModel;
            Integer lastVersion;

            if (batchResult.getSourceUse().equals(batchResult.getTargetUse())) {
                qualifierModel = dbObjectGroupModel
                    .getQualifiers()
                    .stream()
                    .filter(qualifier -> qualifier.getQualifier().equals(batchResult.getSourceUse()))
                    .findFirst()
                    .orElseThrow(
                        () -> new VitamRuntimeException(
                            String.format("No 'Qualifier' %s for 'ObjectGroup' %s.", batchResult.getSourceUse(), gotId)));
                lastVersion = extractLastVersionOfQualifier(batchResult, qualifierModel) + 1;
            } else {
                qualifierModel = dbObjectGroupModel.getQualifiers()
                    .stream()
                    .filter(qualifier -> qualifier.getQualifier().equals(batchResult.getTargetUse()))
                    .findFirst()
                    .orElse(new DbQualifiersModel());
                boolean firstQualifier = isFirstQualifier(qualifierModel, batchResult);
                lastVersion = getLastVersion(batchResult, qualifierModel, firstQualifier);
            }


            List<DbVersionsModel> versionsModelToInsert = IntStream.range(0, generateOkActions.size())
                .mapToObj(i -> buildVersionModel(generateOkActions.get(i), batchResult, lastVersion + i))
                .collect(Collectors.toList());

            qualifierModel.getVersions().addAll(versionsModelToInsert);
            qualifierModel.setNbc(lastVersion);

            List<DbQualifiersModel> finalQualifiersModelToUpdate = dbObjectGroupModel.getQualifiers().stream()
                .filter(qualifier -> !qualifier.getQualifier().equals(batchResult.getTargetUse()))
                .collect(Collectors.toList());
            finalQualifiersModelToUpdate.add(qualifierModel);

            final Optional<Integer> totalBinaries =
                finalQualifiersModelToUpdate.stream()
                    .map(DbQualifiersModel::getNbc)
                    .reduce(Integer::sum);

            if (!totalBinaries.isPresent()) {
                throw new IllegalStateException("total binaries for objectGroup nbc is absent");
            }

            Map<String, JsonNode> action = new HashMap<>();
            action.put(QUALIFIERS.exactToken(), JsonHandler.toJsonNode(finalQualifiersModelToUpdate));
            SetAction setQualifier = new SetAction(action);

            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addHintFilter(OBJECTGROUPS.exactToken());
            JsonNode newUpdateQuery = query.addActions(
                UpdateActionHelper.push(VitamFieldsHelper.operations(), batchResult.getRequestId()),
                UpdateActionHelper.set(VitamFieldsHelper.nbobjects(), totalBinaries.get()),
                setQualifier).getFinalUpdate();

            metaDataClient.updateObjectGroupById(newUpdateQuery, gotId);
            ObjectNode gotIdNode = JsonHandler.createObjectNode();
            gotIdNode.put("gotId", gotId);
            return buildItemStatus(PLUGIN_NAME, OK, gotIdNode);
        } catch (Exception e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, FATAL, e)
                .disableLfc();
        }
    }

    private boolean isFirstQualifier(DbQualifiersModel qualifierModel, WorkflowBatchResult batchResult) {
        if (qualifierModel.getQualifier() == null) {
            qualifierModel.setNbc(1);
            qualifierModel.setQualifier(batchResult.getTargetUse());
            qualifierModel.setVersions(new ArrayList<>());
            return true;
        }
        return false;
    }

    private Integer getLastVersion(WorkflowBatchResult batchResult, DbQualifiersModel qualifierModel, boolean firstQualifier) {
        if (!firstQualifier) {
            return extractLastVersionOfQualifier(batchResult, qualifierModel) + 1;
        }
        return 1;
    }

    private Integer extractLastVersionOfQualifier(WorkflowBatchResult workflowBatchResult, DbQualifiersModel qualifiersModel) {
            return qualifiersModel.getVersions()
                .stream()
                .map(DbVersionsModel::getDataObjectVersion)
                .map(dataObjectVersion -> asList(dataObjectVersion.split("_")).get(1))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .orElseThrow(
                    () -> new VitamRuntimeException(
                        String.format("Error extracting 'DataObjectVersion' for %s", workflowBatchResult.getGotId())));
    }

    private DbVersionsModel buildVersionModel(OutputExtra outputExtra, WorkflowBatchResult workflowBatchResult, Integer newDataObjectVersion) {
        DbVersionsModel versionModel = new DbVersionsModel();
        versionModel.setId(outputExtra.getBinaryGUID());
        versionModel.setDataObjectGroupId(workflowBatchResult.getGotId());
        versionModel.setDataObjectVersion(workflowBatchResult.getTargetUse() + "_" + newDataObjectVersion);
        versionModel.setOpi(workflowBatchResult.getRequestId());
        versionModel.setAlgorithm(digestPreservationGeneration.getName());

        Optional<FormatIdentifierResponse> formatIdentifierResponse = outputExtra.getBinaryFormat();

        if (!formatIdentifierResponse.isPresent()) {
            throw new IllegalStateException("format not found");
        }

        DbFileInfoModel fileInfoModel = new DbFileInfoModel();
        fileInfoModel.setFilename(outputExtra.getOutput().getOutputName());
        fileInfoModel.setLastModified(LocalDateUtil.getFormattedDateForMongo(LocalDateTime.now()));
        versionModel.setFileInfoModel(fileInfoModel);

        boolean allInformationNeeded = outputExtra.getBinaryFormat().isPresent()
            && outputExtra.getBinaryHash().isPresent()
            && outputExtra.getSize().isPresent()
            && outputExtra.getStoredInfo().isPresent();


        if (!allInformationNeeded) {
            throw new VitamRuntimeException("Cannot update this version model, mission information.");
        }

        StoredInfoResult storedInfoResult = outputExtra.getStoredInfo().get();
        DbStorageModel dbStorageModel = new DbStorageModel();
        dbStorageModel.setNbc(storedInfoResult.getNbCopy());
        dbStorageModel.setOfferIds(storedInfoResult.getOfferIds());
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
