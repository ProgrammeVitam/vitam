/**
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

package fr.gouv.vitam.common.server2.benchmark;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import fr.gouv.vitam.common.client2.DefaultClient;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.junit.FakeInputStream;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Benchmark client
 */
public class BenchmarkClientRest extends DefaultClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(BenchmarkClientRest.class);
    private static final String UPLOAD_URL = "/upload";
    private static final String MULTIPART_URL = "/multipart";

    BenchmarkClientRest(BenchmarkClientFactory factory) {
        super(factory);
    }

    /**
     * Upload a stream of size bytes
     *
     * @param method between GET, POST, PUT, DELETE
     * @param size
     * @return time in ns
     */
    public long upload(String method, long size) {
        try (FakeInputStream fakeInputStream = new FakeInputStream(size, true)) {

            final Response response =
                performRequest(method, UPLOAD_URL + method,
                    null, fakeInputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE, MediaType.WILDCARD_TYPE);
            try {
                final Status status = Status.fromStatusCode(response.getStatus());
                switch (status) {
                    case OK:
                        final long sizeRead = response.readEntity(long.class);
                        LOGGER.debug(Response.Status.OK.getReasonPhrase() + " : " + sizeRead);
                        return sizeRead;
                    default:
                        LOGGER.error(status.getReasonPhrase());
                        return -1;
                }
            } finally {
                consumeAnyEntityAndClose(response);
            }
        } catch (VitamClientException e) {
            LOGGER.error(e);
            return -1;
        }
    }

    /**
     * Post and Get a stream of size bytes
     *
     * @param filename file name
     * @param size
     * @return time in ns
     */
    public long multipart(String filename, long size) {
        try (FakeInputStream fakeInputStream = new FakeInputStream(size, true)) {
            final StreamDataBodyPart stream = new StreamDataBodyPart("sip", fakeInputStream, filename);
            try (MultiPart multiPart = new MultiPart()) {
                multiPart.bodyPart(new FormDataBodyPart("check", "test"))
                    .bodyPart(stream).type(MediaType.MULTIPART_FORM_DATA_TYPE);
                final Response response =
                    performRequest(HttpMethod.POST, MULTIPART_URL + HttpMethod.POST,
                        null, multiPart, multiPart.getMediaType(), MediaType.MULTIPART_FORM_DATA_TYPE);
                try {
                    final Status status = Status.fromStatusCode(response.getStatus());
                    switch (status) {
                        case OK:
                            long sizeRead = -1;
                            final MultiPart multiPartResponse = response.readEntity(MultiPart.class);
                            final List<BodyPart> list = multiPartResponse.getBodyParts();
                            for (final BodyPart bodyPart : list) {
                                final ContentDisposition cd = bodyPart.getContentDisposition();
                                if (cd.getFileName() != null) {
                                    final InputStream inputStream = bodyPart.getEntityAs(InputStream.class);
                                    sizeRead = JunitHelper.consumeInputStream(inputStream);
                                }
                            }
                            LOGGER.debug(Response.Status.OK.getReasonPhrase() + " : " + sizeRead);
                            return sizeRead;
                        default:
                            LOGGER.error(status.getReasonPhrase());
                            return -1;
                    }
                } finally {
                    try {
                        consumeAnyEntityAndClose(response);
                    } catch (final ProcessingException e) {
                        LOGGER.error(e);
                        return -1;
                    }
                }
            } catch (final IOException | VitamClientException e) {
                LOGGER.error(e);
                return -1;
            }
        }
    }

    /**
     * Make protected method available
     */
    public Response performRequest(String httpMethod, String path, MultivaluedHashMap<String, Object> headers,
        MediaType accept) throws VitamClientInternalException {
        return super.performRequest(httpMethod, path, headers, accept);
    }

    /**
     * Check Status using other method than GET
     *
     * @param method between HEAD and OPTIONS
     * @return the responde from tghe server
     * @throws VitamClientException
     */
    public boolean getStatus(String method) throws VitamClientException {
        Response response = null;
        try {
            response =
                performRequest(method, STATUS_URL + method,
                    null, MediaType.WILDCARD_TYPE, false);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (status == Status.OK || status == Status.NO_CONTENT) {
                return true;
            }
            final String message = INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase();
            LOGGER.error(message);
            throw new VitamClientException(message);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
