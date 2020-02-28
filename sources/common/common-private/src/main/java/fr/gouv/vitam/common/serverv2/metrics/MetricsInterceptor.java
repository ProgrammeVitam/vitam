/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.serverv2.metrics;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;

import fr.gouv.vitam.common.logging.SysErrLogger;

/**
 */
public class MetricsInterceptor implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String CONTEXT_KEY = "context";
    private final Timer timer;
    private final Meter meter;

    public MetricsInterceptor(Timer timer, Meter meter) {
        this.timer = timer;
        this.meter = meter;
    }


    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Called when a request is received
        requestContext.setProperty(CONTEXT_KEY, timer.time());
        meter.mark();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
        // Called when the response is about to be sent back
        try {
            ((Timer.Context) requestContext.getProperty(CONTEXT_KEY)).stop();
        } catch (NullPointerException | ClassCastException e) {
            // Silently ignore this for now.
            // KWA FIXME : is it really OK to discard all these exceptions ?
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        // KWA TODO: manage error codes here ?
    }
}
