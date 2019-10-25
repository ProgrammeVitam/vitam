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
package fr.gouv.vitam.common.stream;

import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class VitamAsyncInputStream extends InputStream {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamAsyncInputStreamResponse.class);

    private final Response response;
    private InputStream inputStream;

    /**
     * Constructor using one response containing itself a stream
     *
     * @param response the original received response to forward
     */
    public VitamAsyncInputStream(Response response) {
        this.response = response;
        try {

            Object entity = response.getEntity();
            if (entity instanceof InputStream) {
                this.inputStream = (InputStream) entity;
            } else {
                this.inputStream = response.readEntity(InputStream.class);
            }
        } catch (IllegalStateException e) {
            // Not an InputStream
            Object object = response.getEntity();
            if (object == null) {
                this.inputStream = new ByteArrayInputStream(new byte[0]);
            } else {
                try {
                    this.inputStream = JsonHandler.writeToInpustream(response.getEntity());
                } catch (InvalidParseOperationException e1) {
                    LOGGER.error(e.getMessage(), e1);
                    throw e;
                }
            }
        }
    }

    @Override
    public int available() throws IOException {
        return inputStream.available();
    }

    @Override
    public void close() {
        DefaultClient.staticConsumeAnyEntityAndClose(response);
        StreamUtils.closeSilently(inputStream);
    }

    @Override
    public synchronized void mark(int readlimit) {
        inputStream.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return inputStream.markSupported();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return inputStream.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return inputStream.read(b);
    }

    @Override
    public synchronized void reset() throws IOException {
        inputStream.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return inputStream.skip(n);
    }

    @Override
    public int read() throws IOException {
        return inputStream.read();
    }
}
