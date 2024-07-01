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
package fr.gouv.vitam.common.stream;

import org.apache.commons.io.IOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Write for multiplexed streams.
 *
 * Stream format is "SIZE1(long)DATA1(raw)SIZE2(long)DATA2(raw)..."
 */
public class MultiplexedStreamWriter {

    private static final long LONG_SIZE = 8L;
    private static final long EOF_MARKER = -1L;

    private final DataOutputStream dataOutputStream;

    public MultiplexedStreamWriter(OutputStream outputStream) {
        this.dataOutputStream = new DataOutputStream(outputStream);
    }

    public void appendEntry(long size, InputStream inputStream) throws IOException {
        // Write size
        dataOutputStream.writeLong(size);

        // Write body
        ExactSizeInputStream exactSizeInputStream = new ExactSizeInputStream(inputStream, size);
        IOUtils.copy(exactSizeInputStream, this.dataOutputStream);
    }

    public void appendEndOfFile() throws IOException {
        dataOutputStream.writeLong(EOF_MARKER);
        dataOutputStream.flush();
    }

    public static long getTotalStreamSize(List<Long> fileSizes) {
        long totalSize = 0;
        for (Long fileSize : fileSizes) {
            // Size + content
            totalSize += LONG_SIZE + fileSize;
        }
        // EOF_MARKER (8 bytes)
        totalSize += LONG_SIZE;
        return totalSize;
    }
}
