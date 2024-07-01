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
package fr.gouv.vitam.storage.offers.tape.impl;

import fr.gouv.vitam.common.storage.tapelibrary.TapeDriveConf;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveCommandService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeReadWriteService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.mockito.Mockito.mock;

public class TapeDriveManagerTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File tmpTarOutputDir;
    private File inputTarDir;

    @Before
    public void prepare() throws Exception {
        inputTarDir = temporaryFolder.newFolder("inputTars");
        tmpTarOutputDir = temporaryFolder.newFolder("tmpTarOutput");
    }

    @Test
    public void testConstructorOneOK() {
        TapeDriveManager tapeDriveManager = new TapeDriveManager(
            mock(TapeDriveConf.class),
            inputTarDir.getAbsolutePath(),
            tmpTarOutputDir.getAbsolutePath()
        );
        Assertions.assertThat(tapeDriveManager.getDriveCommandService()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getTapeDriveConf()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getReadWriteService()).isNotNull();
    }

    @Test
    public void testConstructorTwoOK() {
        TapeDriveManager tapeDriveManager = new TapeDriveManager(
            mock(TapeDriveConf.class),
            mock(TapeReadWriteService.class),
            mock(TapeDriveCommandService.class)
        );
        Assertions.assertThat(tapeDriveManager.getDriveCommandService()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getTapeDriveConf()).isNotNull();
        Assertions.assertThat(tapeDriveManager.getReadWriteService()).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorOneNullTapeDriveConfKO() {
        new TapeDriveManager(null, inputTarDir.getAbsolutePath(), tmpTarOutputDir.getAbsolutePath());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTwoNullTapeDriveConfKO() {
        new TapeDriveManager(null, mock(TapeReadWriteService.class), mock(TapeDriveCommandService.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTwoNullTapeReadWriteServiceTwoKO() {
        new TapeDriveManager(mock(TapeDriveConf.class), null, mock(TapeDriveCommandService.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorTwoNullTapeDriveCommandServiceKO() {
        new TapeDriveManager(mock(TapeDriveConf.class), mock(TapeReadWriteService.class), null);
    }
}
