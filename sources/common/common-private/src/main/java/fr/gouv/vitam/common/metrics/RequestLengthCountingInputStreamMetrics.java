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

package fr.gouv.vitam.common.metrics;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import io.prometheus.client.Summary;
import org.apache.commons.io.input.CountingInputStream;

import javax.ws.rs.container.ContainerRequestContext;
import java.io.InputStream;

public class RequestLengthCountingInputStreamMetrics extends CountingInputStream {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(
        RequestLengthCountingInputStreamMetrics.class
    );

    public static final Summary RECEIVED_BYTES = Summary.build()
        .name(VitamMetricsNames.VITAM_REQUESTS_SIZE_BYTES)
        .labelNames("tenant", "method")
        .help("Vitam requests size in bytes per tenant and method")
        .register();

    private final ContainerRequestContext requestContext;

    private boolean first = true;

    public RequestLengthCountingInputStreamMetrics(ContainerRequestContext requestContext, InputStream inputStream) {
        super(inputStream);
        ParametersChecker.checkParameter("RequestContext param is required", requestContext);
        this.requestContext = requestContext;
    }

    @Override
    protected void afterRead(int n) {
        if (n == -1 && first) {
            first = false;

            onEndOfFileReached();
        }
        super.afterRead(n);
    }

    private void onEndOfFileReached() {
        try {
            String headerString = requestContext.getHeaderString(GlobalDataRest.X_TENANT_ID);
            String tenant = headerString == null ? "unknown_tenant" : headerString;

            String method = requestContext.getMethod();

            RECEIVED_BYTES.labels(tenant, method).observe(super.getByteCount());
        } catch (Exception e) {
            LOGGER.warn(e);
        }
    }
}
