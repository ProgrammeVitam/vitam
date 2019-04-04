package fr.gouv.vitam.storage.offers.tape.impl;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class TapeDriveManagerTest {


    @Test
    public void testConstructorOneOK() {
        TapeDriveManager tapeDriveManager = new TapeDriveManager(mock(TapeDriveConf.class), "", "");
        Assertions.assertThat(tapeDriveManager.getDriveCommandService()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getTapeDriveConf()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)).isNotNull();
        try {
            tapeDriveManager.getReadWriteService(TapeDriveService.ReadWriteCmd.TAR);
            fail("hould fail");
        } catch (IllegalArgumentException e) {
            //NOSONAR
        }
    }


    @Test
    public void testConstructorTwoOK() {
        TapeDriveManager tapeDriveManager =
            new TapeDriveManager(
                mock(TapeDriveConf.class),
                mock(TapeReadWriteService.class),
                mock(TapeDriveCommandService.class));
        Assertions.assertThat(tapeDriveManager.getDriveCommandService()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getTapeDriveConf()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getReadWriteService(TapeDriveService.ReadWriteCmd.DD)).isNotNull();
        try {
            tapeDriveManager.getReadWriteService(TapeDriveService.ReadWriteCmd.TAR);
            fail("hould fail");
        } catch (IllegalArgumentException e) {
            //NOSONAR
        }    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorOneNullTapeDriveConfKO() {
        new TapeDriveManager(null, "", "");
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTwoNullTapeDriveConfKO() {
        new TapeDriveManager(
            null,
            mock(TapeReadWriteService.class),
            mock(TapeDriveCommandService.class));
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTwoNullTapeReadWriteServiceTwoKO() {
        new TapeDriveManager(
            mock(TapeDriveConf.class),
            null,
            mock(TapeDriveCommandService.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTwoNullTapeDriveCommandServiceKO() {
        new TapeDriveManager(
            mock(TapeDriveConf.class),
            mock(TapeReadWriteService.class),
            null);
    }
}