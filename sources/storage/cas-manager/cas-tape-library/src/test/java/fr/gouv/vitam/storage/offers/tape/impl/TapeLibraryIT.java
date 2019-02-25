package fr.gouv.vitam.storage.offers.tape.impl;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class TapeLibraryIT {
    public static final String OFFER_TAPE_TEST_CONF = "offer-tape-test.conf";
    public static final long TIMEOUT_IN_MILLISECONDES = 1000L;

    private final TapeLibraryFactory tapeLibraryFacotry;
    private static TapeLibraryConfiguration configuration;

    public TapeLibraryIT() throws IOException {
        configuration =
                PropertiesUtils.readYaml(PropertiesUtils.findFile(OFFER_TAPE_TEST_CONF),
                        TapeLibraryConfiguration.class);
        tapeLibraryFacotry = new TapeLibraryFactory();
        tapeLibraryFacotry.initize(configuration);
    }


    @Test
    public void statusInitialization() {
        Assertions.assertThat(tapeLibraryFacotry.getTapeLibraryPool()).hasSize(1);
    }

    @Test
    public void statusRobotStatus() throws InterruptedException {
        TapeLibraryPool tapeLibraryPool = tapeLibraryFacotry.getFirstTapeLibraryPool();
        TapeRobotService tapeRobotService = tapeLibraryPool.checkoutRobotService(false);
        assertThat(tapeRobotService).isNotNull();
        TapeLibraryState state = tapeRobotService.getLoadUnloadService().status(TIMEOUT_IN_MILLISECONDES);
        assertThat(state).isNotNull();
        assertThat(state.getOutput()).isNotNull();
        assertThat(state.getStatus()).isEqualTo(StatusCode.OK);

        TapeRobotService tapeRobotService1 = tapeLibraryPool.checkoutRobotService(false);
        assertThat(tapeRobotService1).isNull();

        tapeLibraryPool.pushRobotService(tapeRobotService);


    }

    @Test
    public void unloadTape() {
    }
}