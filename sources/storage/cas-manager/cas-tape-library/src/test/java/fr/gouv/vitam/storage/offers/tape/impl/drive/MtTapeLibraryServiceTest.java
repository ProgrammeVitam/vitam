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
package fr.gouv.vitam.storage.offers.tape.impl.drive;

import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MtTapeLibraryServiceTest {

    public static final String DEVICE_NST = "/dev/nst0";
    public static final String COMMAND_MT = "/bin/mt";
    private TapeDriveConf tapeDriveConf = mock(TapeDriveConf.class);
    private ProcessExecutor processExecutor = mock(ProcessExecutor.class);

    @Before
    public void setUp() {}

    @After
    public void tearDown() {}

    @Test
    public void test_constructor() {
        assertThatCode(() -> new MtTapeLibraryService(tapeDriveConf)).doesNotThrowAnyException();

        assertThatThrownBy(() -> new MtTapeLibraryService(null)).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new MtTapeLibraryService(tapeDriveConf, null)).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    public void test_status_OK() throws TapeCommandException {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(output.getStdout()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        TapeDriveSpec status = mtTapeLibraryService.status();

        assertThat(status).isNotNull();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "status");
    }

    @Test
    public void test_status_KO() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(1);
        when(output.getStderr()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        Throwable throwable = catchThrowable(mtTapeLibraryService::status);

        assertThat(throwable).isInstanceOf(TapeCommandException.class);
        assertThat(((Output) ((TapeCommandException) throwable).getDetails()).getStderr()).contains(
            "Fake Just To Avoid Null"
        );

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "status");
    }

    @Test
    public void test_rewind_OK() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(output.getStdout()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        assertThatCode(mtTapeLibraryService::rewind).doesNotThrowAnyException();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "rewind");
    }

    @Test
    public void test_rewind_KO() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(1);
        when(output.getStderr()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        Throwable throwable = catchThrowable(mtTapeLibraryService::rewind);

        assertThat(throwable).isInstanceOf(TapeCommandException.class);
        assertThat(((Output) ((TapeCommandException) throwable).getDetails()).getStderr()).contains(
            "Fake Just To Avoid Null"
        );

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "rewind");
    }

    @Test
    public void test_goto_end_OK() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(output.getStdout()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);
        assertThatCode(mtTapeLibraryService::goToEnd).doesNotThrowAnyException();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "eod");
    }

    @Test
    public void test_goto_end_KO() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(1);
        when(output.getStderr()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        Throwable throwable = catchThrowable(mtTapeLibraryService::goToEnd);

        assertThat(throwable).isInstanceOf(TapeCommandException.class);
        assertThat(((Output) ((TapeCommandException) throwable).getDetails()).getStderr()).contains(
            "Fake Just To Avoid Null"
        );

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "eod");
    }

    @Test
    public void test_goto_position_fsf_OK() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(output.getStdout()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        assertThatCode(() -> mtTapeLibraryService.move(5, false)).doesNotThrowAnyException();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "fsf", "5");

        assertThatCode(() -> mtTapeLibraryService.move(5, false)).doesNotThrowAnyException();

        commandPath = ArgumentCaptor.forClass(String.class);
        timeout = ArgumentCaptor.forClass(Long.class);
        args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(2)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "fsf", "5");
    }

    @Test
    public void test_goto_position_bsf_OK() {
        when(tapeDriveConf.getMtPath()).thenReturn(COMMAND_MT);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(output.getStdout()).thenReturn("Fake Just To Avoid Null");
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        MtTapeLibraryService mtTapeLibraryService = new MtTapeLibraryService(tapeDriveConf, processExecutor);

        assertThatCode(() -> mtTapeLibraryService.move(5, true)).doesNotThrowAnyException();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_MT);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("-f", DEVICE_NST, "bsfm", "6");
    }
}
