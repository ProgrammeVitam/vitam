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
package fr.gouv.vitam.common.security.waf;

import fr.gouv.vitam.common.alert.AlertService;
import fr.gouv.vitam.common.alert.AlertServiceImpl;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jboss.resteasy.core.interception.jaxrs.PostMatchContainerRequestContext;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Filter checker for body json
 */
@Priority(Priorities.AUTHORIZATION)
public class SanityCheckerJsonFilter implements ContainerRequestFilter {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SanityCheckerJsonFilter.class);
    private static final String CHECK_SANITY = "CHECK_SANITY";
    private static final String CODE_VITAM = "code_vitam";
    private static final AlertService alertService = new AlertServiceImpl();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final InputStream bodyInputStream = requestContext.getEntityStream();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bodyInputStream.transferTo(bout);
        try {
            SanityChecker.checkJsonAll(JsonHandler.getFromInputStream(bout.toInputStream()));
            requestContext.setEntityStream(bout.toInputStream());
        } catch (final IllegalArgumentException exc) {
            LOGGER.error(exc);
            if (
                Arrays.asList(
                    ((PostMatchContainerRequestContext) requestContext).getResourceMethod().getProduces()
                ).contains(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            ) {
                // no entity
                requestContext.abortWith(Response.status(Status.PRECONDITION_FAILED).build());
            } else {
                requestContext.abortWith(
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED, exc.getMessage()))
                        .build()
                );
            }
        } catch (InvalidParseOperationException exc) {
            LOGGER.error(exc);
            alertService.createAlert("Json invalid: " + exc.getMessage());
            if (
                Arrays.asList(
                    ((PostMatchContainerRequestContext) requestContext).getResourceMethod().getProduces()
                ).contains(MediaType.APPLICATION_OCTET_STREAM_TYPE)
            ) {
                // no entity
                requestContext.abortWith(Response.status(Status.PRECONDITION_FAILED).build());
            } else {
                requestContext.abortWith(
                    Response.status(Status.PRECONDITION_FAILED)
                        .entity(getErrorEntity(Status.PRECONDITION_FAILED, exc.getMessage()))
                        .build()
                );
            }
        }
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setContext(CHECK_SANITY)
            .setState(CODE_VITAM)
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }
}
