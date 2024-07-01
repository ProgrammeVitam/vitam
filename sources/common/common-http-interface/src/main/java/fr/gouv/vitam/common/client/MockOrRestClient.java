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
package fr.gouv.vitam.common.client;

import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.model.VitamAutoCloseable;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

/**
 * Basic client api for vitam client either in Mock or Rest mode
 */
public interface MockOrRestClient extends VitamAutoCloseable {
    /**
     * Get the resource path of the server.
     *
     * @return the resource path as string
     */
    String getResourcePath();

    /**
     * Get the service URL
     *
     * @return the service URL
     */
    String getServiceUrl();

    /**
     * Check the status from the service
     *
     * @throws VitamApplicationServerException if the Server is unavailable
     */
    void checkStatus() throws VitamApplicationServerException;

    /**
     * Check the status from the service
     * used to passe "X-SSL-CLIENT-CERT" header that contain a pem certificate
     *
     * @param headers
     * @throws VitamApplicationServerException if the Server is unavailable
     */
    void checkStatus(MultivaluedHashMap<String, Object> headers) throws VitamApplicationServerException;

    /**
     * Helper when an error occurs on client usage side to consume response however
     *
     * @param response
     */
    void consumeAnyEntityAndClose(Response response);

    /**
     * Close the underneath http client
     */
    @Override
    void close();
}
