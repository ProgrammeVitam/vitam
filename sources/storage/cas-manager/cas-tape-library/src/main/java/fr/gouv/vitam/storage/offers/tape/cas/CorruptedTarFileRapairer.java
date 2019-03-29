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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import fr.gouv.vitam.storage.offers.tape.utils.LocalFileUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.TaggedInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class CorruptedTarFileRapairer {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CorruptedTarFileRapairer.class);

    public List<TarEntryDescription> recopy(Path corruptedInputTarFile, Path newOutputTarFile, String tarId)
        throws IOException {

        try (InputStream inputStream = Files.newInputStream(corruptedInputTarFile, StandardOpenOption.READ);
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);
            TaggedInputStream taggedEntryInputStream = new TaggedInputStream(tarArchiveInputStream)) {

            List<TarEntryDescription> tarEntryDescriptions = new ArrayList<>();

            try (TarAppender tarAppender = new TarAppender(newOutputTarFile, tarId, Long.MAX_VALUE)) {

                while (true) {

                    long inputTarEntryPos = tarArchiveInputStream.getBytesRead();

                    TarArchiveEntry inputTarEntry;
                    try {
                        inputTarEntry = tarArchiveInputStream.getNextTarEntry();
                    } catch (IOException ex) {
                        LOGGER.warn("Entry corrupted at " + inputTarEntryPos + " for file "
                            + corruptedInputTarFile, ex);
                        break;
                    }

                    if (inputTarEntry == null) {
                        // No more data...
                        break;
                    }

                    Path tempEntryFile = null;
                    try {

                        // Write entry to temporary file (to ensure the entry is available & not corrupted)
                        tempEntryFile = Files.createTempFile(inputTarEntry.getName(), LocalFileUtils.TMP_EXTENSION);

                        try {
                            FileUtils.copyInputStreamToFile(taggedEntryInputStream, tempEntryFile.toFile());
                        } catch (IOException ex) {
                            if (taggedEntryInputStream.isCauseOf(ex)) {
                                LOGGER.warn("Entry corrupted at " + inputTarEntryPos + " for file "
                                    + corruptedInputTarFile, ex);
                                break;
                            }
                        }

                        // Recopy to new tar
                        try (InputStream entryInputStream = Files
                            .newInputStream(tempEntryFile, StandardOpenOption.READ)) {
                            TarEntryDescription tarEntryDescription =
                                tarAppender.append(inputTarEntry.getName(), entryInputStream, inputTarEntry.getSize());

                            if (tarEntryDescription.getStartPos() != inputTarEntryPos) {
                                throw new IllegalStateException(
                                    "Entry position mismatch. Expected=" + inputTarEntryPos
                                        + ", actual=" + tarEntryDescription.getStartPos());
                            }

                            tarEntryDescriptions.add(tarEntryDescription);
                        }

                    } finally {
                        if (tempEntryFile != null) {
                            FileUtils.deleteQuietly(tempEntryFile.toFile());
                        }
                    }
                }
            }

            return tarEntryDescriptions;
        }
    }
}
