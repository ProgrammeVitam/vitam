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

import fr.gouv.vitam.common.logging.SysErrLogger;
import org.apache.commons.io.output.ProxyOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ExtendedFileOutputStream extends ProxyOutputStream {

    private final FileChannel fileChannel;
    private final boolean fsyncOnClose;
    private boolean writeMetadata = true;
    private boolean closed = false;

    public ExtendedFileOutputStream(Path filepath, boolean fsyncOnClose) throws IOException {
        super(null);
        this.fileChannel = FileChannel.open(filepath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        this.fsyncOnClose = fsyncOnClose;

        super.out = new BufferedOutputStream(Channels.newOutputStream(fileChannel));
    }

    public void fsync() throws IOException {
        flush();
        fileChannel.force(writeMetadata);
        writeMetadata = false;
    }

    @Override
    public void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;

        if (fsyncOnClose) {
            try {
                fsync();
            } catch (IOException ex) {
                // Try close quietly and rethrow initial exception
                try {
                    super.close();
                } catch (IOException ex2) {
                    SysErrLogger.FAKE_LOGGER.ignoreLog(ex2);
                }
                throw ex;
            }
        }

        super.close();
    }
}
