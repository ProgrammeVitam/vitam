package fr.gouv.vitam.storage.offers.tape.impl;

import static org.mockito.Mockito.mock;

import fr.gouv.vitam.common.storage.tapelibrary.TapeRobotConf;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TapeRobotManagerTest {

    @Test
    public void testConstructorOneOK() {
        TapeRobotManager tapeRobotManager =
            new TapeRobotManager(mock(TapeRobotConf.class), mock(TapeLoadUnloadService.class));
        Assertions.assertThat(tapeRobotManager.getLoadUnloadService()).isNotNull();
    }


    @Test
    public void testConstructorTwoOK() {
        TapeRobotManager tapeRobotManager = new TapeRobotManager(mock(TapeRobotConf.class));
        Assertions.assertThat(tapeRobotManager.getLoadUnloadService()).isNotNull();
    }



    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullLoadUnloadServiceKO() {
        new TapeRobotManager(mock(TapeRobotConf.class), null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullTapeRobotConfAndNotNullTapeLoadUnloadServiceKO() {
        new TapeRobotManager(null, mock(TapeLoadUnloadService.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullTapeRobotConfKO() {
        new TapeRobotManager(null);
    }
}