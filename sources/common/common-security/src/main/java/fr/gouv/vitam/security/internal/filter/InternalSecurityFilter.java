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
package fr.gouv.vitam.security.internal.filter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.auth.web.filter.CertUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.ContextStatus;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.security.internal.client.InternalSecurityClient;
import fr.gouv.vitam.security.internal.client.InternalSecurityClientFactory;
import fr.gouv.vitam.security.internal.common.exception.InternalSecurityException;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.exception.VitamSecurityException;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

/**
 * This filter is used to get certificate from ServletRequest
 * Then get vitam context attached to this certificate.
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR + 30) // must go after UriConnectionFilter (if present)
public class InternalSecurityFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InternalSecurityFilter.class);

    public static final String ACCESS_EXTERNAL = "/access-external/";
    public static final String INGEST_EXTERNAL = "/ingest-external/";
    private final boolean allowSslClientHeader;

    @Context
    private HttpServletRequest httpServletRequest;

    private InternalSecurityClientFactory internalSecurityClientFactory;

    private AdminManagementClientFactory adminManagementClientFactory;

    public InternalSecurityFilter(boolean allowSslClientHeader) {
        super();
        this.allowSslClientHeader = allowSslClientHeader;
        this.internalSecurityClientFactory = InternalSecurityClientFactory.getInstance();
        this.adminManagementClientFactory = AdminManagementClientFactory.getInstance();
    }

    @VisibleForTesting
    InternalSecurityFilter(
        HttpServletRequest httpServletRequest,
        InternalSecurityClientFactory internalSecurityClientFactory,
        AdminManagementClientFactory adminManagementClientFactory,
        boolean allowSslClientHeader
    ) {
        this.httpServletRequest = httpServletRequest;
        this.internalSecurityClientFactory = internalSecurityClientFactory;
        this.adminManagementClientFactory = adminManagementClientFactory;
        this.allowSslClientHeader = allowSslClientHeader;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        X509Certificate[] clientCertChain = CertUtils.extractCert(httpServletRequest, allowSslClientHeader);
        if (clientCertChain == null || clientCertChain.length < 1) {
            throw new VitamSecurityException("Request do not contain any X509Certificate ");
        }
        if (requestContext.getUriInfo().getPath().endsWith(VitamConfiguration.STATUS_URL)) {
            // There is no need to check security profile to retrieve status
            return;
        }

        int tenantId = Integer.parseInt(httpServletRequest.getHeader(GlobalDataRest.X_TENANT_ID));

        String contractId = httpServletRequest.getHeader(GlobalDataRest.X_ACCESS_CONTRAT_ID);
        if (contractId != null) {
            contractId = contractId.split(",")[0];
        }

        final String accessContract = contractId;

        final X509Certificate cert = clientCertChain[0];

        try (InternalSecurityClient internalSecurityClient = internalSecurityClientFactory.getClient()) {
            Optional<IdentityModel> result = internalSecurityClient.findIdentity(cert.getEncoded());

            IdentityModel identityModel = result.orElseThrow(
                () -> new VitamSecurityException("Certificate revoked or not found in database.")
            );

            final ContextModel contextModel = getContext(identityModel);
            String uri = requestContext.getUriInfo().getPath();

            if (null == uri) {
                uri = "";
            }

            // EnableControl should be enabled in the context
            // We also skip all uri that match tenant url and status url
            if (
                contextModel.isEnablecontrol() != null &&
                contextModel.isEnablecontrol() &&
                !uri.endsWith(VitamConfiguration.STATUS_URL) &&
                !uri.endsWith(VitamConfiguration.TENANTS_URL)
            ) {
                if (uri.contains(ACCESS_EXTERNAL)) {
                    verifyAccessContract(tenantId, accessContract, contextModel);
                } else if (uri.contains(INGEST_EXTERNAL)) {
                    verifyIngestContract(tenantId, contextModel);
                } else {
                    verifyTenant(tenantId, contextModel);
                }
            }
            VitamThreadUtils.getVitamSession().setContextId(contextModel.getIdentifier());
            VitamThreadUtils.getVitamSession()
                .setSecurityProfileIdentifier(contextModel.getSecurityProfileIdentifier());
        } catch (
            VitamClientInternalException
            | InternalSecurityException
            | CertificateEncodingException
            | VitamSecurityException e
        ) {
            LOGGER.error("Security Error :", e);
            final VitamError vitamError = generateVitamError(e);

            requestContext.abortWith(
                Response.status(vitamError.getHttpCode())
                    .entity(vitamError)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build()
            );
        }
    }

    /**
     * Generate Vitam Error
     *
     * @param e
     * @return
     */
    private VitamError generateVitamError(Exception e) {
        final VitamError vitamError = new VitamError(VitamCodeHelper.getCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED));

        String description = e.getMessage();
        if (Strings.isNullOrEmpty(description)) {
            description = StringUtils.getClassName(e);
        }
        vitamError
            .setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getMessage())
            .setDescription(description)
            .setState(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.name())
            .setHttpCode(VitamCode.INTERNAL_SECURITY_UNAUTHORIZED.getStatus().getStatusCode());
        return vitamError;
    }

    /**
     * Verify access contract according to the context
     *
     * @param tenantId
     * @param accessContract
     * @param contextModel
     */
    private void verifyAccessContract(int tenantId, String accessContract, ContextModel contextModel) {
        boolean accessContractVerified = false;

        if (contextModel.getPermissions() != null && !contextModel.getPermissions().isEmpty()) {
            accessContractVerified = contextModel
                .getPermissions()
                .stream()
                .filter(pm -> pm.getTenant() == tenantId)
                .filter(pm -> pm.getAccessContract() != null)
                .anyMatch(pm -> pm.getAccessContract().contains(accessContract));
        }

        if (!accessContractVerified) {
            throw new VitamSecurityException(
                "Access contract " + accessContract + " not found in the context for the tenant id :" + tenantId
            );
        }
    }

    /**
     * Verify ingest contract according to the context
     *
     * @param tenantId
     * @param contextModel
     */
    private void verifyIngestContract(int tenantId, ContextModel contextModel) {
        boolean ingestContractVerified = false;

        if (contextModel.getPermissions() != null && !contextModel.getPermissions().isEmpty()) {
            ingestContractVerified = contextModel
                .getPermissions()
                .stream()
                .filter(pm -> pm.getTenant() == tenantId)
                .filter(pm -> pm.getIngestContract() != null)
                .anyMatch(pm -> !pm.getIngestContract().isEmpty());
        }

        if (!ingestContractVerified) {
            throw new VitamSecurityException("Ingest contract not found in the context for the tenant id :" + tenantId);
        }
    }

    /**
     * Verify tenant according to the context
     *
     * @param tenantId
     */
    private void verifyTenant(int tenantId, ContextModel contextModel) {
        boolean tenantVerified = false;

        if (contextModel.getPermissions() != null && !contextModel.getPermissions().isEmpty()) {
            tenantVerified = contextModel.getPermissions().stream().anyMatch(pm -> pm.getTenant() == tenantId);
        }

        if (!tenantVerified) {
            throw new VitamSecurityException("Tenant " + tenantId + " not found in the context");
        }
    }

    /**
     * Get context model from database and set it to VitamSecurityContext
     *
     * @param identityModel
     */
    private ContextModel getContext(IdentityModel identityModel) {
        final String contextId = identityModel.getContextId();
        try (AdminManagementClient adminManagementClient = adminManagementClientFactory.getClient()) {
            RequestResponse<ContextModel> contextResponse = adminManagementClient.findContextById(contextId);

            if (contextResponse.isOk()) {
                List<ContextModel> results = ((RequestResponseOK<ContextModel>) contextResponse).getResults();
                if (results.isEmpty()) {
                    throw new VitamSecurityException("The context " + contextId + "  not found in database");
                } else {
                    final ContextModel context = Iterables.getFirst(results, null);

                    if (!ContextStatus.ACTIVE.equals(context.getStatus())) {
                        throw new VitamSecurityException("The context " + contextId + "  is not activated");
                    }

                    return context;
                }
            } else {
                throw new VitamSecurityException("The context " + contextId + "  not found in database");
            }
        } catch (
            InvalidParseOperationException | ReferentialNotFoundException | AdminManagementClientServerException e
        ) {
            throw new VitamSecurityException(e);
        }
    }

    public void destroy() {
        // Nothing to do
    }
}
