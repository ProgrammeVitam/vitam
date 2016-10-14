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
package fr.gouv.vitam.ingest.external.client;

import java.io.InputStream;
import java.net.SocketException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamRestClientBuilder;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.SSLConfiguration;
import fr.gouv.vitam.ingest.external.api.IngestExternalException;
import fr.gouv.vitam.ingest.external.common.client.ErrorMessage;

/**
 * Ingest External client
 */
public class IngestExternalClientRest implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientRest.class);
    private static final String RESOURCE_PATH = "/ingest-ext/v1";
    private static final String UPLOAD_URL = "/upload";
    private static final String STATUS = "/status";

    private String serviceUrl = "http";
    private final Client client;


    /**
     * Constructor IngestExternalClientRest
     *
     * @param server
     * @param port
     * @throws VitamException
     */
    IngestExternalClientRest(String server, int port) throws VitamException {
        this(server, port, true, new SSLConfiguration(), true);

    }

    /**
     * Constructor IngestExternalClientRest
     *
     * @param server
     * @param port
     * @throws VitamException
     */
    IngestExternalClientRest(String server, int port, boolean secure, SSLConfiguration sslConfiguration,
        boolean hostnameVerification) throws VitamException {
        ParametersChecker.checkParameter("server and port are a mandatory parameter", server, port, secure,
            sslConfiguration, hostnameVerification);
        if (secure) {
            serviceUrl = "https";
        }
        serviceUrl += "://" + server + ":" + port + RESOURCE_PATH;

        final VitamRestClientBuilder restClientBuilder = new VitamRestClientBuilder();
        client = restClientBuilder
            .setSslConfiguration(sslConfiguration)
            .setHostnameVerification(hostnameVerification)
            .build();


    }

    @Override
    public Response upload(InputStream stream) throws IngestExternalException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", stream);
        Response response = null;
        try {
            response = client.target(serviceUrl).path(UPLOAD_URL)
                .request(MediaType.APPLICATION_OCTET_STREAM)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.entity(stream, MediaType.APPLICATION_OCTET_STREAM), Response.class);
            final Status status = Status.fromStatusCode(response.getStatus());
            switch (status) {
                case OK:
                    LOGGER.debug(Response.Status.CREATED.getReasonPhrase());
                    break;
                case INTERNAL_SERVER_ERROR:
                    LOGGER.error(ErrorMessage.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage());
                    throw new IngestExternalException("Upload file error");
                default:
                    throw new IngestExternalException("Unknown error");
            }
        } catch (final ProcessingException e) {
            if (e.getCause().getClass().equals(SocketException.class)) {
                throw new IngestExternalException("Exception can linked to SSL", e);
            } else {
                throw new IngestExternalException(e.getMessage(), e);
            }
        }
        return response;
    }

    @Override
    public Status status() throws IngestExternalException {
        try {
            final Response response = client.target(serviceUrl).path(STATUS).request().get();
            return Status.fromStatusCode(response.getStatus());

        } catch (final ProcessingException e) {
            if (e.getCause().getClass().equals(SocketException.class)) {
                throw new IngestExternalException("Exception can linked to SSL", e);
            } else {
                throw new IngestExternalException(e.getMessage(), e);
            }
        }
    }
}
