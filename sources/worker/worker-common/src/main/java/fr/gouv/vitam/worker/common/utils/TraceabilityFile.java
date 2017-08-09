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
package fr.gouv.vitam.worker.common.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * This class is used for Lifecycle Securization.
 * It will create a ZipArchiveOutputStream including 4 files. 
 * Methods are used here in order to create and fill the 4 files.
 * A file generated from this object will be stored in the storage 
 * 
 * class {@link TraceabilityFile}
 */
public class TraceabilityFile implements AutoCloseable {

    private static final String LOGBOOK_LIFECYCLES_FILENAME = "global_lifecycles.txt";
    private static final String ADDITIONAL_INFORMATION_FILENAME = "additional_information.txt";
    private static final String COMPUTING_INFORMATION_FILENAME = "computing_information.txt";
    private static final String TIMESTAMP_FILENAME = "token.tsp";

    private static final String LINE_SEPARATOR = "\n";

    private final ZipArchiveOutputStream archive;

    /**
     *
     * @param file to archive
     * @throws ArchiveException if error on creating TraceabilityFile 
     * @throws FileNotFoundException if find not found
     */
    public TraceabilityFile(File file) throws ArchiveException, FileNotFoundException {
        final OutputStream archiveStream = new BufferedOutputStream(new FileOutputStream(file));
        archive = (ZipArchiveOutputStream) new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);

        archive.setLevel(0);
    }

    /**
     * store timestamp token in a specific file
     *
     * @param timestampToken to store
     * @throws IOException if error on writing file 
     */
    public void storeTimeStampToken(byte[] timestampToken) throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(TIMESTAMP_FILENAME);
        archive.putArchiveEntry(entry);

        final byte[] encodeBase64 = Base64.encodeBase64(timestampToken);
        archive.write(encodeBase64);
        archive.closeArchiveEntry();
    }
    

    /**
     * init the lifecycles file
     *
     * @throws IOException if error when create entry file
     */
    public void initStoreLifecycleLog() throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(LOGBOOK_LIFECYCLES_FILENAME);
        archive.putArchiveEntry(entry);
    }

    /**
     * Store a lifecycle in the file
     * 
     * @param logbookLifecycle 
     * @throws IOException if write log error
     */
    public void storeLifecycleLog(InputStream logbookLifecycle) throws IOException {
        archive.write(IOUtils.toByteArray(logbookLifecycle));
        archive.write(LINE_SEPARATOR.getBytes());
    }

    /**
     * @throws IOException if error on closing stream
     */
    public void closeStoreLifecycleLog() throws IOException {
        archive.closeArchiveEntry();
    }

    /**
     * store information use to compute timestamp token
     *
     * @param currentHash of timestamp token
     * @param previousHash timestamp token
     * @param currentHashMinusOneMonth timestamp token
     * @param currentHashMinusOneYear timestamp token
     * @throws IOException if error on writing/closing stream
     */
    public void storeHashCalculationInformation(String currentHash, String previousHash,
        String currentHashMinusOneMonth, String currentHashMinusOneYear) throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(COMPUTING_INFORMATION_FILENAME);
        archive.putArchiveEntry(entry);
        archive.write(String.format("currentHash=%s", currentHash).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("previousTimestampToken=%s", previousHash).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("previousTimestampTokenMinusOneMonth=%s", currentHashMinusOneMonth).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("previousTimestampTokenMinusOneYear=%s", currentHashMinusOneYear).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.closeArchiveEntry();
    }

    /**
     * @param numberOfLine information to store
     * @param startDate information to store
     * @param endDate information to store
     * @throws IOException if error on writing/closing stream
     */
    public void storeAdditionalInformation(long numberOfLine, String startDate, String endDate) throws IOException {
        ZipArchiveEntry entry;
        entry = new ZipArchiveEntry(ADDITIONAL_INFORMATION_FILENAME);
        archive.putArchiveEntry(entry);
        archive.write(String.format("numberOfElement=%d", numberOfLine).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("startDate=%s", startDate).getBytes());
        archive.write(LINE_SEPARATOR.getBytes());
        archive.write(String.format("endDate=%s", endDate).getBytes());
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
