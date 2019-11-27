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
package fr.gouv.vitam.security.internal.client;

import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class InternalSecurityClientRest extends DefaultClient implements InternalSecurityClient {

    protected static final String INTERNAL_SECURITY_ERROR = "Internal Security Error";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InternalSecurityClientRest.class);

    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    public InternalSecurityClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public Optional<IdentityModel> findIdentity(byte[] certificate)
        throws VitamClientInternalException, InternalSecurityException {
        try (Response response = make(
            VitamRequestBuilder.get().withPath("/identity/").withHeaders(new MultivaluedHashMap<>())
                .withBody(certificate)
                .withContentType(MediaType.APPLICATION_OCTET_STREAM_TYPE).withJsonAccept())) {
            check(response);
            return Optional.of(response.readEntity(IdentityModel.class));
        }
    }

    @Override
    public boolean contextIsUsed(String contextId) throws VitamClientInternalException, InternalSecurityException {
        try (Response response = make(
            VitamRequestBuilder.get().withPath("/identity/context/" + contextId).withJsonAccept())) {
            check(response);
            return response.readEntity(Boolean.class);
        }
    }

    @Override
    public IsPersonalCertificateRequiredModel isPersonalCertificateRequiredByPermission(String permission)
        throws VitamClientInternalException, InternalSecurityException {
        try (Response response = make(
            VitamRequestBuilder.get().withPath("/personalCertificate/permission-check/" + permission)
                .withJsonAccept())) {
            return check(response).readEntity(IsPersonalCertificateRequiredModel.class);
        }
    }

    @Override
    public void checkPersonalCertificate(byte[] certificate, String permission)
        throws VitamClientInternalException, InternalSecurityException {
        try (Response response = make(VitamRequestBuilder.get()
            .withPath("/personalCertificate/personal-certificate-check/" + permission)
            .withHeaders(new MultivaluedHashMap<>()).withBody(certificate)
            .withContentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .withJsonAccept())) {
            check(response);
        }
    }

    private Response check(Response response) throws InternalSecurityException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return response;
        }

        switch (status) {
            case NOT_FOUND:
            case UNAUTHORIZED:
                throw new InternalSecurityException(INTERNAL_SECURITY_ERROR);
            default:
                throw new InternalSecurityException(INTERNAL_SECURITY_ERROR);
        }
    }
}
