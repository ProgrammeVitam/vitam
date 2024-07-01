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
package fr.gouv.vitam.logbook.common.model;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.security.merkletree.MerkleTree;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Used to handle zip file for traceability
 */
public class TraceabilityFile implements AutoCloseable {

    public static final String previousTimestampToken = "previousTimestampToken";
    public static final String previousTimestampTokenMinusOneMonth = "previousTimestampTokenMinusOneMonth";
    public static final String previousTimestampTokenMinusOneYear = "previousTimestampTokenMinusOneYear";
    public static final String currentHash = "currentHash";

    private static final String EXTRACT_FILENAME = "data.txt";
    private static final String ADDITIONAL_INFORMATION_FILENAME = "additional_information.txt";
    private static final String COMPUTING_INFORMATION_FILENAME = "computing_information.txt";
    private static final String MEKLE_TREE_FILENAME = "merkleTree.json";
    private static final String TIMESTAMP_FILENAME = "token.tsp";
    static final String SECURISATION_VERSION_LABEL = "securisationVersion";
    private static final String SECURISATION_VERSION = "V1";

    private static final byte[] LINE_SEPARATOR = "\n".getBytes();

    private final ZipArchiveOutputStream archive;
    private final String extractedDataFileName;

    // FIXME: Do ArchiveException should be thrown as it is a local exception that not depend on any user data
    // FIXME ArchiveException thrown if ArchiveStreamFactory.ZIP is not found (Throw TraceabilityException instead ?)

    /**
     * @param file the targeted file to store tmp zip file
     * @throws FileNotFoundException if the given file is missing
     * @throws ArchiveException if any error occurs while creating ZipArchiveOutputStram
     */
    public TraceabilityFile(File file) throws FileNotFoundException, ArchiveException {
        final OutputStream archiveStream = new BufferedOutputStream(new FileOutputStream(file));
        archive = (ZipArchiveOutputStream) new ArchiveStreamFactory()
            .createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);

        archive.setLevel(0);
        this.extractedDataFileName = EXTRACT_FILENAME;
    }

    /**
     * Add a merkleTree file with the computed merkleTree in the zipFile
     *
     * @param merkleTree the tree that should be store in zip
     * @throws IOException if any error occurs while attempting to write in zip
     */
    public void storeMerkleTree(MerkleTree merkleTree) throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(MEKLE_TREE_FILENAME);
        archive.putArchiveEntry(entry);
        archive.write(JsonHandler.unprettyPrint(merkleTree).getBytes(StandardCharsets.UTF_8));
        archive.closeArchiveEntry();
    }

    /**
     * Add a timestampToken file with the computed token of the traceability operation in the zipFile
     *
     * @param timestampToken of the traceability operation
     * @throws IOException if any error occurs while attempting to write in zip
     */
    public void storeTimeStampToken(byte[] timestampToken) throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(TIMESTAMP_FILENAME);
        archive.putArchiveEntry(entry);

        final byte[] encodeBase64 = Base64.encodeBase64(timestampToken);
        archive.write(encodeBase64);
        archive.closeArchiveEntry();
    }

    /**
     * Create a new file for extracted data.<br>
     * Should be directly followed by one or multiple storeLog<br>
     * Must be close with closeStoreLog
     *
     * @throws IOException if any error occurs while attempting to write in zip
     */
    public void initStoreLog() throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(extractedDataFileName);
        archive.putArchiveEntry(entry);
    }

    /**
     * Add a line of extracted data in the recently created file.<br>
     * Must be directly preceded by a call of initStoreLog or storeLog
     *
     * @throws IOException if any error occurs while attempting to write in zip
     */
    public void storeLog(byte[] line) throws IOException {
        archive.write(line);
        archive.write(LINE_SEPARATOR);
    }

    /**
     * Close the extracted data file
     *
     * @throws IOException if error on closing stream
     */
    public void closeStoreLog() throws IOException {
        archive.closeArchiveEntry();
    }

    /**
     * Add an additionalInformation file with the given data in the zipFile
     *
     * @param numberOfLine of the extracted data secured by the traceability process
     * @param startDate of the traceability process
     * @param endDate of the traceability process
     * @throws IOException if any error occurs while attempting to write in zip
     */
    public void storeAdditionalInformation(long numberOfLine, String startDate, String endDate) throws IOException {
        ZipArchiveEntry entry;
        entry = new ZipArchiveEntry(ADDITIONAL_INFORMATION_FILENAME);
        archive.putArchiveEntry(entry);
        archive.write(String.format("numberOfElements=%d", numberOfLine).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.write(String.format("startDate=%s", startDate).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.write(String.format("endDate=%s", endDate).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.write((SECURISATION_VERSION_LABEL + "=" + SECURISATION_VERSION).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.closeArchiveEntry();
    }

    /**
     * Add an computedInformation file with the traceability data in the zipFile
     *
     * @param currentHash hash of the new merkleTree root
     * @param previousHash hash of the last traceability operation merkleTree root
     * @param currentHashMinusOneMonth hash of the (Month - 1) traceability operation merkleTree root
     * @param currentHashMinusOneYear hash of the (Year - 1) traceability operation merkleTree root
     * @throws IOException if any error occurs while attempting to write in zip
     */
    public void storeComputedInformation(
        String currentHash,
        String previousHash,
        String currentHashMinusOneMonth,
        String currentHashMinusOneYear
    ) throws IOException {
        final ZipArchiveEntry entry = new ZipArchiveEntry(COMPUTING_INFORMATION_FILENAME);
        archive.putArchiveEntry(entry);
        archive.write(String.format("currentHash=%s", currentHash).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.write(String.format(previousTimestampToken + "=%s", previousHash).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.write(String.format("previousTimestampTokenMinusOneMonth=%s", currentHashMinusOneMonth).getBytes());
        archive.write(LINE_SEPARATOR);
        archive.write(String.format("previousTimestampTokenMinusOneYear=%s", currentHashMinusOneYear).getBytes());
        archive.write(LINE_SEPARATOR);
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
