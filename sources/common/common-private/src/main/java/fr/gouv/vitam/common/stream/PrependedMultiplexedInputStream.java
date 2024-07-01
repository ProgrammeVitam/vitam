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

import org.apache.commons.io.input.ProxyInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Vector;

/**
 * Wrapper around a multiplexed stream to prepend it with a header entry.
 *
 * Initial stream format is "SIZE1(long)DATA1(raw)SIZE2(long)DATA2(raw)..."
 * Target stream format is "SIZE0(long)DATA0(raw)SIZE1(long)DATA1(raw)SIZE2(long)DATA2(raw)..."
 */
public class PrependedMultiplexedInputStream extends ProxyInputStream {

    private static final long LONG_SIZE = 8L;
    private final long size;

    public PrependedMultiplexedInputStream(
        InputStream firstEntryStream,
        long firstEntrySize,
        InputStream multiplexedInputStream,
        long multiplexedInputStreamSize
    ) throws IOException {
        super(buildSequence(firstEntryStream, firstEntrySize, multiplexedInputStream, multiplexedInputStreamSize));
        this.size = LONG_SIZE + firstEntrySize + multiplexedInputStreamSize;
    }

    public long size() {
        return this.size;
    }

    private static InputStream buildSequence(
        InputStream firstEntryStream,
        long firstEntrySize,
        InputStream multiplexedInputStream,
        long multiplexedInputStreamSize
    ) throws IOException {
        ByteArrayOutputStream firstEntrySizeOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream firstEntrySizeDataOutputStream = new DataOutputStream(firstEntrySizeOutputStream)) {
            firstEntrySizeDataOutputStream.writeLong(firstEntrySize);
            firstEntrySizeDataOutputStream.flush();
        }

        InputStream firstEntrySizeInputStream = firstEntrySizeOutputStream.toInputStream();

        // We need Vectors because SequenceInputStream requires an Enumeration<>
        Vector<InputStream> streams = new Vector<>();
        streams.add(firstEntrySizeInputStream);
        streams.add(new ExactSizeInputStream(firstEntryStream, firstEntrySize));
        streams.add(new ExactSizeInputStream(multiplexedInputStream, multiplexedInputStreamSize));

        return new SequenceInputStream(streams.elements());
    }
}
