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
package fr.gouv.vitam.common.server2.application;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

import fr.gouv.vitam.common.ServerIdentity;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

/**
 * Generic Exception Mapper for Jersey Server
 */
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GenericExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        final VitamError vitamError = new VitamError(VitamCodeHelper.getCode(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR));
        vitamError.setContext(ServerIdentity.getInstance().getJsonIdentity())
            .setMessage(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.getMessage())
            .setDescription(exception.getLocalizedMessage())
            .setState(VitamCode.GLOBAL_INTERNAL_SERVER_ERROR.name());
        LOGGER.error(vitamError.toString(), exception);
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(vitamError).type(MediaType.APPLICATION_JSON_TYPE)
            .build();
    }

}
