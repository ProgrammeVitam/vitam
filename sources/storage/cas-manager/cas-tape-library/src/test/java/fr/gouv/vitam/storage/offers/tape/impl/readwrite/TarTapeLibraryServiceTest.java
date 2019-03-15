/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offers.tape.impl.readwrite;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.compress.VitamArchiveStreamFactory;
import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.junit.Rule;
import org.junit.Test;

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
        TapeResponse response =
            tarTapeLibraryService.writeToTape(workingDir + "/", "testtar.tar");

        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNotNull();
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
        tarTapeLibraryService.writeToTape(workingDir + "/", "testtar.tar");
    }

    @Test
    public void testWriteToTapeKO() throws IOException {
        TapeDriveConf tapeDriveConf = new TapeDriveConf();
        tapeDriveConf.setTimeoutInMilliseconds(TIMEOUT_IN_MILLISECONDS);
        String device = tempFolderRule.newFile().getAbsolutePath();
        tapeDriveConf.setDevice(device);
        TarTapeLibraryService tarTapeLibraryService =
            new TarTapeLibraryService(tapeDriveConf, ProcessExecutor.getInstance());

        TapeResponse response =
            tarTapeLibraryService.writeToTape("", "testtar.tar");

        assertThat(response).isNotNull();
        assertThat(response.getEntity()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getEntity(Output.class).getStderr())
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