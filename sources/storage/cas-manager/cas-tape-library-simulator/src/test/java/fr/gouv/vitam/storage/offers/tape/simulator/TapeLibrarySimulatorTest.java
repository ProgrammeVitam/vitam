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
package fr.gouv.vitam.storage.offers.tape.simulator;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveStatus;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlotType;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCommandException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

public class TapeLibrarySimulatorTest {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TapeLibrarySimulatorTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private TapeLibrarySimulator tapeLibrarySimulator;
    private Path inputDir;
    private Path tmpOutputDir;

    @Before
    public void before() throws IOException {
        inputDir = temporaryFolder.newFolder("inputDir").toPath();
        tmpOutputDir = temporaryFolder.newFolder("tmpOutputDir").toPath();
        tapeLibrarySimulator = new TapeLibrarySimulator(inputDir, tmpOutputDir, 4, 10, 8, 100_000, "LTO-6", 10);
    }

    @Test
    public void testTapeLibraryStatusAfterInit() throws TapeCommandException {
        // Given

        // When
        TapeLibrarySpec status = getTapeLibraryStatus();

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getDevice()).isEqualTo("Changer");
        assertThat(status.getDriveCount()).isEqualTo(4);
        assertThat(status.getSlotsCount()).isEqualTo(10);
        assertThat(status.getMailBoxCount()).isEqualTo(0);
        assertThat(status.getDrives()).hasSize(4);
        assertThat(status.getDrives())
            .extracting(TapeDrive::getIndex, TapeDrive::getTape)
            .containsExactly(tuple(0, null), tuple(1, null), tuple(2, null), tuple(3, null));

        assertThat(status.getSlots()).hasSize(10);
        assertThat(status.getSlots())
            .extracting(
                TapeSlot::getIndex,
                TapeSlot::getStorageElementType,
                tapeSlot -> tapeSlot.getTape() == null ? null : tapeSlot.getTape().getSlotIndex(),
                tapeSlot -> tapeSlot.getTape() == null ? null : tapeSlot.getTape().getVolumeTag()
            )
            .containsExactly(
                tuple(1, TapeSlotType.SLOT, 1, "TAPE-0"),
                tuple(2, TapeSlotType.SLOT, 2, "TAPE-1"),
                tuple(3, TapeSlotType.SLOT, 3, "TAPE-2"),
                tuple(4, TapeSlotType.SLOT, 4, "TAPE-3"),
                tuple(5, TapeSlotType.SLOT, 5, "TAPE-4"),
                tuple(6, TapeSlotType.SLOT, 6, "TAPE-5"),
                tuple(7, TapeSlotType.SLOT, 7, "TAPE-6"),
                tuple(8, TapeSlotType.SLOT, 8, "TAPE-7"),
                tuple(9, TapeSlotType.SLOT, null, null),
                tuple(10, TapeSlotType.SLOT, null, null)
            );

