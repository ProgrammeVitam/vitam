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
package fr.gouv.vitam.worker.core.plugin.preservation.model;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.EXTRACT;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.EXTRACT_AU;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.IDENTIFY;

public class WorkflowBatchResult {

    private final String gotId;
    private final String unitId;
    private final String targetUse;
    private final String sourceUse;
    private final String sourceStrategy;
    private final String requestId;
    private final List<OutputExtra> outputExtras;
    private final List<String> unitsForExtractionAU;
    private final StatusCode globalStatus;

    private WorkflowBatchResult(
        String gotId,
        String unitId,
        String targetUse,
        String requestId,
        List<OutputExtra> outputExtras,
        StatusCode globalStatus,
        String sourceUse,
        String sourceStrategy,
        List<String> unitsForExtractionAU
    ) {
        this.gotId = gotId;
        this.unitId = unitId;
        this.targetUse = targetUse;
        this.requestId = requestId;
        this.outputExtras = outputExtras;
        this.globalStatus = globalStatus;
        this.sourceUse = sourceUse;
        this.sourceStrategy = sourceStrategy;
        this.unitsForExtractionAU = unitsForExtractionAU;
    }

    public static WorkflowBatchResult of(
        String gotId,
        String unitId,
        String targetUse,
        String requestId,
        List<OutputExtra> outputExtras,
        String sourceUse,
        String sourceStrategy,
        List<String> unitsForExtractionAU
    ) {
        return new WorkflowBatchResult(
            gotId,
            unitId,
            targetUse,
            requestId,
            Collections.unmodifiableList(outputExtras),
            WorkflowBatchResult.globalStatusFromOutputExtras(outputExtras),
            sourceUse,
            sourceStrategy,
            unitsForExtractionAU
        );
    }

    public static WorkflowBatchResult of(WorkflowBatchResult workflowBatchResult, List<OutputExtra> outputExtras) {
        return new WorkflowBatchResult(
            workflowBatchResult.getGotId(),
            workflowBatchResult.getUnitId(),
            workflowBatchResult.getTargetUse(),
            workflowBatchResult.getRequestId(),
            Collections.unmodifiableList(outputExtras),
            WorkflowBatchResult.globalStatusFromOutputExtras(outputExtras),
            workflowBatchResult.getSourceUse(),
            workflowBatchResult.getSourceStrategy(),
            workflowBatchResult.getUnitsForExtractionAU()
        );
    }

    private static StatusCode globalStatusFromOutputExtras(List<OutputExtra> outputExtras) {
        List<PreservationStatus> statuses = outputExtras
            .stream()
            .map(o -> o.getOutput().getStatus())
            .map(s -> s.equals(PreservationStatus.OK) ? PreservationStatus.OK : PreservationStatus.KO)
            .distinct()
            .collect(Collectors.toList());

        if (statuses.isEmpty()) {
            return KO;
        }

        if (statuses.size() > 1) {
            return WARNING;
        }

        return statuses.get(0).equals(PreservationStatus.OK) ? OK : KO;
    }

    public String getGotId() {
        return gotId;
    }

