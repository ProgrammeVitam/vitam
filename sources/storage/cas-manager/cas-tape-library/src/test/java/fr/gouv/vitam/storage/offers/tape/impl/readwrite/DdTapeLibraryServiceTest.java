package fr.gouv.vitam.storage.offers.tape.impl.readwrite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import org.checkerframework.checker.units.qual.C;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.VerificationModeFactory;

public class DdTapeLibraryServiceTest {

    public static final String COMMAND_DD = "/bin/dd";
    public static final String DEVICE_NST_0 = "/dev/nst0";
    public static final String MY_FAKE_FILE_TAR = "my_fake_file.tar";


    private TapeDriveConf tapeDriveConf = mock(TapeDriveConf.class);
    private ProcessExecutor processExecutor = mock(ProcessExecutor.class);

    @Before
    public void setUp() throws Exception {
        reset(tapeDriveConf);
        reset(processExecutor);
    }

    @Test
    public void test_constructor() {
        ProcessExecutor processExecutor = mock(ProcessExecutor.class);


        new DdTapeLibraryService(tapeDriveConf, processExecutor, "/tmp", "/tmp");

        try {
            new DdTapeLibraryService(tapeDriveConf, processExecutor, "/tmp", null);
            fail("Should fail");
        } catch (Exception e) {
        }

        try {
            new DdTapeLibraryService(tapeDriveConf, processExecutor, null, "/tmp");
            fail("Should fail");
        } catch (Exception e) {
        }

        try {
            new DdTapeLibraryService(tapeDriveConf, null, "/tmp", "/tmp");
            fail("Should fail");
        } catch (Exception e) {
        }

        try {
            new DdTapeLibraryService(null, processExecutor, "/tmp", "/tmp");
            fail("Should fail");
        } catch (Exception e) {
        }
    }

    @Test
    public void test_write_to_tape_OK() {
        when(tapeDriveConf.getDdPath()).thenReturn(COMMAND_DD);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST_0);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000l);

        DdTapeLibraryService ddTapeLibraryService =
            new DdTapeLibraryService(tapeDriveConf, processExecutor, "/tmp", "/tmp");



        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);


        TapeResponse response = ddTapeLibraryService.writeToTape(MY_FAKE_FILE_TAR);

        assertThat(response.isOK()).isTrue();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1))
            .execute(commandPath.capture(), timeout.capture(), args.capture());

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_DD);
        assertThat(timeout.getValue()).isEqualTo(1_000l);
        assertThat(args.getValue()).contains("if=/tmp/my_fake_file.tar", "of=/dev/nst0");
    }

    @Test
    public void test_write_to_tape_KO() {
        when(tapeDriveConf.getDdPath()).thenReturn(COMMAND_DD);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST_0);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000l);

        DdTapeLibraryService ddTapeLibraryService =
            new DdTapeLibraryService(tapeDriveConf, processExecutor, "fakepath", "fakepath");

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(1);
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        TapeResponse response = ddTapeLibraryService.writeToTape(MY_FAKE_FILE_TAR);

        assertThat(response.isOK()).isFalse();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1))
            .execute(commandPath.capture(), timeout.capture(), args.capture());
        assertThat(commandPath.getValue()).isEqualTo(COMMAND_DD);
        assertThat(timeout.getValue()).isEqualTo(1_000l);
        assertThat(args.getValue()).contains("of=/dev/nst0");
    }

    @Test
    public void test_read_from_tape_KO() {
        when(tapeDriveConf.getDdPath()).thenReturn(COMMAND_DD);
        when(tapeDriveConf.getDevice()).thenReturn(DEVICE_NST_0);
        when(tapeDriveConf.getTimeoutInMilliseconds()).thenReturn(1_000l);

        DdTapeLibraryService ddTapeLibraryService =
            new DdTapeLibraryService(tapeDriveConf, processExecutor, "/tmp", "/tmp");

        Output output = mock(Output.class);
        when(output.getExitCode()).thenReturn(0);
        when(processExecutor.execute(anyString(), anyLong(), anyList())).thenReturn(output);

        TapeResponse response = ddTapeLibraryService.readFromTape(MY_FAKE_FILE_TAR);

        assertThat(response.isOK()).isTrue();

        ArgumentCaptor<String> commandPath = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<List> args = ArgumentCaptor.forClass(List.class);

        verify(processExecutor, VerificationModeFactory.times(1))
            .execute(commandPath.capture(), timeout.capture(), args.capture());

        assertThat(commandPath.getValue()).isEqualTo(COMMAND_DD);
        assertThat(timeout.getValue()).isEqualTo(1_000l);
        assertThat(args.getValue()).contains("of=/tmp/my_fake_file.tar", "if=/dev/nst0");
    }

    @Test
    public void test_get_executor() {
        DdTapeLibraryService ddTapeLibraryService =
            new DdTapeLibraryService(tapeDriveConf, processExecutor, "fakepath", "fakepath");
        assertThat(ddTapeLibraryService.getExecutor()).isNotNull();
    }
}