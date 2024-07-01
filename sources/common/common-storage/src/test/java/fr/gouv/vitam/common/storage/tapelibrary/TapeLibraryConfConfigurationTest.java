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
package fr.gouv.vitam.common.storage.tapelibrary;

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class TapeLibraryConfConfigurationTest {

    public static final String OFFER_TAPE_TEST_CONF = "offer-tape-test.conf";

    @Test
    public void testLoadOfferTapeConfOK() throws IOException {
        TapeLibraryConfiguration configuration = PropertiesUtils.readYaml(
            PropertiesUtils.findFile(OFFER_TAPE_TEST_CONF),
            TapeLibraryConfiguration.class
        );
        assertThat(configuration).isNotNull();

        assertThat(configuration.getTapeLibraries()).isNotEmpty();
        assertThat(configuration.getTapeLibraries()).hasSize(1);
        assertThat(configuration.getTapeLibraries().keySet()).containsExactly("TAPE_LIB_1");
        Iterator<TapeLibraryConf> tapeLibraryIt = configuration.getTapeLibraries().values().iterator();

        TapeLibraryConf tapeLibraryConf = tapeLibraryIt.next();
        assertThat(tapeLibraryConf.getRobots()).hasSize(1);
        assertThat(tapeLibraryConf.getDrives()).hasSize(4);

        assertThat(tapeLibraryConf.getRobots())
            .extracting("device")
            .containsExactly("/dev/tape/by-id/scsi-1QUANTUM_B4D282EF285E4365BBA90000");

        assertThat(tapeLibraryConf.getDrives())
            .extracting("device", "mtPath", "ddPath", "timeoutInMilliseconds")
            .containsExactly(
                tuple("/dev/tape/by-id/scsi-1IBM_ULT3580-TD6_B4D2800001-nst", "/bin/mt", "/bin/dd", 3600000L),
                tuple("/dev/tape/by-id/scsi-1IBM_ULT3580-TD6_B4D2800002-nst", "/bin/mt", "/bin/dd", 3600000L),
                tuple("/dev/tape/by-id/scsi-1IBM_ULT3580-TD6_B4D2800003-nst", "/bin/mt", "/bin/dd", 3600000L),
                tuple("/dev/tape/by-id/scsi-1IBM_ULT3580-TD6_B4D2800004-nst", "/bin/mt", "/bin/dd", 3600000L)
            );
    }
}
