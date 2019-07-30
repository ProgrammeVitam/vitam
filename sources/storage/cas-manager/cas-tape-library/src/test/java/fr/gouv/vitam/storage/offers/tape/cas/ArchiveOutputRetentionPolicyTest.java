package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.guid.GUIDFactory;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class ArchiveOutputRetentionPolicyTest {

    @Test
    public void test_put_get_ok() {
        ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy =
            new ArchiveOutputRetentionPolicy(1, TimeUnit.MINUTES, 1);
        archiveOutputRetentionPolicy.put("aaa", Paths.get("aaa"));
        archiveOutputRetentionPolicy.cleanUp();
        Assertions.assertThat(archiveOutputRetentionPolicy.get("aaa")).isNotNull();
    }

    @Test
    public void test_remove_listener() throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile(GUIDFactory.newGUID().getId(), ".tar");
        ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy =
            new ArchiveOutputRetentionPolicy(5, TimeUnit.MILLISECONDS, 1);
        archiveOutputRetentionPolicy.put(tempFile.toFile().getAbsolutePath(), tempFile);

        TimeUnit.MILLISECONDS.sleep(10);
        Assertions.assertThat(archiveOutputRetentionPolicy.get(tempFile.toFile().getAbsolutePath())).isNull();
        TimeUnit.MILLISECONDS.sleep(20);
        Assertions.assertThat(tempFile.toFile().exists()).isFalse();

    }
}