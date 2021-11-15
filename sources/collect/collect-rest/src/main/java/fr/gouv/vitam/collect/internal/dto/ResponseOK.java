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
package fr.gouv.vitam.collect.internal.dto;

import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;

import javax.ws.rs.core.Response;
import java.util.Map;


/**
 * Access RequestResponseOK class contains list of results<br>
 * default results : is an empty list (immutable)
 *
 * @param <T> Type of results
 */
public final class ResponseOK<T> extends RequestResponse<T> {

    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public ResponseOK<T> setHttpCode(int httpCode) {
        super.setHttpCode(httpCode);
        return this;
    }

    /**
     * transform a RequestResponse to a standard response
     *
     * @return Response
     */
    @Override
    public Response toResponse() {
        final Response.ResponseBuilder resp = Response.status(getStatus()).entity(JsonHandler.unprettyPrint(this));
        final Map<String, String> vitamHeaders = getVitamHeaders();
        for (final String key : vitamHeaders.keySet()) {
            resp.header(key, getHeaderString(key));
        }

        unSetVitamHeaders();
        return resp.build();
    }
}
