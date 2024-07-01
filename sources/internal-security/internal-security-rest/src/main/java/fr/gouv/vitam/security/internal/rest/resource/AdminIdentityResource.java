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
package fr.gouv.vitam.security.internal.rest.resource;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.security.internal.common.model.IdentityInsertModel;
import fr.gouv.vitam.security.internal.common.model.IdentityModel;
import fr.gouv.vitam.security.internal.rest.service.IdentityService;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.security.cert.CertificateException;
import java.util.Optional;

@Path("/v1/api/identity")
@Tag(name = "Security")
public class AdminIdentityResource {

    private final IdentityService identityService;

    /**
     * @param identityService
     */
    public AdminIdentityResource(IdentityService identityService) {
        this.identityService = identityService;
    }

    /**
     * @param identityInsertModel
     * @param uri
     * @return
     * @throws InvalidParseOperationException
     * @throws CertificateException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createIdentity(IdentityInsertModel identityInsertModel, @Context UriInfo uri)
        throws InvalidParseOperationException, CertificateException {
        ParametersChecker.checkParameter("Certificate cannot be null", (Object) identityInsertModel.getCertificate());
        final Optional<IdentityModel> identity = identityService.findIdentity(identityInsertModel.getCertificate());
        if (identity.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        identityService.createIdentity(identityInsertModel);
        return Response.created(uri.getRequestUri().normalize()).build();
    }

    /**
     * @param identityInsertModel
     * @param uri
     * @return
     * @throws InvalidParseOperationException
     * @throws CertificateException
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public IdentityModel linkContextToIdentity(IdentityInsertModel identityInsertModel, @Context UriInfo uri)
        throws InvalidParseOperationException, CertificateException {
        ParametersChecker.checkParameter("ContextId cannot be null", identityInsertModel.getContextId());
        ParametersChecker.checkParameter("Certificate cannot be null", identityInsertModel.getCertificate());
        return identityService.linkContextToIdentity(identityInsertModel).orElseThrow(NotFoundException::new);
    }
}
