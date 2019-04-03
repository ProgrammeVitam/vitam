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
package fr.gouv.vitam.storage.offers.tape.cas;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.exception.ObjectReferentialException;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.input.TaggedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public class TarFileRapairer {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TarFileRapairer.class);

    private final Supplier<TarFileDigestVerifier> tarFileDigestVerifierSupplier;

    public TarFileRapairer(ObjectReferentialRepository objectReferentialRepository) {
        this(() -> new TarFileDigestVerifier(objectReferentialRepository, VitamConfiguration.getBatchSize()));
    }

    @VisibleForTesting
    TarFileRapairer(
        Supplier<TarFileDigestVerifier> tarFileDigestVerifierSupplier) {
        this.tarFileDigestVerifierSupplier = tarFileDigestVerifierSupplier;
    }

    public void repairAndVerifyTarArchive(InputStream inputStream, Path newOutputTarFile, String tarId)
        throws IOException, ObjectReferentialException {

        TarFileDigestVerifier tarFileDigestVerifier = tarFileDigestVerifierSupplier.get();

        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
            TaggedInputStream taggedEntryInputStream = new TaggedInputStream(tarArchiveInputStream)) {

            try (TarAppender tarAppender = new TarAppender(newOutputTarFile, tarId, Long.MAX_VALUE)) {

                while (true) {

                    long readBytes = tarArchiveInputStream.getBytesRead();
                    // Last entry position is padded to tar record size
                    long inputTarEntryPos =
                        (readBytes + TarConstants.DEFAULT_RCDSIZE - 1) / TarConstants.DEFAULT_RCDSIZE *
                            TarConstants.DEFAULT_RCDSIZE;

                    TarArchiveEntry inputTarEntry;
                    try {
                        inputTarEntry = tarArchiveInputStream.getNextTarEntry();
                    } catch (IOException ex) {
                        LOGGER.warn("Entry corrupted at " + inputTarEntryPos + " for file " + tarId, ex);
                        break;
                    }

                    if (inputTarEntry == null) {
                        // No more data...
                        break;
                    }

                    Path tempEntryFile = null;
                    try {

                        // Write entry to temporary file (to ensure the entry is available & not corrupted)
                        tempEntryFile =
                            Files.createTempFile(GUIDFactory.newGUID().toString(), LocalFileUtils.TMP_EXTENSION);

                        try {
                            InputStream entryInputStream = new CloseShieldInputStream(taggedEntryInputStream);
                            FileUtils.copyInputStreamToFile(entryInputStream, tempEntryFile.toFile());
                        } catch (IOException ex) {
                            if (taggedEntryInputStream.isCauseOf(ex)) {
                                LOGGER.warn("Entry corrupted at " + inputTarEntryPos + " for file " + tarId, ex);
                                break;
                            }
                        }

                        // Recopy to new tar
                        try (InputStream temptFileInputStream = Files
                            .newInputStream(tempEntryFile, StandardOpenOption.READ)) {

                            Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
                            InputStream entryInputStream = digest.getDigestInputStream(
                                new CloseShieldInputStream(temptFileInputStream));

                            TarEntryDescription tarEntryDescription =
                                tarAppender.append(inputTarEntry.getName(), entryInputStream, inputTarEntry.getSize());

                            if (tarEntryDescription.getStartPos() != inputTarEntryPos) {
                                throw new IllegalStateException(
                                    "Entry position mismatch. Expected=" + inputTarEntryPos
                                        + ", actual=" + tarEntryDescription.getStartPos());
                            }

                            String entryDigest = digest.digestHex();
                            tarFileDigestVerifier.addDigestToCheck(tarEntryDescription.getEntryName(), entryDigest);
                        }

                    } finally {
                        if (tempEntryFile != null) {
                            FileUtils.deleteQuietly(tempEntryFile.toFile());
                        }
                    }
                }
            }
            tarFileDigestVerifier.finalizeChecks();
        }
    }

    public void verifyTarArchive(InputStream inputStream) throws IOException, ObjectReferentialException {

        TarFileDigestVerifier tarFileDigestVerifier = tarFileDigestVerifierSupplier.get();

        try (TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {

            TarArchiveEntry tarEntry;
            while (null != (tarEntry = tarArchiveInputStream.getNextTarEntry())) {

                String tarEntryName = tarEntry.getName();
                Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
                InputStream entryInputStream = new CloseShieldInputStream(tarArchiveInputStream);
                digest.update(entryInputStream);
                String entryDigest = digest.digestHex();

                tarFileDigestVerifier.addDigestToCheck(tarEntryName, entryDigest);
            }
            tarFileDigestVerifier.finalizeChecks();
        }
    }
}
