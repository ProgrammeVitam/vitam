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
        Output out = ProcessExecutor.getInstance().execute("/bin/touch", false, 100l, Lists.newArrayList(file));
        Assertions.assertThat(out).isNotNull();
        Assertions.assertThat(out.getExitCode()).isEqualTo(0);

        File actual = new File(file);

        Assertions.assertThat(actual).exists();

        OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(actual.toPath()));

        for (int i = 0; i < 10_000; i++) {
            writer.write(
                "18:42:22.476 [main] DEBUG fr.gouv.vitam.storage.offers.tape.impl.robot.MtxTapeLibraryService - Execute script : /bin/mtx,timeout: 1000, args : [-f, /dev/sg0, status]\n");
        }

        System.err.println("============================");

        out = ProcessExecutor.getInstance().execute("/bin/cat", false, true, 30000l, Lists.newArrayList(file));

        Assertions.assertThat(out.getStdout())
            .contains("fr.gouv.vitam.storage.offers.tape.impl.robot.MtxTapeLibraryService");

        actual.deleteOnExit();

    }

}
