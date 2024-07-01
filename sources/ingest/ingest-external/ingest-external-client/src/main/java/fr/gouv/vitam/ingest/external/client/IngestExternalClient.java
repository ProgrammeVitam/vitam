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
package fr.gouv.vitam.ingest.external.client;

import fr.gouv.vitam.common.client.MockOrRestClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.model.LocalFile;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Ingest external interface
 */
public interface IngestExternalClient extends MockOrRestClient {
    /**
     * ingest upload file in local and launch an ingest workflow
     *
     * @param vitamContext the vitam context
     * @param stream
     * @param contextId a type of ingest among "DEFAULT_WORKFLOW" (Sip ingest), "HOLDING_SCHEME" (tree)
     * "FILING_SCHEME" (plan) and "BLANK_TEST" (Sip ingest test)
     * @param action an action as a string among "RESUME" (launch workflow entirely) and "NEXT" (launch ingest in step
     * by step mode)
     * @return response
     * @throws IngestExternalException
     */
    RequestResponse<Void> ingest(VitamContext vitamContext, InputStream stream, String contextId, String action)
        throws IngestExternalException;

    /**
     * ingest upload file in local and launch an ingest workflow
     *
     * @param vitamContext the vitam context
     * @param stream sip input stream
     * @param ingestRequestParameters ingest request parameters
     * @return response
     * @throws IngestExternalException
     */
    RequestResponse<Void> ingest(
        VitamContext vitamContext,
        InputStream stream,
        IngestRequestParameters ingestRequestParameters
    ) throws IngestExternalException;

    /**
     * Download object stored by ingest operation<br>
     * <br>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param vitamContext the vitam context
     * @param objectId
     * @param type
     * @return object as stream
     * @throws VitamClientException
     */
    Response downloadObjectAsync(VitamContext vitamContext, String objectId, IngestCollection type)
        throws VitamClientException;

    /**
     * ingest a file that has been uploaded locally on a vitam folder then launch an ingest workflow
     *
     * @param vitamContext the vitam context
     * @param localFile the localFile information
     * @param contextId a type of ingest among "DEFAULT_WORKFLOW" (Sip ingest), "HOLDING_SCHEME" (tree)
     * "FILING_SCHEME" (plan) and "BLANK_TEST" (Sip ingest test)
     * @param action an action as a string among "RESUME" (launch workflow entirely) and "NEXT" (launch ingest in step
     * by step mode)
     * @return response
     * @throws IngestExternalException
     */
    RequestResponse<Void> ingestLocal(VitamContext vitamContext, LocalFile localFile, String contextId, String action)
        throws IngestExternalException;

    /**
     * ingest a file that has been uploaded locally on a vitam folder then launch an ingest workflow
     *
     * @param vitamContext the vitam context
     * @param localFile the localFile information
     * @param ingestRequestParameters ingest request parameters
     * @return response
     * @throws IngestExternalException
     */
    RequestResponse<Void> ingestLocal(
        VitamContext vitamContext,
        LocalFile localFile,
        IngestRequestParameters ingestRequestParameters
    ) throws IngestExternalException;
}
