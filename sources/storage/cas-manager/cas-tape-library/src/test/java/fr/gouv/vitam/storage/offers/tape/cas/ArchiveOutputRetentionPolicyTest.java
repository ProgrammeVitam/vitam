/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
