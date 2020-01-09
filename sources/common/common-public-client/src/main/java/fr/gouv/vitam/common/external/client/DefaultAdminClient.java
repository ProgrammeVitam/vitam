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
package fr.gouv.vitam.common.external.client;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;

/**
 * Abstract Partial client class for all vitam clients
 */
public class DefaultAdminClient extends AbstractCommonClient implements AdminClient {
    private static final String THE_REQUESTED_SERVICE_IS_UNAVAILABLE = "The requested service is unavailable";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultAdminClient.class);
    private final String adminUrl;

    /**
     * Constructor with standard configuration
     *
     * @param factory The client factory
     */
    public DefaultAdminClient(VitamClientFactoryInterface<DefaultAdminClient> factory) {
        super(factory);
        adminUrl = (factory.getClientConfiguration().isSecure() ? "https://"
            : "http://") + factory.getClientConfiguration().getServerHost() + ":" +
            factory.getClientConfiguration().getServerPort() +
            VitamConfiguration.ADMIN_PATH;
    }

    @Override
    public AdminStatusMessage adminStatus() throws VitamClientException {
        Response response = null;
        try {
            AdminStatusMessage message = null;
            final Builder builder =
                buildRequest(HttpMethod.GET, adminUrl, STATUS_URL, null, MediaType.APPLICATION_JSON_TYPE, true);
            response = builder.method(HttpMethod.GET);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (response.hasEntity()) {
                message = response.readEntity(AdminStatusMessage.class);
            } else {
                message = new AdminStatusMessage().setStatus(status == Status.OK);
            }
            if (status == Status.OK || status == Status.SERVICE_UNAVAILABLE) {
                return message;
            }
            final String messageText = INTERNAL_SERVER_ERROR.getReasonPhrase() + " : " + status.getReasonPhrase();
            LOGGER.error(messageText);
            throw new VitamClientException(messageText);
        } catch (final ProcessingException e) {
            LOGGER.debug(e);
            return new AdminStatusMessage().setStatus(false);
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public VitamError adminAutotest() throws VitamClientException {
        Response response = null;
        final String name = StringUtils.getClassName(clientFactory);
        VitamError message = new VitamError("000000")
            .setContext(name).setDescription(THE_REQUESTED_SERVICE_IS_UNAVAILABLE)
            .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode()).setMessage(THE_REQUESTED_SERVICE_IS_UNAVAILABLE)
            .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());
        try {
            final Builder builder =
                buildRequest(HttpMethod.GET, adminUrl, VitamConfiguration.AUTOTEST_URL, null,
                    MediaType.APPLICATION_JSON_TYPE, true);
            response = builder.method(HttpMethod.GET);
            final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
            if (response.hasEntity()) {
                message = response.readEntity(VitamError.class);
            }
            if (status == Status.OK || status == Status.SERVICE_UNAVAILABLE) {
                return message;
            }
            final String messageText = INTERNAL_SERVER_ERROR + " : " + status.getReasonPhrase();
            LOGGER.error(messageText);
            throw new VitamClientException(messageText);
        } catch (final ProcessingException e) {
            LOGGER.error(e);
            return message;
        } finally {
            consumeAnyEntityAndClose(response);
        }
    }

    @Override
    public String getAdminUrl() {
        return adminUrl;
    }
}
