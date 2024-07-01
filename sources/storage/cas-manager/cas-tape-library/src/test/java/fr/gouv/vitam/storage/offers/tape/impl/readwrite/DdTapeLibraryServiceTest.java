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
package fr.gouv.vitam.storage.offers.tape.impl.readwrite;

import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DdTapeLibraryServiceTest {

    public static final String COMMAND_DD = "/bin/dd";
    public static final String DEVICE_NST_0 = "/dev/nst0";
    public static final String MY_FAKE_FILE_TAR = "my_fake_file.tar";

    private TapeDriveConf tapeDriveConf = mock(TapeDriveConf.class);
    private ProcessExecutor processExecutor = mock(ProcessExecutor.class);

    @Test
    public void test_constructor() {
        assertThatCode(() -> new DdTapeLibraryService(tapeDriveConf, "/tmp", "/tmp")).doesNotThrowAnyException();

        assertThatThrownBy(() -> new DdTapeLibraryService(tapeDriveConf, "/tmp", null)).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThatThrownBy(() -> new DdTapeLibraryService(tapeDriveConf, null, "/tmp")).isInstanceOf(
            IllegalArgumentException.class
        );

        assertThatThrownBy(() -> new DdTapeLibraryService(null, "/tmp", "/tmp")).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    public void test_write_to_tape_OK() {
        when(tapeDriveConf.getDdPath()).thenReturn(COMMAND_DD);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST_0);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        DdTapeLibraryService ddTapeLibraryService = new DdTapeLibraryService(
            tapeDriveConf,
            processExecutor,
            "/tmp",
            "/tmp"
        );

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        assertThatCode(() -> ddTapeLibraryService.writeToTape(MY_FAKE_FILE_TAR)).doesNotThrowAnyException();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_DD);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("if=/tmp/my_fake_file.tar", "of=/dev/nst0");
    }

    @Test
    public void test_write_to_tape_KO() {
        when(tapeDriveConf.getDdPath()).thenReturn(COMMAND_DD);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST_0);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        DdTapeLibraryService ddTapeLibraryService = new DdTapeLibraryService(
            tapeDriveConf,
            processExecutor,
            "fakepath",
            "fakepath"
        );

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(1);
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        assertThatThrownBy(() -> ddTapeLibraryService.writeToTape(MY_FAKE_FILE_TAR)).isInstanceOf(
            TapeCommandException.class
        );

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );
        assertThat(commandPath.getValue()).isEqualTo(COMMAND_DD);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("of=/dev/nst0");
    }

    @Test
    public void test_read_from_tape_KO() {
        when(tapeDriveConf.getDdPath()).thenReturn(COMMAND_DD);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST_0);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000L);

        DdTapeLibraryService ddTapeLibraryService = new DdTapeLibraryService(
            tapeDriveConf,
            processExecutor,
            "/tmp",
            "/tmp"
        );

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        assertThatCode(() -> ddTapeLibraryService.readFromTape(MY_FAKE_FILE_TAR)).doesNotThrowAnyException();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1)).execute(
            commandPath.capture(),
            timeout.capture(),
            args.capture()
        );

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_DD);
        assertThat(timeout.getValue()).isEqualTo(1_000L);
        assertThat(args.getValue()).contains("of=/tmp/my_fake_file.tar", "if=/dev/nst0");
    }
}
