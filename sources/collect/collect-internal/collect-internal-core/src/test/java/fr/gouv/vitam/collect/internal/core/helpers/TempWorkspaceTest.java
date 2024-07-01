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
package fr.gouv.vitam.collect.internal.core.helpers;

import fr.gouv.vitam.common.tmp.TempFolderRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class TempWorkspaceTest {

    @Rule
    public TempFolderRule tempFolder = new TempFolderRule();

    @Test
    public void testReadWrite() throws IOException {
        // Given one instance with 2 files
        TempWorkspace instance = new TempWorkspace();

        // When writing 2 files
        File file1 = instance.writeToFile(
            "file1.txt",
            new ByteArrayInputStream("toto".getBytes(StandardCharsets.UTF_8))
        );
        File file2 = instance.writeToFile(
            "file2.txt",
            new ByteArrayInputStream("tata".getBytes(StandardCharsets.UTF_8))
        );

        // Then check file isolation
        assertThat(file1).hasName("file1.txt");
        assertThat(file1).hasContent("toto");

        assertThat(file2).hasName("file2.txt");
        assertThat(file2).hasContent("tata");

        // When reading existing file
        File anotherFile1 = instance.getFile("file1.txt");

        // Then same filename returned
        assertThat(anotherFile1.getAbsolutePath()).isEqualTo(file1.getAbsolutePath());

        // Ensure files are stored in temp folder
        File parentDir = file1.getParentFile();
        assertThat(parentDir.getAbsolutePath()).startsWith(tempFolder.getRoot().getAbsolutePath());

        // When closing instance
        instance.close();

        // Then files are cleaned up
        assertThat(file1).doesNotExist();
        assertThat(file2).doesNotExist();
        assertThat(parentDir).doesNotExist();
    }

    @Test
    public void testIsolationOfMultipleInstances() throws IOException {
        // Given 2 instances with some files
        TempWorkspace instance1 = new TempWorkspace();
        TempWorkspace instance2 = new TempWorkspace();

        File instance1File1 = instance1.writeToFile(
            "file1.txt",
            new ByteArrayInputStream("toto".getBytes(StandardCharsets.UTF_8))
        );
        File instance1File2 = instance1.writeToFile(
            "file2.txt",
            new ByteArrayInputStream("tata".getBytes(StandardCharsets.UTF_8))
        );
        File instance2File1 = instance2.writeToFile(
            "file1.txt",
            new ByteArrayInputStream("titi".getBytes(StandardCharsets.UTF_8))
        );

        // Check file content isolation
        assertThat(instance1File1).hasName("file1.txt");
        assertThat(instance1File1).hasContent("toto");

        assertThat(instance1File2).hasName("file2.txt");
        assertThat(instance1File2).hasContent("tata");

        assertThat(instance2File1).hasName("file1.txt");
        assertThat(instance2File1).hasContent("titi");

        // When closing instance 2, only its file is deleted
        instance2.close();

        assertThat(instance1File1).exists();
        assertThat(instance1File2).exists();
        assertThat(instance2File1).doesNotExist();

        // When closing instance 1, its files are also cleaned up
        instance1.close();
        assertThat(instance1File1).doesNotExist();
        assertThat(instance1File2).doesNotExist();
    }
}
