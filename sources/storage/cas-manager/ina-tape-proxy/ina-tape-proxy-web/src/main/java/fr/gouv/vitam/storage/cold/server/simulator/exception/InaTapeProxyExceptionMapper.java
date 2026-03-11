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
package fr.gouv.vitam.storage.cold.server.simulator.exception;

import com.google.common.base.Strings;
import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.StringUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Generic Exception Mapper for Jetty Server
 */
public class InaTapeProxyExceptionMapper implements ExceptionMapper<InaTapeProxyException> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyExceptionMapper.class);
    private static final String URI_HOST_PORT_PATTERN = "(https|http)://[a-zA-Z0-9.\\-_]+(:[0-9]+)?/";

    @Override
    public Response toResponse(InaTapeProxyException exception) {
        String description = exception.getMessage();
        if (Strings.isNullOrEmpty(description)) {
            description = StringUtils.getClassName(exception);
        } else {
            description = description.replaceAll(URI_HOST_PORT_PATTERN, "");
        }

        VitamCode vitamCode;
        if (exception instanceof InaTapeProxyBadRequestException) {
            vitamCode = VitamCode.INA_TAPE_PROXY_BAD_REQUEST;
        } else {
            vitamCode = VitamCode.INA_TAPE_PROXY_INTERNAL_SERVER_ERROR;
        }

        VitamError<?> vitamError = new VitamError<>(VitamCodeHelper.getCode(vitamCode))
            .setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(vitamCode.getMessage())
            .setDescription(description)
            .setState(vitamCode.name())
            .setHttpCode(vitamCode.getStatus().getStatusCode());

        LOGGER.error(vitamError.toString(), exception);
        return Response.status(vitamError.getHttpCode())
            .entity(vitamError)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }
}