        assertThatNoFailuresReported();
    }

    @Test
    public void testTapeLibraryStatusAfterFewOperations() throws IOException, TapeCommandException {
        // Given
        loadTape(1, 0);
        loadTape(2, 1);
        loadTape(3, 2);
        loadTape(4, 3);

        writeFile(0, "content1");
        writeFile(0, "content2");
        moveDrive(0, 1, true);

        ejectDrive(1);

        ejectDrive(2);
        unloadTape(10, 2);

        // When
        TapeLibrarySpec status = getTapeLibraryStatus();

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getDevice()).isEqualTo("Changer");
        assertThat(status.getDriveCount()).isEqualTo(4);
        assertThat(status.getSlotsCount()).isEqualTo(10);
        assertThat(status.getMailBoxCount()).isEqualTo(0);
        assertThat(status.getDrives()).hasSize(4);
        assertThat(status.getDrives())
            .extracting(
                TapeDrive::getIndex,
                tapeDrive -> tapeDrive.getTape() == null ? null : tapeDrive.getTape().getVolumeTag()
            )
            .containsExactly(tuple(0, "TAPE-0"), tuple(1, "TAPE-1"), tuple(2, null), tuple(3, "TAPE-3"));

        assertThat(status.getSlots()).hasSize(10);
        assertThat(status.getSlots())
            .extracting(
                TapeSlot::getIndex,
                TapeSlot::getStorageElementType,
                tapeSlot -> tapeSlot.getTape() == null ? null : tapeSlot.getTape().getSlotIndex(),
                tapeSlot -> tapeSlot.getTape() == null ? null : tapeSlot.getTape().getVolumeTag()
            )
            .containsExactly(
                tuple(1, TapeSlotType.SLOT, null, null),
                tuple(2, TapeSlotType.SLOT, null, null),
                tuple(3, TapeSlotType.SLOT, null, null),
                tuple(4, TapeSlotType.SLOT, null, null),
                tuple(5, TapeSlotType.SLOT, 5, "TAPE-4"),
                tuple(6, TapeSlotType.SLOT, 6, "TAPE-5"),
                tuple(7, TapeSlotType.SLOT, 7, "TAPE-6"),
                tuple(8, TapeSlotType.SLOT, 8, "TAPE-7"),
                tuple(9, TapeSlotType.SLOT, null, null),
                tuple(10, TapeSlotType.SLOT, 10, "TAPE-2")
            );

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithEmptyDrive() throws TapeCommandException {
        // Given

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isNull();
        assertThat(status.getFileNumber()).isNull();
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithEmptyTapeLoadedIntoDrive() throws TapeCommandException {
        // Given
        loadTape(2, 0);

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isEqualTo("LTO-6");
        assertThat(status.getFileNumber()).isEqualTo(0);
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithNonEmptyTapeLoadedIntoDriveAtBeginningOfTape()
        throws TapeCommandException, IOException {
        // Given
        loadTape(2, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isEqualTo("LTO-6");
        assertThat(status.getFileNumber()).isEqualTo(0);
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithNonEmptyTapeLoadedIntoDriveAtEndOfFile1() throws TapeCommandException, IOException {
        // Given
        loadTape(2, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);
        Path outFile1 = readFile(0);

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isEqualTo("LTO-6");
        assertThat(status.getFileNumber()).isEqualTo(1);
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        assertThat(outFile1).hasContent("content1");

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithNonEmptyTapeLoadedIntoDriveAtEndOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(2, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        goToEnd(0);

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isEqualTo("LTO-6");
        assertThat(status.getFileNumber()).isEqualTo(2);
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithTapeEjectedFromDriveButNotYetUnloaded() throws TapeCommandException, IOException {
        // Given
        loadTape(2, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        ejectDrive(0);

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isNull();
        assertThat(status.getFileNumber()).isNull();
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testDriveStatusWithTapeEjectedAndUnloadedFromDrive() throws TapeCommandException, IOException {
        // Given
        loadTape(2, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        ejectDrive(0);
        unloadTape(2, 0);

        // When
        TapeDriveSpec status = getDriveStatus(0);

        // Then
        assertThat(status).isNotNull();
        assertThat(status.getCartridge()).isNull();
        assertThat(status.getFileNumber()).isNull();
        assertThat(status.getErrorCountSinceLastStatus()).isEqualTo(0);
        assertThat(status.getDescription()).isEqualTo("DRIVE-0");
        assertThat(status.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testLoadTapeFromSlotIntoEmptyDrive() throws TapeCommandException {
        // Given

        // When / Then
        assertThatCode(() -> loadTape(2, 0)).doesNotThrowAnyException();

        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec).isNotNull();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-1");
        assertThat(tapeLibrarySpec.getSlots().get(1).getIndex()).isEqualTo(2);
        assertThat(tapeLibrarySpec.getSlots().get(1).getTape()).isNull();

        assertThatNoFailuresReported();
    }

    @Test
    public void testLoadTapeFromEmptySlotThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> loadTape(9, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec).isNotNull();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();
        assertThat(tapeLibrarySpec.getSlots().get(8).getIndex()).isEqualTo(9);
        assertThat(tapeLibrarySpec.getSlots().get(8).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testLoadTapeFromSlotIntoLoadedDriveThenKO() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");

        // When / Then
        assertThatThrownBy(() -> loadTape(9, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(1);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec).isNotNull();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");
        assertThat(tapeLibrarySpec.getSlots().get(8).getIndex()).isEqualTo(9);
        assertThat(tapeLibrarySpec.getSlots().get(8).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testLoadTapeFromSlotIntoEjectedButNotUnloadedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> loadTape(9, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec).isNotNull();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");
        assertThat(tapeLibrarySpec.getSlots().get(8).getIndex()).isEqualTo(9);
        assertThat(tapeLibrarySpec.getSlots().get(8).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testLoadTapeFromSlotIntoUnloadedDriveThenOK() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(10, 0);

        // When / Then
        assertThatCode(() -> loadTape(2, 0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-1");

        assertThat(tapeLibrarySpec.getSlots().get(8).getIndex()).isEqualTo(9);
        assertThat(tapeLibrarySpec.getSlots().get(8).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(9).getIndex()).isEqualTo(10);
        assertThat(tapeLibrarySpec.getSlots().get(9).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThatNoFailuresReported();
    }

    @Test
    public void testUnloadTapeFromEjectedDriveIntoSlot() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatCode(() -> unloadTape(10, 0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(9).getIndex()).isEqualTo(10);
        assertThat(tapeLibrarySpec.getSlots().get(9).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThatNoFailuresReported();
    }

    @Test
    public void testUnloadTapeFromNonEjectedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> unloadTape(10, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThat(tapeLibrarySpec.getSlots().get(9).getIndex()).isEqualTo(10);
        assertThat(tapeLibrarySpec.getSlots().get(9).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testUnloadTapeFromEmptyDriveIntoSlotThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> unloadTape(9, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(8).getIndex()).isEqualTo(9);
        assertThat(tapeLibrarySpec.getSlots().get(8).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testUnloadTapeFromAlreadyUnloadedDriveIntoSlotThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(10, 0);

        // When / Then
        assertThatThrownBy(() -> unloadTape(9, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(8).getIndex()).isEqualTo(9);
        assertThat(tapeLibrarySpec.getSlots().get(8).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(9).getIndex()).isEqualTo(10);
        assertThat(tapeLibrarySpec.getSlots().get(9).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThatFailuresReported(1);
    }

    @Test
    public void testUnloadTapeIntoFullSlotThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> unloadTape(5, 0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThat(tapeLibrarySpec.getSlots().get(0).getIndex()).isEqualTo(1);
        assertThat(tapeLibrarySpec.getSlots().get(0).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(4).getIndex()).isEqualTo(5);
        assertThat(tapeLibrarySpec.getSlots().get(4).getTape().getVolumeTag()).isEqualTo("TAPE-4");

        assertThatFailuresReported(1);
    }

    @Test
    public void testEjectLoadedTapeFromDrive() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatCode(() -> ejectDrive(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThat(tapeLibrarySpec.getSlots().get(0).getIndex()).isEqualTo(1);
        assertThat(tapeLibrarySpec.getSlots().get(0).getTape()).isNull();

        assertThatNoFailuresReported();
    }

    @Test
    public void testEjectFromEmptyDriveThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> ejectDrive(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testEjectAnAlreadyEjectedTapeFromDriveThanKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> ejectDrive(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThat(tapeLibrarySpec.getSlots().get(0).getIndex()).isEqualTo(1);
        assertThat(tapeLibrarySpec.getSlots().get(0).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testEjectFromAnUnloadedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> ejectDrive(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testRewindAnEmptyTape() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatCode(() -> rewindDriveTape(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testRewindANonEmptyTapeAlreadyAtBeginningOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        moveDrive(0, 2, true);

        // When / Then
        assertThatCode(() -> rewindDriveTape(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testRewindANonEmptyTapePositionedAtMiddleOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        moveDrive(0, 1, true);

        // When / Then
        assertThatCode(() -> rewindDriveTape(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testRewindANonEmptyTapePositionedAtEndOfLastFile() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");

        // When / Then
        assertThatCode(() -> rewindDriveTape(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testRewindANonEmptyTapePositionedAtEndOfData() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        goToEnd(0);

        // When / Then
        assertThatCode(() -> rewindDriveTape(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testRewindWithEmptyDriveThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> rewindDriveTape(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testRewindWithEjectedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> rewindDriveTape(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testRewindWithUnloadedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> rewindDriveTape(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThat(tapeLibrarySpec.getSlots().get(0).getIndex()).isEqualTo(1);
        assertThat(tapeLibrarySpec.getSlots().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThatFailuresReported(1);
    }

    @Test
    public void testGoToEndAnEmptyTape() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatCode(() -> goToEnd(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testGoToEndANonEmptyTapePositionedAtBeginningOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);

        // When / Then
        assertThatCode(() -> goToEnd(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testGoToEndANonEmptyTapePositionedAtMiddleOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        moveDrive(0, 1, true);

        // When / Then
        assertThatCode(() -> goToEnd(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testGoToEndANonEmptyTapePositionedAtEndOfLastFile() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");

        // When / Then
        assertThatCode(() -> goToEnd(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testGoToEndANonEmptyTapeAlreadyAtEndOfData() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        goToEnd(0);

        // When / Then
        assertThatCode(() -> goToEnd(0)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testGoToEndWithEmptyDriveThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> goToEnd(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();
        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThatFailuresReported(1);
    }

    @Test
    public void testGoToEndWithEjectedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> goToEnd(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testGoToEndWithUnloadedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> goToEnd(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        // Check tape library status for drive & slot
        TapeLibrarySpec tapeLibrarySpec = getTapeLibraryStatus();

        assertThat(tapeLibrarySpec.getDrives().get(0).getTape()).isNull();

        assertThat(tapeLibrarySpec.getSlots().get(0).getIndex()).isEqualTo(1);
        assertThat(tapeLibrarySpec.getSlots().get(0).getTape().getVolumeTag()).isEqualTo("TAPE-0");

        assertThatFailuresReported(1);
    }

    @Test
    public void testMove0FilesForwardThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 0, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testMove0FilesBackwardThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 0, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testMoveNegativeNumberOfFilesForwardThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, -1, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testMoveNegativeNumberOfFilesBackwardThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, -1, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testMoveMultiplePositionsForwardForEmptyTapeThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 2, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testTryMoveTape1PositionForwardToCheckForEmptinessThenNonBlockingError() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 1, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testMovePositionBackwardForEmptyTapeThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 1, true)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testMovePositionBackwardToMiddleOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");

        // When / Then
        assertThatCode(() -> moveDrive(0, 1, true)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(1);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testMovePositionBackwardToBeginningOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");

        // When / Then
        assertThatCode(() -> moveDrive(0, 2, true)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testMovePositionBackwardToPastBeginningOfTapeThenKO() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 3, true)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.BOT
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testMovePositionForwardToMiddleOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);

        // When / Then
        assertThatCode(() -> moveDrive(0, 1, false)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(1);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testMovePositionForwardToEnd() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);

        // When / Then
        assertThatCode(() -> moveDrive(0, 2, false)).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatNoFailuresReported();
    }

    @Test
    public void testMovePositionForwardPastEndOfTapeThenKO() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);

        // When / Then
        assertThatThrownBy(() -> moveDrive(0, 3, false)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testReadFromEmptyTapeThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(0);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testReadFromTapePositionedAfterLastFileThenKO() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");

        // When / Then
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testReadFromTapePositionedAtEndOfDataThenKO() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        goToEnd(0);

        // When / Then
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOD
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testReadFromTapePositionedAtMiddleOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        moveDrive(0, 1, true);

        // When / Then
        Path readFile2 = readFile(0);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        assertThat(readFile2).hasContent("content2");

        assertThatNoFailuresReported();
    }

    @Test
    public void testReadFromTapePositionedAtBeginningOfTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        rewindDriveTape(0);

        // When / Then
        Path readFile1 = readFile(0);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(1);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        assertThat(readFile1).hasContent("content1");

        assertThatNoFailuresReported();
    }

    @Test
    public void testReadFromEmptyDriveThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testReadFromEjectedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testReadFromUnloadedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testWriteToEmptyTape() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);

        // When / Then
        assertThatCode(() -> writeFile(0, "content1")).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(1);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        // Test read
        rewindDriveTape(0);
        readFileAndVerifyContent(0, "content1");

        assertThatNoFailuresReported();
    }

    @Test
    public void testWriteOnTapePositionedAfterLastFile() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");

        // When / Then
        assertThatCode(() -> writeFile(0, "content2")).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        // Test read
        rewindDriveTape(0);
        readFileAndVerifyContent(0, "content1");
        readFileAndVerifyContent(0, "content2");

        assertThatNoFailuresReported();
    }

    @Test
    public void testWriteToTapePositionedAtEndOfData() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        goToEnd(0);

        // When / Then
        assertThatCode(() -> writeFile(0, "content2")).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(2);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        // Test read
        rewindDriveTape(0);
        readFileAndVerifyContent(0, "content1");
        readFileAndVerifyContent(0, "content2");

        assertThatNoFailuresReported();
    }

    @Test
    public void testWriteToTapePositionedAtMiddleOfTapeThenOverrideExistingData()
        throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        writeFile(0, "content1");
        writeFile(0, "content2");
        writeFile(0, "content3");
        writeFile(0, "content4");
        moveDrive(0, 3, true);

        // When / Then
        assertThatCode(() -> writeFile(0, "content2-new")).doesNotThrowAnyException();
        assertThatCode(() -> writeFile(0, "content3-new")).doesNotThrowAnyException();

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(3);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        // Test read
        rewindDriveTape(0);
        readFileAndVerifyContent(0, "content1");
        readFileAndVerifyContent(0, "content2-new");
        readFileAndVerifyContent(0, "content3-new");

        assertThatNoFailuresReported();

        // Ensure no more data available
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);
        assertThatThrownBy(() -> moveDrive(0, 1, false)).isInstanceOf(TapeCommandException.class);

        assertThatFailuresReported(2);
    }

    @Test
    public void testWriteToFullTapeThenFailure() throws TapeCommandException, IOException {
        // Given
        loadTape(1, 0);
        String random3KString = RandomStringUtils.randomAlphabetic(3000);
        for (int i = 0; i < 33; i++) {
            writeFile(0, random3KString);
        }

        // When / Then
        assertThatThrownBy(() -> writeFile(0, random3KString)).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isEqualTo("LTO-6");
        assertThat(driveStatus.getFileNumber()).isEqualTo(34);
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.ONLINE,
            TapeDriveStatus.IM_REP_EN,
            TapeDriveStatus.EOF
        );

        // Test read
        rewindDriveTape(0);
        for (int i = 0; i < 33; i++) {
            readFileAndVerifyContent(0, random3KString);
        }
        // Last file is incomplete
        readFileAndVerifyContent(0, random3KString.substring(0, 1000));

        assertThat(tapeLibrarySimulator.getFailures()).isEmpty();

        // Ensure no more writes allowed
        assertThatThrownBy(() -> writeFile(0, "content")).isInstanceOf(TapeCommandException.class);

        // Ensure no more data available
        assertThatThrownBy(() -> readFile(0)).isInstanceOf(TapeCommandException.class);
        assertThatThrownBy(() -> moveDrive(0, 1, false)).isInstanceOf(TapeCommandException.class);

        assertThatFailuresReported(2);
    }

    @Test
    public void testWriteToEmptyDriveThenKO() throws TapeCommandException {
        // Given

        // When / Then
        assertThatThrownBy(() -> writeFile(0, "content1")).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testWriteToEjectedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);

        // When / Then
        assertThatThrownBy(() -> writeFile(0, "content1")).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void testWriteToFromUnloadedDriveThenKO() throws TapeCommandException {
        // Given
        loadTape(1, 0);
        ejectDrive(0);
        unloadTape(1, 0);

        // When / Then
        assertThatThrownBy(() -> writeFile(0, "content1")).isInstanceOf(TapeCommandException.class);

        // Check drive status
        TapeDriveSpec driveStatus = getDriveStatus(0);
        assertThat(driveStatus.getCartridge()).isNull();
        assertThat(driveStatus.getFileNumber()).isNull();
        assertThat(driveStatus.getDriveStatuses()).containsExactlyInAnyOrder(
            TapeDriveStatus.DR_OPEN,
            TapeDriveStatus.IM_REP_EN
        );

        assertThatFailuresReported(1);
    }

    @Test
    public void complexConcurrentWorkflow() {
        // Given
        this.tapeLibrarySimulator = new TapeLibrarySimulator(inputDir, tmpOutputDir, 4, 10, 8, 100_000, "LTO-6", 500);

        CountDownLatch tape2UnloadedFromDrive2 = new CountDownLatch(1);

        Runnable job1 = () -> {
            try {
                loadTape(1, 0);

                for (int i = 1; i <= 5; i++) {
                    writeFile(0, "tape1-content" + i);
                }

                moveDrive(0, 3, true);
                readFileAndVerifyContent(0, "tape1-content3");

                moveDrive(0, 1, false);
                readFileAndVerifyContent(0, "tape1-content5");

                goToEnd(0);
                writeFile(0, "tape1-content6");

                rewindDriveTape(0);
                readFileAndVerifyContent(0, "tape1-content1");

                ejectDrive(0);
                unloadTape(1, 0);
            } catch (TapeCommandException | IOException e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }
        };

        Runnable job2 = () -> {
            try {
                loadTape(2, 1);

                for (int i = 1; i <= 2; i++) {
                    writeFile(1, "tape2-content" + i);
                }

                ejectDrive(1);
                unloadTape(2, 1);

                tape2UnloadedFromDrive2.countDown();

                loadTape(6, 1);

                for (int i = 1; i <= 2; i++) {
                    writeFile(1, "tape6-content" + i);
                }
            } catch (TapeCommandException | IOException e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }
        };

        Runnable job3 = () -> {
            try {
                if (!tape2UnloadedFromDrive2.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timeout");
                }

                loadTape(2, 2);

                moveDrive(2, 1, false);
                readFileAndVerifyContent(2, "tape2-content2");
            } catch (TapeCommandException | IOException | InterruptedException e) {
                LOGGER.error(e);
                throw new RuntimeException(e);
            }
        };

        CompletableFuture<Void> job1CompletableFuture = CompletableFuture.runAsync(job1);
        CompletableFuture<Void> job2CompletableFuture = CompletableFuture.runAsync(job2);
        CompletableFuture<Void> job3CompletableFuture = CompletableFuture.runAsync(job3);

        CompletableFuture<Void> allJobsDone = CompletableFuture.allOf(
            job1CompletableFuture,
            job2CompletableFuture,
            job3CompletableFuture
        );
        allJobsDone.join();
        assertThat(allJobsDone).isCompleted();
        assertThat(allJobsDone).hasNotFailed();

        assertThatNoFailuresReported();
    }

    private TapeLibrarySpec getTapeLibraryStatus() throws TapeCommandException {
        // Operations on charger must be synchronized
        synchronized (this) {
            return tapeLibrarySimulator.getTapeLoadUnloadService().status();
        }
    }

    private void loadTape(int slotNumber, int driveIndex) throws TapeCommandException {
        // Operations on charger must be synchronized
        synchronized (this) {
            tapeLibrarySimulator.getTapeLoadUnloadService().loadTape(slotNumber, driveIndex);
        }
    }

    private void unloadTape(int slotNumber, int driveIndex) throws TapeCommandException {
        // Operations on charger must be synchronized
        synchronized (this) {
            tapeLibrarySimulator.getTapeLoadUnloadService().unloadTape(slotNumber, driveIndex);
        }
    }

    private TapeDriveSpec getDriveStatus(int driveIndex) throws TapeCommandException {
        return tapeLibrarySimulator.getTapeDriveCommandServices().get(driveIndex).status();
    }

    private void moveDrive(int driveIndex, int nbPositions, boolean backward) throws TapeCommandException {
        tapeLibrarySimulator.getTapeDriveCommandServices().get(driveIndex).move(nbPositions, backward);
    }

    private void rewindDriveTape(int driveIndex) throws TapeCommandException {
        tapeLibrarySimulator.getTapeDriveCommandServices().get(driveIndex).rewind();
    }

    private void ejectDrive(int driveIndex) throws TapeCommandException {
        tapeLibrarySimulator.getTapeDriveCommandServices().get(driveIndex).eject();
    }

    private void goToEnd(int driveIndex) throws TapeCommandException {
        tapeLibrarySimulator.getTapeDriveCommandServices().get(driveIndex).goToEnd();
    }

    private void writeFile(int driveIndex, String content) throws IOException, TapeCommandException {
        String randomFileName = GUIDFactory.newGUID().toString();
        Path fileToWrite = inputDir.resolve(randomFileName);
        Files.writeString(fileToWrite, content, StandardCharsets.UTF_8);
        tapeLibrarySimulator.getTapeReadWriteServices().get(driveIndex).writeToTape(randomFileName);
        Files.delete(fileToWrite);
    }

    private Path readFile(int driveIndex) throws TapeCommandException {
        String randomFileName = GUIDFactory.newGUID().toString();
        tapeLibrarySimulator.getTapeReadWriteServices().get(driveIndex).readFromTape(randomFileName);
        Path tmpFile = tmpOutputDir.resolve(randomFileName);
        assertThat(tmpFile).exists();
        return tmpFile;
    }

    private void readFileAndVerifyContent(int driveIndex, String expectedContent)
        throws IOException, TapeCommandException {
        Path readFile = readFile(driveIndex);
        assertThat(readFile).hasContent(expectedContent);
    }

    private void assertThatNoFailuresReported() {
        assertThat(tapeLibrarySimulator.getFailures()).isEmpty();
    }

    private void assertThatFailuresReported(int nbFailures) {
        assertThat(tapeLibrarySimulator.getFailures()).hasSize(nbFailures);
    }
}
