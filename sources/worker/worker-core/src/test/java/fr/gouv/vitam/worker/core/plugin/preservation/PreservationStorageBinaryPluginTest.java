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

import fr.gouv.vitam.batch.report.model.PreservationStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.functional.administration.core.backup.BackupService;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationActionPlugin.OUTPUT_FILES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

public class PreservationStorageBinaryPluginTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @Mock
    private BackupService backupService;

    @InjectMocks
    private PreservationStorageBinaryPlugin storageBinaryPlugin;

    private final HandlerIO handler = new TestHandlerIO();

    @Test
    public void should_disable_lfc_when_not_GENERATE_action() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.ANALYSE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = storageBinaryPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::isLifecycleEnable).containsOnly(false);
    }

    @Test
    public void should_store_binary_file() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        OutputExtra outputExtra = new OutputExtra(
            output,
            "binaryGUID",
            Optional.of(42L),
            Optional.of("sha-42"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        List<OutputExtra> outputExtras = Arrays.asList(outputExtra);
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "other_binary_strategy", Collections.emptyList())
        );
        File file = tmpGriffinFolder.newFolder(OUTPUT_FILES);
        Files.createFile(file.toPath().resolve(output.getOutputName()));
        WorkflowBatchResults batchResults = new WorkflowBatchResults(
            tmpGriffinFolder.getRoot().toPath(),
            workflowBatchResults
        );
        handler.addOutputResult(0, batchResults);

        StoredInfoResult storedInfo = new StoredInfoResult();
        storedInfo.setId("id");
        storedInfo.setDigest("SHA-42");
        storedInfo.setDigestType("SHA");
        storedInfo.setOfferIds(Collections.singletonList("offer.42.consul"));
        given(
            backupService.backup(any(), eq(DataCategory.OBJECT), anyString(), eq("other_binary_strategy"))
        ).willReturn(storedInfo);

        // When
        List<ItemStatus> itemStatuses = storageBinaryPlugin.executeList(null, handler);

        // Then
        String binaryGUID =
            ((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults()
                .get(0)
                .getOutputExtras()
                .get(0)
                .getBinaryGUID();

        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        assertThat(itemStatuses)
            .extracting(ItemStatus::getItemsStatus)
            .extracting(itemStatus -> itemStatus.get(PreservationStorageBinaryPlugin.ITEM_ID))
            .extracting(ItemStatus::getSubTaskStatus)
            .extracting(subItemStatus -> subItemStatus.get(binaryGUID))
            .isNotEmpty();
    }

    @Test
    public void should_return_KO_when_backup_exception() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "other_binary_strategy", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(backupService.backup(any(), eq(DataCategory.OBJECT), anyString(), eq("other_binary_strategy"))).willThrow(
            new BackupServiceException("execption")
        );

        // When
        List<ItemStatus> itemStatuses = storageBinaryPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(KO);
    }

    @Test
    public void should_add_result_with_stored_info() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        OutputExtra outputExtra = new OutputExtra(
            output,
            "binaryGUID",
            Optional.of(42L),
            Optional.of("42424242424242424242"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        List<OutputExtra> outputExtras = Arrays.asList(outputExtra);
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "other_binary_strategy", Collections.emptyList())
        );
        File file = tmpGriffinFolder.newFolder(OUTPUT_FILES);
        Files.createFile(file.toPath().resolve(output.getOutputName()));
        WorkflowBatchResults batchResults = new WorkflowBatchResults(
            tmpGriffinFolder.getRoot().toPath(),
            workflowBatchResults
        );
        handler.addOutputResult(0, batchResults);

        StoredInfoResult storedInfo = new StoredInfoResult();
        storedInfo.setId("id");
        storedInfo.setDigest("42424242424242424242");
        storedInfo.setDigestType("SHA");
        storedInfo.setOfferIds(Collections.singletonList("offer.42.consul"));
        given(
            backupService.backup(any(), eq(DataCategory.OBJECT), anyString(), eq("other_binary_strategy"))
        ).willReturn(storedInfo);

        // When
        storageBinaryPlugin.executeList(null, handler);

        // Then
        assertThat(((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults())
            .extracting(w -> w.getOutputExtras().get(0).getStoredInfo())
            .extracting(Optional::get)
            .extracting(StoredInfoResult::getDigest)
            .containsOnly("42424242424242424242");
    }

    @Test
    public void should_compare_digest() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        OutputExtra outputExtra = new OutputExtra(
            output,
            "binaryGuid",
            Optional.of(32L),
            Optional.of("hash_42"),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
        List<OutputExtra> outputExtras = Arrays.asList(outputExtra);
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "other_binary_strategy", Collections.emptyList())
        );
        File file = tmpGriffinFolder.newFolder(OUTPUT_FILES);
        Files.createFile(file.toPath().resolve(output.getOutputName()));
        WorkflowBatchResults batchResults = new WorkflowBatchResults(
            tmpGriffinFolder.getRoot().toPath(),
            workflowBatchResults
        );
        handler.addOutputResult(0, batchResults);

        StoredInfoResult storedInfo = new StoredInfoResult();
        storedInfo.setId("id");
        storedInfo.setDigest("hash_43");
        storedInfo.setDigestType("SHA");
        storedInfo.setOfferIds(Collections.singletonList("offer.42.consul"));
        given(
            backupService.backup(any(), eq(DataCategory.OBJECT), anyString(), eq("other_binary_strategy"))
        ).willReturn(storedInfo);

        // When
        List<ItemStatus> itemStatuses = storageBinaryPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(KO);
    }
}
