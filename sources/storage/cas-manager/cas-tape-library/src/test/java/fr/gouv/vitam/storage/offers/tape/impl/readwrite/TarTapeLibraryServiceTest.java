package fr.gouv.vitam.storage.offers.tape.impl.readwrite;



import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.offers.tape.dto.CommandResponse;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class TarTapeLibraryServiceTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TarTapeLibraryServiceTest.class);
    public static final long TIMEOUT_IN_MILLISECONDS = 1000L;

    @Rule
    public TempFolderRule tempFolderRule = new TempFolderRule();

    @Test
    public void testServiceConstructor() {
        try {
            new TarTapeLibraryService(null, ProcessExecutor.getInstance());
            fail("should fail constructor: tapeDriveConf");
        } catch (Exception e) {
        }

        try {
            new TarTapeLibraryService(new TapeDriveConf(), null);
            fail("should fail constructor: processExecutor");
        } catch (Exception e) {
        }

        new TarTapeLibraryService(new TapeDriveConf(), ProcessExecutor.getInstance());
    }

    @Test
    public void testWriteToTapeOK() throws IOException {
        TapeDriveConf tapeDriveConf = new TapeDriveConf();
        tapeDriveConf.setTimeoutInMilliseconds(TIMEOUT_IN_MILLISECONDS);
        String device = tempFolderRule.newFile().getAbsolutePath();
        tapeDriveConf.setDevice(device);
        TarTapeLibraryService tarTapeLibraryService =
            new TarTapeLibraryService(tapeDriveConf, ProcessExecutor.getInstance());

        String workingDir = PropertiesUtils.getResourceFile("tar/").getAbsolutePath();
        CommandResponse response =
            tarTapeLibraryService.writeToTape(tapeDriveConf.getTimeoutInMilliseconds(), workingDir + "/", "testtar.tar");

        assertThat(response).isNotNull();
        assertThat(response.getOutput()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.OK);

        try (ArchiveInputStream archiveInputStream = new VitamArchiveStreamFactory()
            .createArchiveInputStream(CommonMediaType.TAR_TYPE, new FileInputStream(device))) {
            ArchiveEntry entry;
            while ((entry = archiveInputStream.getNextEntry()) != null) {
                if (archiveInputStream.canReadEntryData(entry)) {
                    if (!entry.isDirectory()) {
                        assertThat(entry.getName()).contains("testtar.tar");
                    }
                }
            }
        } catch (ArchiveException e) {
            LOGGER.error(e);
            fail("should not throw an exception");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteToTapeIllegalArgumentException() throws FileNotFoundException {
        TapeDriveConf tapeDriveConf = new TapeDriveConf();
        tapeDriveConf.setTimeoutInMilliseconds(TIMEOUT_IN_MILLISECONDS);
        TarTapeLibraryService tarTapeLibraryService =
            new TarTapeLibraryService(tapeDriveConf, ProcessExecutor.getInstance());

        String workingDir = PropertiesUtils.getResourceFile("tar/").getAbsolutePath();
        tarTapeLibraryService.writeToTape(tapeDriveConf.getTimeoutInMilliseconds(), workingDir + "/", "testtar.tar");
    }

    @Test
    public void testWriteToTapeKO() throws IOException {
        TapeDriveConf tapeDriveConf = new TapeDriveConf();
        tapeDriveConf.setTimeoutInMilliseconds(TIMEOUT_IN_MILLISECONDS);
        String device = tempFolderRule.newFile().getAbsolutePath();
        tapeDriveConf.setDevice(device);
        TarTapeLibraryService tarTapeLibraryService =
            new TarTapeLibraryService(tapeDriveConf, ProcessExecutor.getInstance());

        CommandResponse response =
            tarTapeLibraryService.writeToTape(tapeDriveConf.getTimeoutInMilliseconds(), "", "testtar.tar");

        assertThat(response).isNotNull();
        assertThat(response.getOutput()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getOutput().getStderr())
            .contains("tar: testtar.tar: Cannot stat: No such file or directory");

    }

    @Test
    public void testReadFromTapeOK() {

    }


    @Test
    public void testReadFromTapeKO() {

    }


    @Test
    public void testlistFromTapeOK() {

    }

    @Test
    public void testlistFromTapeKO() {

    }
}