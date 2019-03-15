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
package fr.gouv.vitam.storage.offers.tape.impl;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

public class TapeLibraryIT {
    public static final String OFFER_TAPE_TEST_CONF = "offer-tape-test.conf";
    public static final long TIMEOUT_IN_MILLISECONDS = 60000L;
    public static final Integer SLOT_INDEX = 10;
    public static final Integer DRIVE_INDEX = 2;

    @Rule
    public TempFolderRule tempFolderRule = new TempFolderRule();


    private final TapeLibraryFactory tapeLibraryFacotry;
    private static TapeLibraryConfiguration configuration;

    public TapeLibraryIT() throws IOException {
        configuration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(OFFER_TAPE_TEST_CONF),
                TapeLibraryConfiguration.class);
        tapeLibraryFacotry = TapeLibraryFactory.getInstance();
        tapeLibraryFacotry.initialize(configuration, mock(MongoDbAccess.class));
    }


    @Test
    public void statusInitialization() {
        Assertions.assertThat(tapeLibraryFacotry.getTapeLibraryPool()).hasSize(1);
    }

    @Test
    public void statusRobotStatus() throws InterruptedException {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibraryState state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.getStatus()).isEqualTo(StatusCode.OK);

            // TODO: 25/02/19 Check parse response

            // As only one robot exists, then should return null
            TapeRobotService tapeRobotService1 =
                tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
            assertThat(tapeRobotService1).isNull();
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);

        }


        TapeRobotService tapeRobotService2 =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        try {
            assertThat(tapeRobotService2).isNotNull();
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService2);

        }


        TapeDriveService tapeDriveService1 =
            tapeLibraryPool.checkoutDriveService(0);

        try {
            TapeDriveState driveState1 =
                tapeDriveService1.getDriveCommandService().status();
            assertThat(driveState1).isNotNull();

            assertThat(driveState1.getEntity()).isNotNull();
            assertThat(driveState1.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService1);
        }

        TapeDriveService tapeDriveService2 =
            tapeLibraryPool.checkoutDriveService(1);

        try {
            TapeDriveState driveState2 =
                tapeDriveService2.getDriveCommandService().status();
            assertThat(driveState2).isNotNull();

            assertThat(driveState2.getEntity()).isNotNull();
            assertThat(driveState2.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService2);
        }


        TapeDriveService tapeDriveService3 =
            tapeLibraryPool.checkoutDriveService(2);
        try {
            TapeDriveState driveState3 =
                tapeDriveService3.getDriveCommandService().status();
            assertThat(driveState3).isNotNull();

            assertThat(driveState3.getEntity()).isNotNull();
            assertThat(driveState3.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService3);

        }

        TapeDriveService tapeDriveService4 =
            tapeLibraryPool.checkoutDriveService(3);
        try {
            TapeDriveState driveState4 =
                tapeDriveService4.getDriveCommandService().status();
            assertThat(driveState4).isNotNull();

            assertThat(driveState4.getEntity()).isNotNull();
            assertThat(driveState4.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService4);
        }


    }

    @Test
    public void testLoadUnloadTape() throws InterruptedException {

        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibraryState state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.getStatus()).isEqualTo(StatusCode.OK);



            TapeResponse result = tapeRobotService.getLoadUnloadService()
                .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // Load already loaded tape KO
            result = tapeRobotService.getLoadUnloadService()
                .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
            // TODO: 25/02/19 Check parse response


            // unload tape OK
            result = tapeRobotService.getLoadUnloadService()
                .unloadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response

            // unload already loaded tape KO
            result = tapeRobotService.getLoadUnloadService()
                .unloadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
            // TODO: 25/02/19 Check parse response

        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);

        }
    }

    @Test
    public void test_load_rewind_write_read_rewind_then_unload_tape() throws InterruptedException,
        IOException {

        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        TapeDriveService tapeDriveService = tapeLibraryPool.checkoutDriveService(DRIVE_INDEX);
        assertThat(tapeDriveService).isNotNull();

        try {
            TapeLibraryState state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.getStatus()).isEqualTo(StatusCode.OK);


            // 1 Load Tape
            TapeResponse response = tapeRobotService.getLoadUnloadService()
                .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response



            TapeReadWriteService ddRreadWriteService =
                tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD);

            TapeReadWriteService tarRreadWriteService =
                tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.TAR);


            TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();


            // 2 erase tape
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            //3 Write file to tape

            String workingDir = PropertiesUtils.getResourceFile("tar/").getAbsolutePath();
            response = ddRreadWriteService.writeToTape(workingDir + "/", "testtar.tar");

            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            Assertions.assertThat(response.getStatus()).isEqualTo(StatusCode.OK);


            //4 Rewind
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response



            // 5 Read file from tape with dd command
            String outDir1 = tempFolderRule.newFolder().getAbsolutePath();

            response = ddRreadWriteService.readFromTape(outDir1, "testtar.tar");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response



            //6 Rewind
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // 7 Read file from tape with tar command
            String outDir2 = tempFolderRule.newFolder().getAbsolutePath();
            response = tarRreadWriteService.readFromTape(outDir2, "");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            //8 Rewind
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // 9 Read file from tape with tar command
            String outDir3 = tempFolderRule.newFolder().getAbsolutePath();
            response = tarRreadWriteService.readFromTape(outDir3, "file1");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response

        } finally {
            try {
                tapeRobotService.getLoadUnloadService().unloadTape(SLOT_INDEX, DRIVE_INDEX);
            } finally {
                tapeLibraryPool.pushDriveService(tapeDriveService);
                tapeLibraryPool.pushRobotService(tapeRobotService);
            }


        }
    }
}