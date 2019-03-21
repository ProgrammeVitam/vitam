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

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConf;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.QueueState;
import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.offers.tape.dto.TapeResponse;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import fr.gouv.vitam.storage.offers.tape.impl.catalog.TapeCatalogServiceImpl;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadTask;
import fr.gouv.vitam.storage.offers.tape.worker.tasks.ReadWriteResult;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class TapeLibraryIT {
    public static final String OFFER_TAPE_TEST_CONF = "offer-tape-test.conf";
    public static final long TIMEOUT_IN_MILLISECONDS = 60000L;
    public static final Integer SLOT_INDEX = 10;
    public static final Integer DRIVE_INDEX = 2;
    private static MongoDbAccess mongoDbAccess;

    @ClassRule
    public static TempFolderRule tempFolderRule = new TempFolderRule();


    @ClassRule
    public static MongoRule mongoRule = new MongoRule(getMongoClientOptions(), OfferCollections.OFFER_TAPE_CATALOG.getName());


    private static TapeLibraryFactory tapeLibraryFacotry;
    private static TapeLibraryConfiguration configuration;

    @BeforeClass
    public static void beforeClass() throws IOException {
        configuration =
            PropertiesUtils.readYaml(PropertiesUtils.findFile(OFFER_TAPE_TEST_CONF),
                TapeLibraryConfiguration.class);

        configuration.setInputDirectory(PropertiesUtils.getResourceFile("tar/").getAbsolutePath());
        configuration.setOutputDirectory(tempFolderRule.newFolder().getAbsolutePath());
        tapeLibraryFacotry = TapeLibraryFactory.getInstance();
        mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        tapeLibraryFacotry.initialize(configuration, mongoDbAccess);
    }

    @AfterClass
    public static void setDownAfterClass() {
        mongoRule.handleAfterClass();
    }



    @Test
    public void statusInitialization() {
        Assertions.assertThat(tapeLibraryFacotry.getTapeLibraryPool()).hasSize(1);
    }

    @Test
    public void statusRobotStatus() throws InterruptedException {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
            tapeLibraryPool.checkoutRobotService(5, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        try {
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.isOK()).isTrue();

            // As only one robot exists, then should return null
            TapeRobotService tapeRobotService1 =
                tapeLibraryPool.checkoutRobotService(1, TimeUnit.MILLISECONDS);
            assertThat(tapeRobotService1).isNull();
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService);

        }


        TapeRobotService tapeRobotService2 =
            tapeLibraryPool.checkoutRobotService(5, TimeUnit.MILLISECONDS);
        try {
            assertThat(tapeRobotService2).isNotNull();
        } finally {
            tapeLibraryPool.pushRobotService(tapeRobotService2);

        }


        TapeDriveService tapeDriveService1 =
            tapeLibraryPool.checkoutDriveService(0);

        try {
            TapeDriveSpec driveState1 =
                tapeDriveService1.getDriveCommandService().status();
            assertThat(driveState1).isNotNull();

            assertThat(driveState1.getEntity()).isNotNull();
            assertThat(driveState1.isOK()).isTrue();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService1);
        }

        TapeDriveService tapeDriveService2 =
            tapeLibraryPool.checkoutDriveService(1);

        try {
            TapeDriveSpec driveState2 =
                tapeDriveService2.getDriveCommandService().status();
            assertThat(driveState2).isNotNull();

            assertThat(driveState2.getEntity()).isNotNull();
            assertThat(driveState2.isOK()).isTrue();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService2);
        }


        TapeDriveService tapeDriveService3 =
            tapeLibraryPool.checkoutDriveService(2);
        try {
            TapeDriveSpec driveState3 =
                tapeDriveService3.getDriveCommandService().status();
            assertThat(driveState3).isNotNull();

            assertThat(driveState3.getEntity()).isNotNull();
            assertThat(driveState3.isOK()).isTrue();
        } finally {
            tapeLibraryPool.pushDriveService(tapeDriveService3);

        }

        TapeDriveService tapeDriveService4 =
            tapeLibraryPool.checkoutDriveService(3);
        try {
            TapeDriveSpec driveState4 =
                tapeDriveService4.getDriveCommandService().status();
            assertThat(driveState4).isNotNull();

            assertThat(driveState4.getEntity()).isNotNull();
            assertThat(driveState4.isOK()).isTrue();
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
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.isOK()).isTrue();



            TapeResponse result = tapeRobotService.getLoadUnloadService()
                .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.OK);


            // Load already loaded tape KO
            result = tapeRobotService.getLoadUnloadService()
                .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.KO);


            // unload tape OK
            result = tapeRobotService.getLoadUnloadService()
                .unloadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.OK);

            // unload already loaded tape KO
            result = tapeRobotService.getLoadUnloadService()
                .unloadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(StatusCode.KO);

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
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.isOK()).isTrue();


            // 1 Load Tape
            TapeResponse response = tapeRobotService.getLoadUnloadService()
                .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.isOK()).isTrue();

            TapeReadWriteService ddRreadWriteService =
                tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD);

            TapeReadWriteService tarRreadWriteService =
                tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.TAR);


            TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();


            // 2 erase tape
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.isOK()).isTrue();


            //3 Write file to tape
            response = ddRreadWriteService.writeToTape("testtar.tar");

            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.isOK()).isTrue();


            //4 Rewind
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.isOK()).isTrue();



            // 5 Read file from tape with dd command
            response = ddRreadWriteService.readFromTape("testtar.tar");
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.isOK()).isTrue();
            // TODO: 25/02/19 Check parse response



            //6 Rewind
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
            // TODO: 25/02/19 Check parse response


            // 7 Read file from tape with tar command
            response = tarRreadWriteService.readFromTape("");
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
            response = tarRreadWriteService.readFromTape("file1");
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

    @Test
    public void test_read_files_from_tape() throws InterruptedException, IOException {

        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();

        TapeRobotService tapeRobotService =
                tapeLibraryPool.checkoutRobotService(10, TimeUnit.MILLISECONDS);
        assertThat(tapeRobotService).isNotNull();

        TapeDriveService tapeDriveService = tapeLibraryPool.checkoutDriveService(DRIVE_INDEX);
        assertThat(tapeDriveService).isNotNull();

        try {
            TapeLibrarySpec state = tapeRobotService.getLoadUnloadService().status();
            assertThat(state).isNotNull();
            assertThat(state.getEntity()).isNotNull();
            assertThat(state.isOK()).isTrue();

            // 1 Load Tape
            TapeResponse response = tapeRobotService.getLoadUnloadService()
                    .loadTape(SLOT_INDEX, DRIVE_INDEX);

            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(StatusCode.OK);

            TapeReadWriteService ddReadWriteService =
                    tapeDriveService.getReadWriteService(TapeDriveService.ReadWriteCmd.DD);

            TapeDriveCommandService driveCommandService = tapeDriveService.getDriveCommandService();

            // 2 erase tape
            response = driveCommandService.rewind();
            assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            assertThat(response.isOK()).isTrue();

            //3 Write files to tape
            // file 1
            response = ddReadWriteService.writeToTape("testtar.tar");

            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            Assertions.assertThat(response.isOK()).isTrue();

            // file 2
            response = ddReadWriteService.writeToTape("testtar_2.tar");

            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(response.getEntity()).isNotNull();
            Assertions.assertThat(response.isOK()).isTrue();

            // read file from tape with given position
            TapeCatalogService tapeCatalogService = new TapeCatalogServiceImpl(mongoDbAccess);
            String tapeCode = state.getSlots().get(SLOT_INDEX - 1).getTape().getVolumeTag();
            TapeCatalog workerCurrentTape = tapeCatalogService.find(
                    Arrays.asList(new QueryCriteria(TapeCatalog.CODE, tapeCode, QueryCriteriaOperator.EQ))).get(0);

            ReadTask readTask1 = new ReadTask(new ReadOrder(tapeCode, 0, "testtar.tar"), workerCurrentTape,
                    tapeLibraryPool, tapeDriveService, tapeCatalogService);

            ReadTask readTask2 = new ReadTask(new ReadOrder(tapeCode, 1, "testtar_2.tar"), workerCurrentTape,
                    tapeLibraryPool, tapeDriveService, tapeCatalogService);

            ReadWriteResult result1 = readTask1.get();
            assertThat(result1).isNotNull();
            assertThat(result1.getOrderState()).isEqualTo(QueueState.COMPLETED);

            ReadWriteResult result2 = readTask2.get();
            assertThat(result2).isNotNull();
            assertThat(result2.getOrderState()).isEqualTo(QueueState.COMPLETED);

        } catch (TapeCatalogException e) {
            e.printStackTrace();
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