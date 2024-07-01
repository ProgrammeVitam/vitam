/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.external.client;

import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.AdminStatusMessage;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static fr.gouv.vitam.common.VitamConfiguration.ADMIN_PATH;

public class DefaultAdminClient extends AbstractCommonClient implements AdminClient {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DefaultAdminClient.class);
    private final String adminUrl;

    public DefaultAdminClient(VitamClientFactoryInterface<DefaultAdminClient> factory) {
        super(factory);
        adminUrl = getUrl(factory);
    }

    @Override
    public AdminStatusMessage adminStatus() throws VitamClientException {
        VitamRequestBuilder request = VitamRequestBuilder.get()
            .withBaseUrl(adminUrl)
            .withPath(STATUS_URL)
            .withJsonAccept()
            .withChunckedMode(true);
        try (Response response = makeSpecifyingUrl(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
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
    public VitamError adminAutotest() throws VitamClientException {
        VitamError message = new VitamError("000000")
            .setContext(StringUtils.getClassName(this.getClientFactory()))
            .setDescription(Status.SERVICE_UNAVAILABLE.getReasonPhrase())
            .setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode())
            .setMessage(Status.SERVICE_UNAVAILABLE.getReasonPhrase())
            .setState(Status.SERVICE_UNAVAILABLE.getReasonPhrase());

        VitamRequestBuilder request = VitamRequestBuilder.get()
            .withBaseUrl(adminUrl)
            .withPath(VitamConfiguration.AUTOTEST_URL)
            .withJsonAccept()
            .withChunckedMode(true);

        try (Response response = makeSpecifyingUrl(request)) {
            Response.Status status = response.getStatusInfo().toEnum();
            if (response.hasEntity()) {
                message = response.readEntity(VitamError.class);
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