    public String getUnitId() {
        return unitId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getTargetUse() {
        return targetUse;
    }

    public String getSourceUse() {
        return sourceUse;
    }

    public String getSourceStrategy() {
        return sourceStrategy;
    }

    public List<OutputExtra> getOutputExtras() {
        return outputExtras;
    }

    public StatusCode getGlobalStatus() {
        return globalStatus;
    }

    public List<String> getUnitsForExtractionAU() {
        return unitsForExtractionAU;
    }

    public static class OutputExtra {

        private final OutputPreservation output;
        private final String binaryGUID;
        private final Optional<Long> size;
        private final Optional<String> binaryHash;
        private final Optional<FormatIdentifierResponse> binaryFormat;
        private final Optional<StoredInfoResult> storedInfo;
        private final Optional<ExtractedMetadata> extractedMetadataGOT;
        private final Optional<ExtractedMetadataForAu> extractedMetadataAU;
        private final Optional<String> error;

        @VisibleForTesting
        public OutputExtra(
            OutputPreservation output,
            String binaryGUID,
            Optional<Long> size,
            Optional<String> binaryHash,
            Optional<FormatIdentifierResponse> binaryFormat,
            Optional<StoredInfoResult> storedInfo,
            Optional<ExtractedMetadata> extractedMetadataGOT,
            Optional<ExtractedMetadataForAu> extractedMetadataAU,
            Optional<String> error
        ) {
            this.output = output;
            this.binaryGUID = binaryGUID;
            this.size = size;
            this.binaryHash = binaryHash;
            this.binaryFormat = binaryFormat;
            this.storedInfo = storedInfo;
            this.extractedMetadataGOT = extractedMetadataGOT;
            this.extractedMetadataAU = extractedMetadataAU;
            this.error = error;
        }

        public static OutputExtra of(OutputPreservation output) {
            return new OutputExtra(
                output,
                GUIDFactory.newGUID().getId(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
            );
        }

        public static OutputExtra withBinaryHashAndSize(OutputExtra outputExtra, String binaryHash, long size) {
            return new OutputExtra(
                outputExtra.getOutput(),
                outputExtra.getBinaryGUID(),
                Optional.of(size),
                Optional.of(binaryHash),
                outputExtra.getBinaryFormat(),
                outputExtra.getStoredInfo(),
                outputExtra.getExtractedMetadataGOT(),
                outputExtra.getExtractedMetadataAU(),
                outputExtra.getError()
            );
        }

        public static OutputExtra withBinaryFormat(OutputExtra outputExtra, FormatIdentifierResponse binaryFormat) {
            return new OutputExtra(
                outputExtra.getOutput(),
                outputExtra.getBinaryGUID(),
                outputExtra.getSize(),
                outputExtra.getBinaryHash(),
                Optional.of(binaryFormat),
                outputExtra.getStoredInfo(),
                outputExtra.getExtractedMetadataGOT(),
                outputExtra.getExtractedMetadataAU(),
                outputExtra.getError()
            );
        }

        public static OutputExtra withStoredInfo(OutputExtra outputExtra, StoredInfoResult storedInfo) {
            return new OutputExtra(
                outputExtra.getOutput(),
                outputExtra.getBinaryGUID(),
                outputExtra.getSize(),
                outputExtra.getBinaryHash(),
                outputExtra.getBinaryFormat(),
                Optional.of(storedInfo),
                outputExtra.getExtractedMetadataGOT(),
                outputExtra.getExtractedMetadataAU(),
                outputExtra.getError()
            );
        }

        public static OutputExtra withExtractedMetadataForGot(
            OutputExtra outputExtra,
            ExtractedMetadata extractedMetadataGOT
        ) {
            return new OutputExtra(
                outputExtra.getOutput(),
                outputExtra.getBinaryGUID(),
                outputExtra.getSize(),
                outputExtra.getBinaryHash(),
                outputExtra.getBinaryFormat(),
                outputExtra.getStoredInfo(),
                Optional.of(extractedMetadataGOT),
                outputExtra.getExtractedMetadataAU(),
                outputExtra.getError()
            );
        }

        public static OutputExtra withExtractedMetadataForAu(
            OutputExtra outputExtra,
            ExtractedMetadataForAu extractedMetadataAU
        ) {
            return new OutputExtra(
                outputExtra.getOutput(),
                outputExtra.getBinaryGUID(),
                outputExtra.getSize(),
                outputExtra.getBinaryHash(),
                outputExtra.getBinaryFormat(),
                outputExtra.getStoredInfo(),
                outputExtra.getExtractedMetadataGOT(),
                Optional.of(extractedMetadataAU),
                outputExtra.getError()
            );
        }

        public static OutputExtra inError(String errorMessage) {
            return new OutputExtra(
                null,
                "ERROR",
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(errorMessage)
            );
        }

        public OutputPreservation getOutput() {
            return output;
        }

        public String getBinaryGUID() {
            return binaryGUID;
        }

        public Optional<Long> getSize() {
            return size;
        }

        public Optional<String> getBinaryHash() {
            return binaryHash;
        }

        public Optional<FormatIdentifierResponse> getBinaryFormat() {
            return binaryFormat;
        }

        public Optional<StoredInfoResult> getStoredInfo() {
            return storedInfo;
        }

        public Optional<String> getError() {
            return error;
        }

        public Optional<ExtractedMetadata> getExtractedMetadataGOT() {
            return extractedMetadataGOT;
        }

        public Optional<ExtractedMetadataForAu> getExtractedMetadataAU() {
            return extractedMetadataAU;
        }

        public boolean isOkAndGenerated() {
            return (
                this.getOutput().getAction().equals(GENERATE) &&
                this.getOutput().getStatus().equals(PreservationStatus.OK)
            );
        }

        public boolean isInError() {
            return this.error.isPresent();
        }

        public boolean isOkAndIdentify() {
            return (
                this.getOutput().getAction().equals(IDENTIFY) &&
                this.getOutput().getStatus().equals(PreservationStatus.OK)
            );
        }

        public boolean isOkAndExtractedGot() {
            return (
                this.getOutput().getAction().equals(EXTRACT) &&
                this.getOutput().getStatus().equals(PreservationStatus.OK)
            );
        }

        public boolean isOkAndExtractedAu() {
            return (
                this.getOutput().getAction().equals(EXTRACT_AU) &&
                this.getOutput().getStatus().equals(PreservationStatus.OK)
            );
        }
    }
}
