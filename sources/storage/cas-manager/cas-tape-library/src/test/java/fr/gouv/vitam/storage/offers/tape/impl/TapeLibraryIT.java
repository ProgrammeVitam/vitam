package fr.gouv.vitam.storage.offers.tape.impl;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.dto.CommandResponse;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveState;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
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
    public static final long TIMEOUT_IN_MILLISECONDES = 60000L;
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
        tapeLibraryFacotry.initize(configuration);
    }


    @Test
    public void statusInitialization() {
        Assertions.assertThat(tapeLibraryFacotry.getTapeLibraryPool()).hasSize(1);
    }

    @Test
    public void statusRobotStatus() throws InterruptedException {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDES, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibraryState state = tapeRobotService.getLoadUnloadService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(state).isNotNull();
            assertThat(state.getOutput()).isNotNull();
            assertThat(state.getStatus()).isEqualTo(StatusCode.OK);

            // TODO: 25/02/19 Check parse response

            // As only one robot exists, then should return null
            TapeRobotService tapeRobotService1 =
                tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDES, TimeUnit.MILLISECONDS);
            assertThat(tapeRobotService1).isNull();
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);

        }


        TapeRobotService tapeRobotService2 =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDES, TimeUnit.MILLISECONDS);
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
                tapeDriveService1.getDriveCommandService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(driveState1).isNotNull();

            assertThat(driveState1.getOutput()).isNotNull();
            assertThat(driveState1.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService1);
        }

        TapeDriveService tapeDriveService2 =
            tapeLibraryPool.checkoutDriveService(1);

        try {
            TapeDriveState driveState2 =
                tapeDriveService2.getDriveCommandService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(driveState2).isNotNull();

            assertThat(driveState2.getOutput()).isNotNull();
            assertThat(driveState2.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService2);
        }


        TapeDriveService tapeDriveService3 =
            tapeLibraryPool.checkoutDriveService(2);
        try {
            TapeDriveState driveState3 =
                tapeDriveService3.getDriveCommandService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(driveState3).isNotNull();

            assertThat(driveState3.getOutput()).isNotNull();
            assertThat(driveState3.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService3);

        }

        TapeDriveService tapeDriveService4 =
            tapeLibraryPool.checkoutDriveService(3);
        try {
            TapeDriveState driveState4 =
                tapeDriveService4.getDriveCommandService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(driveState4).isNotNull();

            assertThat(driveState4.getOutput()).isNotNull();
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
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDES, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibraryState state = tapeRobotService.getLoadUnloadService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(state).isNotNull();
            assertThat(state.getOutput()).isNotNull();
            assertThat(state.getStatus()).isEqualTo(StatusCode.OK);



            CommandResponse result = tapeRobotService.getLoadUnloadService()
                .loadTape(TIMEOUT_IN_MILLISECONDES, SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // Load already loaded tape KO
            result = tapeRobotService.getLoadUnloadService()
                .loadTape(TIMEOUT_IN_MILLISECONDES, SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
            // TODO: 25/02/19 Check parse response


            // unload tape OK
            result = tapeRobotService.getLoadUnloadService()
                .unloadTape(TIMEOUT_IN_MILLISECONDES, SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response

            // unload already loaded tape KO
            result = tapeRobotService.getLoadUnloadService()
                .unloadTape(TIMEOUT_IN_MILLISECONDES, SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.KO);
            // TODO: 25/02/19 Check parse response

        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);

        }
    }

    @Test
    public void test_load_erase_write_read_rewind_then_unload_tape() throws InterruptedException,
        IOException {

        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
            tapeLibraryPool.checkoutRobotService(TIMEOUT_IN_MILLISECONDES, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        TapeDriveService tapeDriveService = tapeLibraryPool.checkoutDriveService(DRIVE_INDEX);
        assertThat(tapeDriveService).isNotNull();

        try {
            TapeLibraryState state = tapeRobotService.getLoadUnloadService().status(TIMEOUT_IN_MILLISECONDES);
            assertThat(state).isNotNull();
            assertThat(state.getOutput()).isNotNull();
            assertThat(state.getStatus()).isEqualTo(StatusCode.OK);


            // 1 Load Tape
            CommandResponse response = tapeRobotService.getLoadUnloadService()
                .loadTape(TIMEOUT_IN_MILLISECONDES, SLOT_INDEX, DRIVE_INDEX);

            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response



            TapeReadWriteService ddRreadWriteService =
                tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD);

            TapeReadWriteService tarRreadWriteService =
                tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.TAR);


            TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();


            // 2 erase tape
            response = driveCommandService.erase(TIMEOUT_IN_MILLISECONDES);
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            //3 Write file to tape

            String workingDir = PropertiesUtils.getResourceFile("tar/").getAbsolutePath();
            response = ddRreadWriteService.writeToTape(TIMEOUT_IN_MILLISECONDES, workingDir + "/", "testtar.tar");

            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            Assertions.assertThat(response.getStatus()).isEqualTo(StatusCode.OK);


            //4 Rewind
            response = driveCommandService.rewind(TIMEOUT_IN_MILLISECONDES);
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response



            // 5 Read file from tape with dd command
            String outDir1 = tempFolderRule.newFolder().getAbsolutePath();

            response = ddRreadWriteService.readFromTape(TIMEOUT_IN_MILLISECONDES, outDir1, "testtar.tar");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response



            //6 Rewind
            response = driveCommandService.rewind(TIMEOUT_IN_MILLISECONDES);
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // 7 Read file from tape with tar command
            String outDir2 = tempFolderRule.newFolder().getAbsolutePath();
            response = tarRreadWriteService.readFromTape(TIMEOUT_IN_MILLISECONDES, outDir2, "");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            //8 Rewind
            response = driveCommandService.rewind(TIMEOUT_IN_MILLISECONDES);
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // 9 Read file from tape with tar command
            String outDir3 = tempFolderRule.newFolder().getAbsolutePath();
            response = tarRreadWriteService.readFromTape(TIMEOUT_IN_MILLISECONDES, outDir3, "file1");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getOutput()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response

        } finally {
            try {
                tapeRobotService.getLoadUnloadService().unloadTape(TIMEOUT_IN_MILLISECONDES, SLOT_INDEX, DRIVE_INDEX);
            } finally {
                tapeLibraryPool.pushDriveService(tapeDriveService);
                tapeLibraryPool.pushRobotService(tapeRobotService);
            }


        }
    }
}