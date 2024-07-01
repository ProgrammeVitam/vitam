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

package fr.gouv.vitam.storage.engine.common.metrics;

import fr.gouv.vitam.common.client.VitamAutoClosableResponse;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.lang.annotation.Annotation;

public class DownloadCountingSizeMetricsResponse extends VitamAutoClosableResponse {

    private final InputStream inputStream;

    public DownloadCountingSizeMetricsResponse(
        Integer tenant,
        String strategy,
        String offer,
        String origin,
        DataCategory dataCategory,
        Response response
    ) {
        super(response);
        InputStream is = response.readEntity(InputStream.class);
        this.inputStream = new DownloadCountingInputStreamMetrics(tenant, strategy, offer, origin, dataCategory, is);
    }

    @Override
    public Object getEntity() {
        return inputStream;
    }

    @Override
    public <T> T readEntity(Class<T> entityType) {
        if (InputStream.class.isAssignableFrom(entityType)) {
            return (T) inputStream;
        }
        throw new IllegalStateException("InputStream is required");
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType) {
        if (InputStream.class.isAssignableFrom(entityType.getRawType())) {
            return (T) inputStream;
        }
        throw new IllegalStateException("InputStream is required");
    }

    @Override
    public <T> T readEntity(Class<T> entityType, Annotation[] annotations) {
        if (InputStream.class.isAssignableFrom(entityType)) {
            return (T) inputStream;
        }
        throw new IllegalStateException("InputStream is required");
    }

    @Override
    public <T> T readEntity(GenericType<T> entityType, Annotation[] annotations) {
        if (InputStream.class.isAssignableFrom(entityType.getRawType())) {
            return (T) inputStream;
        }
        throw new IllegalStateException("InputStream is required");
    }
}
