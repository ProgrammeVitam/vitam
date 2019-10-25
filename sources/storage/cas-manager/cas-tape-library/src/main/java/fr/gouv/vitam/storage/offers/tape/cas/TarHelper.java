/*
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
 */
package fr.gouv.vitam.storage.offers.tape.cas;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.stream.ExactDigestValidatorInputStream;
import fr.gouv.vitam.common.stream.ExactSizeInputStream;
import fr.gouv.vitam.storage.engine.common.model.TarEntryDescription;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class TarHelper {

    public static InputStream readEntryAtPos(Path tarFilePath, TarEntryDescription entryDescription)
        throws IOException {

        SeekableByteChannel seekableByteChannel = null;
        InputStream inputStream = null;

        try {
            seekableByteChannel = Files.newByteChannel(tarFilePath, StandardOpenOption.READ);
            seekableByteChannel.position(entryDescription.getStartPos());

            inputStream = Channels.newInputStream(seekableByteChannel);
            TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream);

            ArchiveEntry tarEntry = tarArchiveInputStream.getNextEntry();
            if (!tarEntry.getName().equals(entryDescription.getEntryName())) {
                throw new IOException(
                    "Tar entry name conflict. Expected '" + entryDescription.getEntryName() + "', found '" +
                        tarEntry.getName() + "'");
            }
            if (tarEntry.getSize() != entryDescription.getSize()) {
                throw new IOException(
                    "Tar entry size conflict. Expected '" + entryDescription.getSize() + "', found '" +
                        tarEntry.getSize() + "'");
            }

            return new ExactDigestValidatorInputStream(
                new ExactSizeInputStream(tarArchiveInputStream, entryDescription.getSize()),
                VitamConfiguration.getDefaultDigestType(), entryDescription.getDigestValue());

        } catch (IOException | RuntimeException e) {
            // Close streams if and only if exception occurred
            IOUtils.closeQuietly(seekableByteChannel);
            IOUtils.closeQuietly(inputStream);
            // Rethrow original exception
            throw e;
        }
    }
}
