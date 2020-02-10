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
package fr.gouv.vitam.common.stream;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fr.gouv.vitam.common.junit.FakeInputStream;

public class SizedInputStreamTest {

    @Test
    public void getSizeReadByArrayTest() throws Exception {
        try (FakeInputStream fis = new FakeInputStream(1024 * 1024, false)) {
            try (SizedInputStream sis = new SizedInputStream(fis)) {
                assertEquals(0, sis.getSize());

                final byte[] array = new byte[1024 * 1024];

                sis.read(array);
                assertEquals(1024 * 1024, sis.getSize());
            }
        }
    }

    @Test
    public void getSizeReadTest() throws Exception {
        try (FakeInputStream fis = new FakeInputStream(1024 * 1024, false)) {
            try (SizedInputStream sis = new SizedInputStream(fis)) {
                assertEquals(0, sis.getSize());
                sis.read();
                assertEquals(1, sis.getSize());
                sis.read();
                assertEquals(2, sis.getSize());
                for (int i = 2; i < 1024 * 1024; i++) {
                    sis.read();
                }
                assertEquals(1024 * 1024, sis.getSize());
            }
        }
    }

    @Test
    public void getSizeReadByArrayBuffer() throws Exception {
        try (FakeInputStream fis = new FakeInputStream(1024 * 1024, false)) {
            try (SizedInputStream sis = new SizedInputStream(fis)) {
                final byte[] array = new byte[10];
                sis.read(array, 0, 10);
                assertEquals(10, sis.getSize());
                sis.read(array, 0, 10);
                assertEquals(20, sis.getSize());
                sis.read(array, 0, 10);
                assertEquals(30, sis.getSize());
                sis.read(array, 0, 10);
                assertEquals(40, sis.getSize());
                sis.read(array, 0, 10);
                assertEquals(50, sis.getSize());
                sis.read(array, 0, 10);
                assertEquals(60, sis.getSize());
                sis.read(array, 0, 10);
                assertEquals(70, sis.getSize());
                sis.read(array, 0, 6);
                assertEquals(76, sis.getSize());
                for (int i = 76; i < 1024 * 1024; i += 10) {
                    sis.read(array, 0, 10);
                }
                assertEquals(1024 * 1024, sis.getSize());
            }
        }
    }
}
