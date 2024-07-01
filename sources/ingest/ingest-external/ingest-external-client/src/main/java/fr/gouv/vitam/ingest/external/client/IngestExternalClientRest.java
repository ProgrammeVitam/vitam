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

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.external.client.DefaultClient;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.model.LocalFile;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalClientNotFoundException;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalClientServerException;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;

import static fr.gouv.vitam.common.client.VitamRequestBuilder.get;
import static fr.gouv.vitam.common.client.VitamRequestBuilder.post;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.http.HttpHeaders.EXPECT;
import static org.apache.http.protocol.HTTP.EXPECT_CONTINUE;

/**
 * Ingest External client
 */
class IngestExternalClientRest extends DefaultClient implements IngestExternalClient {

    private static final String INGEST_EXTERNAL_MODULE = "IngestExternalModule";
    private static final String INGEST_URL = "/ingests";
    private static final String BLANK_OBJECT_ID = "object identifier should be filled";
    private static final String BLANK_TYPE = "Type should be filled";

    IngestExternalClientRest(IngestExternalClientFactory factory) {
        super(factory);
    }

    @Override
    public RequestResponse<Void> ingest(VitamContext vitamContext, InputStream stream, String contextId, String action)
        throws IngestExternalException {
        return ingest(vitamContext, stream, new IngestRequestParameters(contextId, action));
    }

    @Override
    public RequestResponse<Void> ingest(
        VitamContext vitamContext,
        InputStream stream,
        IngestRequestParameters ingestRequestParameters
    ) throws IngestExternalException {
        ParametersChecker.checkParameter("Tenant identifier is a mandatory parameter", vitamContext.getTenantId());

        final MultivaluedMap<String, Object> headers = vitamContext.getHeaders();

        VitamRequestBuilder request = post()
            .withPath(INGEST_URL)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(GlobalDataRest.X_CONTEXT_ID, ingestRequestParameters.getContextId())
            .withHeader(GlobalDataRest.X_ACTION, ingestRequestParameters.getAction())
            .withHeaderIgnoreNull(
                GlobalDataRest.X_MANIFEST_DIGEST_ALGORITHM,
                ingestRequestParameters.getManifestDigestAlgo()
            )
            .withHeaderIgnoreNull(
                GlobalDataRest.X_MANIFEST_DIGEST_VALUE,
                ingestRequestParameters.getManifestDigestValue()
            )
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withBody(stream, "Stream is a mandatory parameter")
            .withOctetContentType()
            .withXMLAccept();

        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK<Void>().parseHeadersFromResponse(response).setHttpCode(response.getStatus());
        } catch (IngestExternalClientServerException vitamError) {
            return vitamError.getVitamError();
        } catch (VitamClientInternalException | IngestExternalClientNotFoundException e) {
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        }
    }

    public Response downloadObjectAsync(VitamContext vitamContext, String objectId, IngestCollection type)
        throws VitamClientException {
        ParametersChecker.checkParameter(BLANK_OBJECT_ID, objectId);
        ParametersChecker.checkParameter(BLANK_TYPE, type);

        Response response = null;
        VitamRequestBuilder request = get()
            .withPath(INGEST_URL + "/" + objectId + "/" + type.getCollectionName())
            .withHeaders(vitamContext.getHeaders())
            .withOctetAccept();
        try {
            response = make(request);
            check(response);
            return response;
        } catch (IngestExternalClientServerException | IngestExternalException e) {
            throw new VitamClientException(e);
        } catch (IngestExternalClientNotFoundException e) {
            return response;
        } finally {
            if (
                response != null &&
                SUCCESSFUL != response.getStatusInfo().getFamily() &&
                Status.NOT_FOUND.getStatusCode() != response.getStatus()
            ) {
                response.close();
            }
        }
    }

    @Override
    public RequestResponse<Void> ingestLocal(
        VitamContext vitamContext,
        LocalFile localFile,
        String contextId,
        String action
    ) throws IngestExternalException {
        return ingestLocal(vitamContext, localFile, new IngestRequestParameters(contextId, action));
    }

    @Override
    public RequestResponse<Void> ingestLocal(
        VitamContext vitamContext,
        LocalFile localFile,
        IngestRequestParameters ingestRequestParameters
    ) throws IngestExternalException {
        ParametersChecker.checkParameter("Tenant identifier is a mandatory parameter", vitamContext.getTenantId());

        VitamRequestBuilder request = post()
            .withPath(INGEST_URL)
            .withHeaders(vitamContext.getHeaders())
            .withHeader(GlobalDataRest.X_CONTEXT_ID, ingestRequestParameters.getContextId())
            .withHeader(GlobalDataRest.X_ACTION, ingestRequestParameters.getAction())
            .withHeaderIgnoreNull(
                GlobalDataRest.X_MANIFEST_DIGEST_ALGORITHM,
                ingestRequestParameters.getManifestDigestAlgo()
            )
            .withHeaderIgnoreNull(
                GlobalDataRest.X_MANIFEST_DIGEST_VALUE,
                ingestRequestParameters.getManifestDigestValue()
            )
            .withHeader(EXPECT, EXPECT_CONTINUE)
            .withBody(localFile, "localFile is a mandatory parameter")
            .withJsonContentType()
            .withXMLAccept();
        try (Response response = make(request)) {
            check(response);
            return new RequestResponseOK<Void>().parseHeadersFromResponse(response).setHttpCode(response.getStatus());
        } catch (IngestExternalClientServerException vitamError) {
            return vitamError.getVitamError();
        } catch (VitamClientInternalException | IngestExternalClientNotFoundException e) {
            throw new IngestExternalException("Ingest External Internal Server Error", e);
        }
    }

    private void check(Response response)
        throws IngestExternalException, IngestExternalClientServerException, IngestExternalClientNotFoundException {
        Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily())) {
            return;
        }

        switch (status) {
            case BAD_REQUEST:
            case PARTIAL_CONTENT:
            case INTERNAL_SERVER_ERROR:
                final VitamError vitamError = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                    .setHttpCode(status.getStatusCode())
                    .setDescription(
                        VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " + status.getReasonPhrase()
                    )
                    .setMessage(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage())
                    .setState(StatusCode.KO.name())
                    .setContext(INGEST_EXTERNAL_MODULE);
                throw new IngestExternalClientServerException(vitamError);
            case SERVICE_UNAVAILABLE:
                final VitamError vitamErrorFatal = new VitamError(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getItem())
                    .setHttpCode(status.getStatusCode())
                    .setDescription(
                        VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage() + " Cause : " + status.getReasonPhrase()
                    )
                    .setMessage(VitamCode.INGEST_EXTERNAL_UPLOAD_ERROR.getMessage())
                    .setState(StatusCode.FATAL.name())
                    .setContext(INGEST_EXTERNAL_MODULE);
                throw new IngestExternalClientServerException(vitamErrorFatal);
            case NOT_FOUND:
                throw new IngestExternalClientNotFoundException("Not Found");
            default:
                throw new IngestExternalException(status.getReasonPhrase());
        }
    }
}
