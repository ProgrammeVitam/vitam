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
package fr.gouv.vitam.storage.offers.tape.process;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.guid.GUIDFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;

public class ProcessExecutorTest {

    @Test
    public void testExecute() throws IOException {
        String file = "/tmp/" + GUIDFactory.newGUID().getId() + ".test";
        Output out = ProcessExecutor.getInstance().execute("/bin/touch", 100L, Lists.newArrayList(file));
        Assertions.assertThat(out).isNotNull();
        Assertions.assertThat(out.getExitCode()).isEqualTo(0);

        File actual = new File(file);

        Assertions.assertThat(actual).exists();

        OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(actual.toPath()));

        for (int i = 0; i < 10_000; i++) {
            writer.write(
                "18:42:22.476 [main] DEBUG fr.gouv.vitam.storage.offers.tape.impl.robot.MtxTapeLibraryService - Execute script : /bin/mtx,timeout: 1000, args : [-f, /dev/sg0, status]\n"
            );
        }

        System.err.println("============================");

        out = ProcessExecutor.getInstance().execute("/bin/cat", true, 30000L, Lists.newArrayList(file));

        Assertions.assertThat(out.getStdout()).contains(
            "fr.gouv.vitam.storage.offers.tape.impl.robot.MtxTapeLibraryService"
        );

        actual.deleteOnExit();
    }
}
