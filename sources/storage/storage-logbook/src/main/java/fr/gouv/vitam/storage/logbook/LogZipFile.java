/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.logbook;

import com.google.common.io.ByteStreams;
import fr.gouv.vitam.common.stream.StreamUtils;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * class {@link LogZipFile}
 */
public class LogZipFile implements AutoCloseable {

    private static final String LOGBOOK_OPERATIONS_FILENAME = "logFile.log";
    private static final String ADDITIONAL_INFORMATION_FILENAME = "information.txt";

    private static final String LINE_SEPARATOR = "\n";

    private final ZipArchiveOutputStream archive;

    /**
     * @param file to archive
     * @throws ArchiveException      if error on creating TraceabilityFile
     * @throws FileNotFoundException if find not found
     */
    public LogZipFile(File file) throws ArchiveException, FileNotFoundException {
        final OutputStream archiveStream = new BufferedOutputStream(new FileOutputStream(file));
        archive = (ZipArchiveOutputStream) new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);

        archive.setLevel(0);
    }


    /**
     * @throws IOException if error when create entry file
     */
    public void initStoreLog() throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(LOGBOOK_OPERATIONS_FILENAME);
        archive.putArchiveEntry(entry);
    }

    /**
     * @throws IOException if write log error
     */
    public void storeLogFile(InputStream stream) throws IOException {
        ByteStreams.copy(stream, archive);
        stream.close();
        archive.closeArchiveEntry();
    }



    /**
     * @param startDate       information to store
     * @param endDate         information to store
     * @param hash
     * @param zipCreationDate
     * @param tenantId
     * @throws IOException if error on writing/closing stream
     */
    public void storeAdditionalInformation(String startDate, String endDate, String hash, String zipCreationDate,
        Integer tenantId) throws IOException {
        ZipArchiveEntry entry;
        entry = new ZipArchiveEntry(ADDITIONAL_INFORMATION_FILENAME);
        archive.putArchiveEntry(entry);
        archive.write(String.format("Log File startDate=%s", startDate).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("Log File endDate=%s", endDate).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("hash log File=%s", hash).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("Zip creation Date=%s", zipCreationDate).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("TenantId=%s", tenantId).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.closeArchiveEntry();
    }

    /**
     * flush the stream and close it.
     *
     * @throws IOException if error on closing stream
     */
    @Override
    public void close() throws IOException {
        archive.flush();
        archive.close();
    }

}
