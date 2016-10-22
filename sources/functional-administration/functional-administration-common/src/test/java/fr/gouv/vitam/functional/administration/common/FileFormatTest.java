/**
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
 **/

package fr.gouv.vitam.functional.administration.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class FileFormatTest {

    @Test
    public void cleanNullFileFormatFieldValues() {
        FileFormat fm = new FileFormat();
        // theoretically never null
        fm.setPUID("PUID");
        assertNull(fm.get(FileFormat.VERSION_PRONOM));
        assertNull(fm.get(FileFormat.VERSION));
        assertNull(fm.get(FileFormat.CREATED_DATE));
        assertNull(fm.get(FileFormat.HAS_PRIORITY_OVER_FILE_FORMAT_ID));
        assertNull(fm.get(FileFormat.MIME_TYPE));
        assertNull(fm.get(FileFormat.NAME));
        assertNull(fm.get(FileFormat.GROUP));
        assertNull(fm.get(FileFormat.ALERT));
        assertNull(fm.get(FileFormat.COMMENT));
        assertNull(fm.get(FileFormat.EXTENSION));
        fm.cleanNullValues();
        assertNotNull(fm.get(FileFormat.VERSION_PRONOM));
        assertNotNull(fm.get(FileFormat.VERSION));
        assertNotNull(fm.get(FileFormat.CREATED_DATE));
        assertNotNull(fm.get(FileFormat.HAS_PRIORITY_OVER_FILE_FORMAT_ID));
        assertNotNull(fm.get(FileFormat.MIME_TYPE));
        assertNotNull(fm.get(FileFormat.NAME));
        assertNotNull(fm.get(FileFormat.GROUP));
        assertNotNull(fm.get(FileFormat.ALERT));
        assertNotNull(fm.get(FileFormat.COMMENT));
        assertNotNull(fm.get(FileFormat.EXTENSION));
    }
}
