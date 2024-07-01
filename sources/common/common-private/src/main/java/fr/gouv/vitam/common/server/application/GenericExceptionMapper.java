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
package fr.gouv.vitam.common.server.application;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.base.Strings;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Generic Exception Mapper for Jetty Server
 */
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GenericExceptionMapper.class);
    private static final String URI_HOST_PORT_PATTERN = "(https|http)://[a-zA-Z0-9.\\-_]+(:[0-9]+)?/";

    @Override
    public Response toResponse(Throwable exception) {
        final VitamError vitamError = new VitamError(VitamCodeHelper.getCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR));
        String description = exception.getMessage();
        if (Strings.isNullOrEmpty(description)) {
            description = StringUtils.getClassName(exception);
        } else {
            description = description.replaceAll(URI_HOST_PORT_PATTERN, "");
        }
        vitamError
            .setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getMessage())
            .setDescription(description)
            .setState(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.name())
            .setHttpCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getStatus().getStatusCode());
        if (exception instanceof BadRequestException || exception instanceof JsonMappingException) {
            vitamError.setMessage(description).setHttpCode(Status.BAD_REQUEST.getStatusCode());
        } else if (exception instanceof ForbiddenException) {
            vitamError.setMessage(description).setHttpCode(Status.FORBIDDEN.getStatusCode());
        } else if (exception instanceof NotAcceptableException) {
            vitamError.setMessage(description).setHttpCode(Status.NOT_ACCEPTABLE.getStatusCode());
        } else if (exception instanceof NotAllowedException) {
            vitamError.setMessage(description).setHttpCode(Status.METHOD_NOT_ALLOWED.getStatusCode());
        } else if (exception instanceof NotAuthorizedException) {
            vitamError.setMessage(description).setHttpCode(Status.UNAUTHORIZED.getStatusCode());
        } else if (exception instanceof NotFoundException) {
            vitamError.setMessage(description).setHttpCode(Status.NOT_FOUND.getStatusCode());
        } else if (exception instanceof NotSupportedException) {
            vitamError.setMessage(description).setHttpCode(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode());
        } else if (exception instanceof RedirectionException) {
            vitamError.setMessage(description).setHttpCode(Status.SEE_OTHER.getStatusCode());
        } else if (exception instanceof ServiceUnavailableException) {
            vitamError.setMessage(description).setHttpCode(Status.SERVICE_UNAVAILABLE.getStatusCode());
        }
        LOGGER.error(vitamError.toString(), exception);
        return Response.status(vitamError.getHttpCode())
            .entity(vitamError)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }
}
