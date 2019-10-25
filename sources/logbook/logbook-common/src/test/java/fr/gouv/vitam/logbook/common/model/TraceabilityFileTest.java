/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.logbook.common.model;

import static fr.gouv.vitam.common.PropertiesUtils.getResourceAsStream;
import static fr.gouv.vitam.logbook.common.model.TraceabilityFile.SECURISATION_VERSION_LABEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.security.merkletree.MerkleTree;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * TraceabilityFile ClassTest
 */
public class TraceabilityFileTest {

    private static final String LOGBOOK_OPERATION = "logbookOperationKO.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void should_generate_zip_file() throws Exception {

        // Given
        File destination = folder.newFile();

        // When
        TraceabilityFile traceabilityFile = new TraceabilityFile(destination);
        traceabilityFile.initStoreLog();
        byte[] line = StreamUtils.toString(getResourceAsStream(LOGBOOK_OPERATION)).getBytes();
        traceabilityFile.storeLog(line);
        traceabilityFile.closeStoreLog();

        MerkleTree merkleTree = mock(MerkleTree.class);
        traceabilityFile.storeMerkleTree(merkleTree);

        byte[] timeStampToken = "2016-11-21T16:19:14.469".getBytes();

        traceabilityFile.storeTimeStampToken(timeStampToken);

        traceabilityFile.storeAdditionalInformation(1, "2016-11-21T16:19:13.469", "2016-11-21T16:19:14.469");
        traceabilityFile.storeComputedInformation("hah11111", "hahss11221", "h12334", "hs12334SS");
        traceabilityFile.close();

        //Then
        ZipFile zipFile = new ZipFile(destination.getAbsolutePath());
        assertThat(zipFile.size()).isEqualTo(5);
        ZipEntry entry = zipFile.getEntry("additional_information.txt");
        InputStream stream = zipFile.getInputStream(entry);

        Properties prop = new Properties();
        prop.load(stream);
        assertThat(prop.getProperty("startDate")).isEqualTo("2016-11-21T16:19:13.469");
        assertThat(prop.getProperty("numberOfElements")).isEqualTo("1");
        assertThat(prop.getProperty("endDate")).isEqualTo("2016-11-21T16:19:14.469");
        assertThat(prop.getProperty(SECURISATION_VERSION_LABEL)).isEqualTo("V1");

    }

}
