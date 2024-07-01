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
package fr.gouv.vitam.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;
import fr.gouv.vitam.common.server.application.resources.AdminStatusResource;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static fr.gouv.vitam.common.VitamConfiguration.ADMIN_PATH;

public class DefaultAdminClient extends AbstractCommonClient implements AdminClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultAdminClient.class);
    private final String adminUrl;

    public DefaultAdminClient(VitamClientFactoryInterface<DefaultAdminClient> factory) {
        super(factory);
        this.adminUrl = getUrl(factory);
    }

    @Override
    public AdminStatusMessage adminStatus() {
        VitamRequestBuilder request = VitamRequestBuilder.get()
            .withBaseUrl(adminUrl)
            .withPath(STATUS_URL)
            .withJsonAccept()
            .withChunckedMode(true);
        try (Response response = makeSpecifyingUrl(request)) {
            Status status = response.getStatusInfo().toEnum();
            AdminStatusMessage message;
            if (response.hasEntity()) {
                message = response.readEntity(AdminStatusMessage.class);
            } else {
                message = new AdminStatusMessage().setStatus(status == Status.OK);
            }
            if (status == Status.OK || status == Status.SERVICE_UNAVAILABLE) {
                return message;
            }
            String messageText = status.getReasonPhrase() + " : " + status.getStatusCode();
            LOGGER.error(messageText);
            throw new VitamClientException(messageText);
        } catch (Exception e) {
            LOGGER.error(e);
            return new AdminStatusMessage().setStatus(false);
        }
    }

    @Override
    public VitamError<JsonNode> adminAutotest() {
        VitamError<JsonNode> message = new VitamError<JsonNode>("000000")
            .setContext(StringUtils.getClassName(this.getClientFactory()))
            .setDescription(Status.SERVICE_UNAVAILABLE.getReasonPhrase())
            .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
            .setMessage(Status.SERVICE_UNAVAILABLE.getReasonPhrase())
            .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());

        VitamRequestBuilder request = VitamRequestBuilder.get()
            .withBaseUrl(adminUrl)
            .withPath(AdminStatusResource.AUTOTEST_URL)
            .withJsonAccept()
            .withChunckedMode(true);

        try (Response response = makeSpecifyingUrl(request)) {
            Status status = response.getStatusInfo().toEnum();
            if (response.hasEntity()) {
                message = VitamError.getFromJsonNode(JsonHandler.getFromString(response.readEntity(String.class)));
            }
            if (status == Status.OK || status == Status.SERVICE_UNAVAILABLE) {
                return message;
            }
            String messageText = status.getReasonPhrase() + " : " + status.getStatusCode();
            LOGGER.error(messageText);
            throw new VitamClientException(messageText);
        } catch (Exception e) {
            LOGGER.error(e);
            return message;
        }
    }

    @Override
    public String getAdminUrl() {
        return adminUrl;
    }

    private String getUrl(VitamClientFactoryInterface<DefaultAdminClient> factory) {
        String protocol = factory.getClientConfiguration().isSecure() ? "https" : "http";
        String host = factory.getClientConfiguration().getServerHost();
        int port = factory.getClientConfiguration().getServerPort();

        return protocol + "://" + host + ":" + port + ADMIN_PATH;
    }
}
