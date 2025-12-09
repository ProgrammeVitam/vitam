/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.api;

import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import fr.gouv.vitam.storage.engine.common.api.exception.TapeCommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class InaTapeProxyResourceTest {

    private InaTapeProxyResource resource;
    private InaTapeProxyConfiguration mockConfig;

    @BeforeEach
    void setUp() {
        mockConfig = mock(InaTapeProxyConfiguration.class);
        resource = new InaTapeProxyResource(mockConfig);
    }

    @Test
    void testGetDriveStatus_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.getDriveStatus();
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testMove_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.move(10, false);
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testMoveBackward_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.move(5, true);
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testRewind_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.rewind();
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testGoToEnd_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.goToEnd();
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testEject_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.eject();
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testGetLibraryStatus_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.getLibraryStatus();
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testLoadTape_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.loadTape(1, 1);
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testUnloadTape_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.unloadTape(1, 1);
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testWriteToTape_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.writeToTape("/some/path/file.txt");
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    void testReadFromTape_throwsException() {
        TapeCommandException exception = assertThrows(TapeCommandException.class, () -> {
            resource.readFromTape("/some/path/output.txt");
        });
        assertEquals("Not implemented yet", exception.getMessage());
    }
}
