package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.storage.offers.tape.exception.ReadRequestReferentialException;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.internal.verification.Times;
import org.mockito.internal.verification.VerificationModeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveOutputRetentionPolicyTest {

    @Test
    public void test_put_get_ok() throws ReadRequestReferentialException {
        ReadRequestReferentialCleaner readRequestReferentialCleaner = mock(ReadRequestReferentialCleaner.class);
        when(readRequestReferentialCleaner.cleanUp()).thenReturn(1L);

        ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy =
            new ArchiveOutputRetentionPolicy(1, TimeUnit.MINUTES, 1, readRequestReferentialCleaner);
        archiveOutputRetentionPolicy.put("aaa", Paths.get("aaa"));
        archiveOutputRetentionPolicy.cleanUp();
        Assertions.assertThat(archiveOutputRetentionPolicy.get("aaa")).isNotNull();
        verify(readRequestReferentialCleaner, VerificationModeFactory.atLeastOnce()).cleanUp();
    }

    @Test
    public void test_remove_listener_when_expire_entry()
        throws IOException, InterruptedException, ReadRequestReferentialException {
        ReadRequestReferentialCleaner readRequestReferentialCleaner = mock(ReadRequestReferentialCleaner.class);
        when(readRequestReferentialCleaner.cleanUp()).thenReturn(1L);

        Path tempFile = Files.createTempFile(GUIDFactory.newGUID().getId(), ".tar");
        ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy =
            new ArchiveOutputRetentionPolicy(5, TimeUnit.MILLISECONDS, 1, readRequestReferentialCleaner);
        archiveOutputRetentionPolicy.put(tempFile.toFile().getAbsolutePath(), tempFile);

        TimeUnit.MILLISECONDS.sleep(10);
        Assertions.assertThat(archiveOutputRetentionPolicy.get(tempFile.toFile().getAbsolutePath())).isNull();
        TimeUnit.MILLISECONDS.sleep(20);
        Assertions.assertThat(tempFile.toFile().exists()).isFalse();
        verify(readRequestReferentialCleaner, VerificationModeFactory.atLeastOnce()).cleanUp();
    }

    @Test
    public void test_remove_listener_when_invalidate()
        throws IOException, InterruptedException, ReadRequestReferentialException {
        ReadRequestReferentialCleaner readRequestReferentialCleaner = mock(ReadRequestReferentialCleaner.class);
        when(readRequestReferentialCleaner.cleanUp()).thenReturn(1L);

        Path tempFile = Files.createTempFile(GUIDFactory.newGUID().getId(), ".tar");
        ArchiveOutputRetentionPolicy archiveOutputRetentionPolicy =
            new ArchiveOutputRetentionPolicy(1000, TimeUnit.MILLISECONDS, 1, readRequestReferentialCleaner);
        archiveOutputRetentionPolicy.put(tempFile.toFile().getAbsolutePath(), tempFile);

        // Manually invalidate entry in cache
        archiveOutputRetentionPolicy.invalidate(tempFile.toFile().getAbsolutePath());
        TimeUnit.MILLISECONDS.sleep(10);
        Assertions.assertThat(archiveOutputRetentionPolicy.get(tempFile.toFile().getAbsolutePath())).isNull();

        //cleanUp
        archiveOutputRetentionPolicy.cleanUp();
        TimeUnit.MILLISECONDS.sleep(20);

        // Assert That file is deleted and onRemoval est invoked
        Assertions.assertThat(tempFile.toFile().exists()).isFalse();

        verify(readRequestReferentialCleaner, VerificationModeFactory.atLeastOnce()).cleanUp();

    }
}