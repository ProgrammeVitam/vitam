package fr.gouv.vitam.storage.offers.tape.process;

import java.io.File;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.guid.GUIDFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ProcessExecutorTest {

    @Test
    public void testExecute() {
        String file = "/tmp/" + GUIDFactory.newGUID().getId() + ".test";
        Output out = ProcessExecutor.getInstance().execute("/bin/touch", false, 100l, Lists.newArrayList(file));
        Assertions.assertThat(out).isNotNull();
        Assertions.assertThat(out.getExitCode()).isEqualTo(0);

        File actual = new File(file);

        Assertions.assertThat(actual).exists();

        actual.deleteOnExit();

    }

}