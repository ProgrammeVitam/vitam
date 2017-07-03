/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.storage.offer.hash;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import fr.gouv.vitam.common.storage.filesystem.v2.HashFileSystemHelper;
import fr.gouv.vitam.common.storage.filesystem.v2.metadata.container.HashContainerMetadata;

public class HashContainerMetadataIT {
    public static final String CONTAINER = "container";
    public static final String FILE = "test1";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /**
     * THis test work only if your temporary folder support linux extended attribute
     *
     * @throws Exception
     */
    @Test
    public void should_write_and_read_extended_attribute() throws Exception {

        File tmp = temporaryFolder.newFolder();

        File file = new File(tmp, CONTAINER);
        file.mkdir();

        HashFileSystemHelper hashFileSystemHelper = new HashFileSystemHelper(file.getAbsolutePath());
        File container = new File(file, CONTAINER);
        container.mkdir();

        File test1 = new File(container, FILE);
        test1.createNewFile();

        HashContainerMetadata containerName = new HashContainerMetadata(FILE, hashFileSystemHelper);
        containerName.updateAndMarshall(100L, 200L);

        containerName = new HashContainerMetadata(FILE, hashFileSystemHelper, true);
        assertThat(containerName.getNbObjects()).isEqualTo(100L);
        assertThat(containerName.getUsedBytes()).isEqualTo(200L);
    }

}
