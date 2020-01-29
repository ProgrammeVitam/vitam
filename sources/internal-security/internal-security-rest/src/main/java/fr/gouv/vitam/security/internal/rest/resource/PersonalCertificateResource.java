/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.security.internal.rest.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.security.internal.common.exception.PersonalCertificateException;
import fr.gouv.vitam.security.internal.common.model.IsPersonalCertificateRequiredModel;
import fr.gouv.vitam.security.internal.rest.service.PermissionService;
import fr.gouv.vitam.security.internal.rest.service.PersonalCertificateService;

/**
 * public resource to personal resource
 */
@Path("/v1/api/personalCertificate")
public class PersonalCertificateResource {

    private final PermissionService permissionService;

    private final PersonalCertificateService personalCertificateService;

    /**
     * Constructor
     * 
     * @param permissionService
     * @param personalCertificateService
     */
    public PersonalCertificateResource(
        PermissionService permissionService,
        PersonalCertificateService personalCertificateService) {
        this.permissionService = permissionService;
        this.personalCertificateService = personalCertificateService;
    }

    /**
     * Check personal certificate
     * 
     * @param certificate the certificate
     * @param permission the permission
     * @throws LogbookClientServerException
     * @throws LogbookClientAlreadyExistsException
     * @throws LogbookClientBadRequestException
     * @throws InvalidParseOperationException
     * @throws PersonalCertificateException
     */
    @Path("/personal-certificate-check/{permission}")
    @GET
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public void checkPersonalCertificate(byte[] certificate,
        @PathParam("permission") String permission)
        throws LogbookClientServerException, LogbookClientAlreadyExistsException, LogbookClientBadRequestException,
        InvalidParseOperationException, PersonalCertificateException {
        ParametersChecker.checkParameter("Permission cannot be null", permission);

        personalCertificateService.checkPersonalCertificateExistence(certificate, permission);
    }

    /**
     * Gets whether personal certificate if required for the provided endpoint permission
     *
     * @param permission the endpoint permission
     * @return the requirement as a IsPersonalCertificateRequiredModel object
     */
    @GET
    @Path("/permission-check/{permission}")
    @Produces(MediaType.APPLICATION_JSON)
    public IsPersonalCertificateRequiredModel isPersonalCertificateRequiredForPermission(
        @PathParam("permission") String permission) {
        ParametersChecker.checkParameter("Permission cannot be null", permission);

        return permissionService.isPersonalCertificateRequiredForPermission(permission);
    }
}
