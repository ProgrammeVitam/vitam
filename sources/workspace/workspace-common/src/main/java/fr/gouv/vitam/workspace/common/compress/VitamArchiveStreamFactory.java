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
package fr.gouv.vitam.workspace.common.compress;

import fr.gouv.vitam.common.CommonMediaType;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

/**
 * Factory to create ArchiveInputStreams from names
 */
public class VitamArchiveStreamFactory {

    /**
     * Create an archive input stream from an archiver name and an input stream.
     *
     * @param mediaType MediaType object {@link MediaType} the archive name, i.e. ZIP, TAR, or
     * GZIP
     * @param in the input stream
     * @return the archive input stream
     * @throws ArchiveException if the archiver name is not known
     * @throws IOException if cannot create gzip/bzip input stream
     * @throws IllegalArgumentException if the archiver name or stream is null
     */
    public ArchiveInputStream createArchiveInputStream(final MediaType mediaType, final InputStream in)
        throws ArchiveException, IOException {
        if (mediaType == null) {
            throw new IllegalArgumentException("archiverMediaType must not be null.");
        }

        if (in == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }

        switch (CommonMediaType.mimeTypeOf(mediaType)) {
            case CommonMediaType.ZIP:
                return new ZipArchiveInputStream(in);
            case CommonMediaType.TAR:
                return new TarArchiveInputStream(in);
            case CommonMediaType.XGZIP:
            case CommonMediaType.GZIP:
                return new TarArchiveInputStream(new GzipCompressorInputStream(in));
            case CommonMediaType.BZIP2:
                return new TarArchiveInputStream(new BZip2CompressorInputStream(in));
            default:
                throw new ArchiveException("Archiver: " + mediaType + " not found.");
        }
    }
}
