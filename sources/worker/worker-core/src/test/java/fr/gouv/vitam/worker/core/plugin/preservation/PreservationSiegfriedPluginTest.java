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
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResult.OutputExtra;
import fr.gouv.vitam.worker.core.plugin.preservation.model.WorkflowBatchResults;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried.PRONOM_NAMESPACE;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.worker.core.plugin.preservation.PreservationActionPlugin.OUTPUT_FILES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

public class PreservationSiegfriedPluginTest {

    private HandlerIO handler = new TestHandlerIO();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @InjectMocks
    private PreservationSiegfriedPlugin siegriedPlugin;

    @Mock
    private FormatIdentifierFactory siegfriedFactory;

    @Mock
    private FormatIdentifier siegfried;

    @Before
    public void setup() throws Exception {
        given(siegfriedFactory.getFormatIdentifierFor(any())).willReturn(siegfried);
    }

    @Test
    public void should_disable_lfc_when_not_GENERATE_action() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.ANALYSE);
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        WorkflowBatchResults batchResults = new WorkflowBatchResults(Paths.get("tmp"), workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        // When
        List<ItemStatus> itemStatuses = siegriedPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::isLifecycleEnable).containsOnly(false);
    }

    @Test
    public void should_execute_throw_exception_not_implemented() {
        // Given / When
        ThrowingCallable executeSingle = () -> siegriedPlugin.execute(null, null);

        // Then
        assertThatThrownBy(executeSingle).isInstanceOf(IllegalStateException.class).hasMessage("Not implemented.");
    }

    @Test
    public void should_check_parameters_throw_exception_not_implemented() {
        // Given / When
        ThrowingCallable checkParameters = () -> siegriedPlugin.checkMandatoryIOParameter(null);

        // Then
        assertThatThrownBy(checkParameters).isInstanceOf(IllegalStateException.class).hasMessage("Not implemented.");
    }

    @Test
    public void should_find_a_format_return_item_status_OK() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        FormatIdentifierResponse formatIdentifierResponse = new FormatIdentifierResponse(
            "fmt/trololo",
            "application/trololo",
            "42",
            PRONOM_NAMESPACE
        );
        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willReturn(
            Collections.singletonList(formatIdentifierResponse)
        );

        // When
        List<ItemStatus> itemStatuses = siegriedPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(OK);
        String binaryGUID =
            ((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults()
                .get(0)
                .getOutputExtras()
                .get(0)
                .getBinaryGUID();

        assertThat(itemStatuses)
            .extracting(ItemStatus::getItemsStatus)
            .extracting(itemStatus -> itemStatus.get(PreservationSiegfriedPlugin.ITEM_ID))
            .extracting(ItemStatus::getSubTaskStatus)
            .extracting(subItemStatus -> subItemStatus.get(binaryGUID))
            .isNotEmpty();
    }

    @Test
    public void should_add_result_with_format() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        FormatIdentifierResponse formatIdentifierResponse = new FormatIdentifierResponse(
            "fmt/trololo",
            "application/trololo",
            "42",
            PRONOM_NAMESPACE
        );
        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willReturn(
            Collections.singletonList(formatIdentifierResponse)
        );

        // When
        siegriedPlugin.executeList(null, handler);

        // Then
        assertThat(((WorkflowBatchResults) handler.getInput(0)).getWorkflowBatchResults())
            .extracting(w -> w.getOutputExtras().get(0).getBinaryFormat())
            .extracting(Optional::get)
            .extracting(FormatIdentifierResponse::getFormatLiteral)
            .containsOnly("fmt/trololo");
    }

    @Test
    public void should_not_find_format_return_item_status_KO() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willReturn(
            Collections.emptyList()
        );

        // When
        List<ItemStatus> itemStatuses = siegriedPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(KO);
    }

    @Test
    public void should_throw_exception_FileFormatNotFoundException_return_item_status_KO() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willThrow(
            new FileFormatNotFoundException("trololo")
        );

        // When
        List<ItemStatus> itemStatuses = siegriedPlugin.executeList(null, handler);

        // Then
        assertThat(itemStatuses).extracting(ItemStatus::getGlobalStatus).containsOnly(KO);
    }

    @Test
    public void should_exception_FormatIdentifierNotFoundException_stop_the_workflow() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willThrow(
            new FormatIdentifierNotFoundException("trololo")
        );

        // When
        ThrowingCallable siegfriedError = () -> siegriedPlugin.executeList(null, handler);

        // Then
        assertThatThrownBy(siegfriedError)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("FATAL - FILE_FORMAT_TOOL_DOES_NOT_ANSWER");
    }

    @Test
    public void should_exception_FormatIdentifierTechnicalException_stop_the_workflow() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willThrow(
            new FormatIdentifierTechnicalException("trololo")
        );

        // When
        ThrowingCallable siegfriedError = () -> siegriedPlugin.executeList(null, handler);

        // Then
        assertThatThrownBy(siegfriedError)
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("FATAL - FILE_FORMAT_REFERENTIAL_TECHNICAL_ERROR");
    }

    @Test
    public void should_exception_of_any_type_stop_the_workflow() throws Exception {
        // Given
        OutputPreservation output = new OutputPreservation();
        output.setStatus(PreservationStatus.OK);
        output.setAction(ActionTypePreservation.GENERATE);
        output.setOutputName("outputName");
        List<OutputExtra> outputExtras = Arrays.asList(OutputExtra.of(output));
        List<WorkflowBatchResult> workflowBatchResults = Collections.singletonList(
            WorkflowBatchResult.of("", "", "", "", outputExtras, "", "", Collections.emptyList())
        );
        Path batchDirectory = Paths.get("tmp");
        WorkflowBatchResults batchResults = new WorkflowBatchResults(batchDirectory, workflowBatchResults);
        handler.addOutputResult(0, batchResults);

        given(siegfried.analysePath(batchDirectory.resolve(OUTPUT_FILES).resolve(output.getOutputName()))).willThrow(
            new IllegalStateException("trololo")
        );

        // When
        ThrowingCallable siegfriedError = () -> siegriedPlugin.executeList(null, handler);

        // Then
        assertThatThrownBy(siegfriedError).isInstanceOf(RuntimeException.class);
    }
}
