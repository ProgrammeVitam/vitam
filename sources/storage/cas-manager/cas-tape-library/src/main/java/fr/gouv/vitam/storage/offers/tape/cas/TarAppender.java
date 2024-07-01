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
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.ExtendedFileOutputStream;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public class TarAppender implements AutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TarAppender.class);

    private final String tarId;
    private final long maxTarSize;

    private final OutputStream outputStream;
    private final TarArchiveOutputStream tarArchiveOutputStream;
    private final Digest digest;
    private int entryCount;
    private long bytesWritten = 0L;

    public TarAppender(Path outputTarFilePath, String tarId, long maxTarSize) throws IOException {
        this(new ExtendedFileOutputStream(outputTarFilePath, true), tarId, maxTarSize);
    }

    public TarAppender(OutputStream outputStream, String tarId, long maxTarSize) {
        this.outputStream = outputStream;
        this.tarId = tarId;
        this.maxTarSize = maxTarSize;
        digest = new Digest(VitamConfiguration.getDefaultDigestType());
        OutputStream digestOutputStream = digest.getDigestOutputStream(outputStream);
        tarArchiveOutputStream = new TarArchiveOutputStream(digestOutputStream);
        tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    }

    public boolean canAppend(long size) {
        if (size > TarConstants.MAXSIZE) {
            throw new IllegalStateException("Invalid entry size. MAX=" + TarConstants.MAXSIZE);
        }

        long currentPos = tarArchiveOutputStream.getBytesWritten();
        long contentWithPaddingSize = (size + TarConstants.DEFAULT_RCDSIZE - 1L) + TarConstants.DEFAULT_RCDSIZE;

        // Header (1 record) + content size (padded to record size) + footer (2 empty records)
        return (
            currentPos + TarConstants.DEFAULT_RCDSIZE + contentWithPaddingSize + 2 * TarConstants.DEFAULT_RCDSIZE <
            maxTarSize
        );
    }

    public TarEntryDescription append(String entryName, InputStream inputStream, long size) throws IOException {
        if (!canAppend(size)) {
            throw new IllegalStateException("Could not append to tar file");
        }

        try {
            long startPos = tarArchiveOutputStream.getBytesWritten();

            TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
            tarEntry.setSize(size);
            tarArchiveOutputStream.putArchiveEntry(tarEntry);

            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
            InputStream digestInputStream = digest.getDigestInputStream(inputStream);
            IOUtils.copy(digestInputStream, tarArchiveOutputStream);

            tarArchiveOutputStream.closeArchiveEntry();
            long endPos = tarArchiveOutputStream.getBytesWritten();

            String entryDigestValue = digest.digestHex();

            LOGGER.info(
                "Written {} [{} bytes] into tar file {} [{}-{}] with digest {}",
                entryName,
                size,
                tarId,
                startPos,
                endPos,
                entryDigestValue
            );
            entryCount++;
            bytesWritten = tarArchiveOutputStream.getBytesWritten();

            return new TarEntryDescription(this.tarId, entryName, startPos, size, entryDigestValue);
        } catch (IOException ex) {
            IOUtils.closeQuietly(outputStream);
            throw ex;
        }
    }

    public void flush() throws IOException {
        tarArchiveOutputStream.flush();
        outputStream.flush();
    }

    public void close() throws IOException {
        try {
            tarArchiveOutputStream.flush();
            outputStream.flush();
            tarArchiveOutputStream.close();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
        this.bytesWritten = tarArchiveOutputStream.getBytesWritten();
    }

    public String getTarId() {
        return tarId;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public String getDigestValue() {
        return this.digest.digestHex();
    }
}
