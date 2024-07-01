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
package fr.gouv.vitam.ingest.external.rest;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.LocalFile;
import fr.gouv.vitam.common.model.LocalFileAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.IllegalPathException;
import fr.gouv.vitam.common.security.SafeFileChecker;
import fr.gouv.vitam.common.security.rest.EndpointInfo;
import fr.gouv.vitam.common.security.rest.SecureEndpointRegistry;
import fr.gouv.vitam.common.security.rest.Secured;
import fr.gouv.vitam.common.security.rest.Unsecured;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.VitamAsyncInputStreamResponse;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import fr.gouv.vitam.ingest.external.core.AtrKoBuilder;
import fr.gouv.vitam.ingest.external.core.IngestExternalImpl;
import fr.gouv.vitam.ingest.external.core.ManifestDigestValidator;
import fr.gouv.vitam.ingest.external.core.PreUploadResume;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientNotFoundException;
import fr.gouv.vitam.ingest.internal.common.exception.IngestInternalClientServerException;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import static fr.gouv.vitam.utils.SecurityProfilePermissions.INGESTS_CREATE;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.INGESTS_ID_ARCHIVETRANSFERTREPLY_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.INGESTS_ID_MANIFESTS_READ;
import static fr.gouv.vitam.utils.SecurityProfilePermissions.INGESTS_LOCAL_CREATE;

@Path("/ingest-external/v1")
@Tag(name = "Ingest")
public class IngestExternalResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalResource.class);

    private final IngestExternalConfiguration ingestExternalConfiguration;

    private final SecureEndpointRegistry secureEndpointRegistry;
    private final FormatIdentifierFactory formatIdentifierFactory;
    private final IngestInternalClientFactory ingestInternalClientFactory;

    /**
     * Constructor IngestExternalResource
     *
     * @param ingestExternalConfiguration the configuration of server resource
     * @param secureEndpointRegistry
     */
    public IngestExternalResource(
        IngestExternalConfiguration ingestExternalConfiguration,
        SecureEndpointRegistry secureEndpointRegistry,
        FormatIdentifierFactory formatIdentifierFactory,
        IngestInternalClientFactory ingestInternalClientFactory
    ) {
        this.ingestExternalConfiguration = ingestExternalConfiguration;
        this.secureEndpointRegistry = secureEndpointRegistry;
        this.formatIdentifierFactory = formatIdentifierFactory;
        this.ingestInternalClientFactory = ingestInternalClientFactory;
        LOGGER.info("init Ingest External Resource server");
    }

    /**
     * List secured resource end points
     *
     * @return response
     */
    @Path("/")
    @OPTIONS
    @Produces(MediaType.APPLICATION_JSON)
    @Unsecured
    public Response listResourceEndpoints() {
        String resourcePath = IngestExternalResource.class.getAnnotation(Path.class).value();

        List<EndpointInfo> securedEndpointList = secureEndpointRegistry.getEndPointsByResourcePath(resourcePath);

        return Response.status(Status.OK).entity(securedEndpointList).build();
    }

    /**
     * upload the file in local
     *
     * @param contextId the context id of upload
     * @param action in workflow
     * @param uploadedInputStream data input stream
     * @param asyncResponse the asynchronized response
     */
    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(permission = INGESTS_CREATE, description = "Envoyer un SIP à Vitam afin qu'il en réalise l'entrée")
    public void upload(
        @HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
        @HeaderParam(GlobalDataRest.X_ACTION) String action,
        @HeaderParam(GlobalDataRest.X_MANIFEST_DIGEST_ALGORITHM) String manifestDigestAlgo,
        @HeaderParam(GlobalDataRest.X_MANIFEST_DIGEST_VALUE) String manifestDigestValue,
        InputStream uploadedInputStream,
        @Suspended final AsyncResponse asyncResponse
    ) {
        final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID guid = GUIDReader.getGUIDUnsafe(requestId);

        Integer tenantId = ParameterHelper.getTenantParameter();

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(
                () ->
                    uploadAsync(
                        uploadedInputStream,
                        asyncResponse,
                        tenantId,
                        contextId,
                        action,
                        manifestDigestValue,
                        manifestDigestAlgo,
                        guid,
                        Optional.empty()
                    )
            );
    }

    /**
     * upload a local file
     *
     * @param contextId the context id of upload
     * @param action in workflow
     * @param localFile local file
     * @param asyncResponse the asynchronized response
     */

    @Path("ingests")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(
        permission = INGESTS_LOCAL_CREATE,
        description = "Envoyer un SIP en local à Vitam afin qu'il en réalise l'entrée"
    )
    public void uploadLocal(
        @HeaderParam(GlobalDataRest.X_CONTEXT_ID) String contextId,
        @HeaderParam(GlobalDataRest.X_ACTION) String action,
        @HeaderParam(GlobalDataRest.X_MANIFEST_DIGEST_ALGORITHM) String manifestDigestAlgo,
        @HeaderParam(GlobalDataRest.X_MANIFEST_DIGEST_VALUE) String manifestDigestValue,
        LocalFile localFile,
        @Suspended final AsyncResponse asyncResponse
    ) {
        final String requestId = VitamThreadUtils.getVitamSession().getRequestId();
        GUID guid = GUIDReader.getGUIDUnsafe(requestId);

        Integer tenantId = ParameterHelper.getTenantParameter();

        java.nio.file.Path path;
        try {
            path = SafeFileChecker.checkSafeFilePath(
                ingestExternalConfiguration.getBaseUploadPath(),
                localFile.getPath()
            ).toPath();
        } catch (IllegalPathException e) {
            LOGGER.error("Path traversal check failed", e);
            doAsyncResponse(
                guid,
                asyncResponse,
                Status.BAD_REQUEST,
                VitamCode.INGEST_EXTERNAL_LOCAL_UPLOAD_FILE_SECURITY_ALERT,
                "Only files in the folder " + ingestExternalConfiguration.getBaseUploadPath() + " could be accessed"
            );
            return;
        }

        boolean exists = Files.exists(path);

        if (!exists) {
            doAsyncResponse(
                guid,
                asyncResponse,
                Status.INTERNAL_SERVER_ERROR,
                VitamCode.INGEST_EXTERNAL_LOCAL_UPLOAD_FILE_HANDLING_ERROR,
                "File path does not exist."
            );
        }

        VitamThreadPoolExecutor.getDefaultExecutor()
            .execute(() -> {
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(path.toString()))) {
                    uploadAsync(
                        inputStream,
                        asyncResponse,
                        tenantId,
                        contextId,
                        action,
                        manifestDigestValue,
                        manifestDigestAlgo,
                        guid,
                        Optional.of(path)
                    );
                } catch (IOException e) {
                    LOGGER.error(e);
                    doAsyncResponse(
                        guid,
                        asyncResponse,
                        Status.INTERNAL_SERVER_ERROR,
                        VitamCode.INGEST_EXTERNAL_LOCAL_UPLOAD_FILE_HANDLING_ERROR,
                        e.getLocalizedMessage()
                    );
                }
            });
    }

    private void doAsyncResponse(
        final GUID requestId,
        AsyncResponse asyncResponse,
        Status status,
        VitamCode vitamCode,
        String message
    ) {
        AsyncInputStreamHelper.asyncResponseResume(
            asyncResponse,
            Response.status(status)
                .header(GlobalDataRest.X_REQUEST_ID, requestId)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL)
                .entity(getErrorStream(VitamCodeHelper.toVitamError(vitamCode, message)))
                .build()
        );
    }

    private void uploadAsync(
        InputStream uploadedInputStream,
        AsyncResponse asyncResponse,
        Integer tenantId,
        String contextId,
        String xAction,
        String manifestDigestValue,
        String manifestDigestAlgo,
        GUID operationId,
        Optional<java.nio.file.Path> localFilePath
    ) {
        final IngestExternalImpl ingestExternal = new IngestExternalImpl(
            ingestExternalConfiguration,
            formatIdentifierFactory,
            ingestInternalClientFactory,
            new ManifestDigestValidator()
        );
        final LocalFileAction afterUploadAction = LocalFileAction.getLocalFileAction(
            ingestExternalConfiguration.getFileActionAfterUpload().name()
        );
        try {
            ParametersChecker.checkParameter("HTTP Request must contains stream", uploadedInputStream);
            VitamThreadUtils.getVitamSession().setTenantId(tenantId);
            PreUploadResume preUploadResume;
            try {
                preUploadResume = ingestExternal.preUploadAndResume(
                    uploadedInputStream,
                    contextId,
                    operationId,
                    xAction,
                    asyncResponse
                );
            } catch (WorkflowNotFoundException ex) {
                LOGGER.error(ex);
                String atr = AtrKoBuilder.buildAtrKo(
                    operationId.getId(),
                    "ArchivalAgencyToBeDefined",
                    "TransferringAgencyToBeDefined",
                    IngestExternalImpl.INGEST_INT_UPLOAD,
                    ex.getMessage(),
                    StatusCode.KO,
                    LocalDateUtil.now()
                );
                ingestExternal.handleResponseWithATR(operationId, asyncResponse, atr);
                return;
            }
            ingestExternal.upload(preUploadResume, xAction, operationId, manifestDigestValue, manifestDigestAlgo);

            if (localFilePath.isPresent()) {
                java.nio.file.Path path = localFilePath.get();

                switch (afterUploadAction) {
                    case DELETE:
                        Files.delete(path);
                        break;
                    case MOVE:
                        if (
                            ingestExternalConfiguration.getSuccessfulUploadDir() != null &&
                            !ingestExternalConfiguration.getSuccessfulUploadDir().isEmpty()
                        ) {
                            Files.move(
                                path,
                                Paths.get(
                                    ingestExternalConfiguration.getSuccessfulUploadDir(),
                                    path.toFile().getName()
                                ),
                                StandardCopyOption.REPLACE_EXISTING
                            );
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (final Exception exc) {
            if (localFilePath.isPresent()) {
                try {
                    if (
                        afterUploadAction.equals(LocalFileAction.MOVE) &&
                        ingestExternalConfiguration.getFailedUploadDir() != null &&
                        !ingestExternalConfiguration.getFailedUploadDir().isEmpty()
                    ) {
                        java.nio.file.Path path = localFilePath.get();
                        Files.move(
                            path,
                            Paths.get(ingestExternalConfiguration.getFailedUploadDir(), path.toFile().getName()),
                            StandardCopyOption.REPLACE_EXISTING
                        );
                    }
                } catch (IOException e) {
                    LOGGER.error(e);
                    AsyncInputStreamHelper.asyncResponseResume(
                        asyncResponse,
                        Response.status(Status.INTERNAL_SERVER_ERROR)
                            .header(GlobalDataRest.X_REQUEST_ID, operationId.getId())
                            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                            .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL)
                            .entity(
                                getErrorStream(
                                    VitamCodeHelper.toVitamError(
                                        VitamCode.INGEST_EXTERNAL_LOCAL_UPLOAD_FILE_HANDLING_ERROR,
                                        e.getLocalizedMessage()
                                    )
                                )
                            )
                            .build()
                    );
                }
            }
            LOGGER.error(exc);
            AsyncInputStreamHelper.asyncResponseResume(
                asyncResponse,
                Response.status(Status.INTERNAL_SERVER_ERROR)
                    .header(GlobalDataRest.X_REQUEST_ID, operationId.getId())
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATE, ProcessState.COMPLETED)
                    .header(GlobalDataRest.X_GLOBAL_EXECUTION_STATUS, StatusCode.FATAL)
                    .entity(
                        getErrorStream(
                            VitamCodeHelper.toVitamError(
                                VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR,
                                exc.getLocalizedMessage()
                            )
                        )
                    )
                    .build(),
                uploadedInputStream
            );
        }
    }

    /**
     * Download archive transfer reply stored by Ingest operation
     * <p>
     * Return the archive transfer reply as stream asynchronously<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param objectId the id of archive transfer reply to download
     * @return response
     */
    @GET
    @Path("/ingests/{objectId}/archivetransferreply")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(
        permission = INGESTS_ID_ARCHIVETRANSFERTREPLY_READ,
        description = "Récupérer l'accusé de récéption pour une opération d'entrée donnée"
    )
    public Response downloadArchiveTransferReplyAsStream(@PathParam("objectId") String objectId) {
        return downloadObjectAsync(objectId, IngestCollection.REPORTS);
    }

    /**
     * Download manifest stored by Ingest operation
     * <p>
     * Return the manifest as stream asynchronously<br/>
     * <br/>
     * <b>The caller is responsible to close the Response after consuming the inputStream.</b>
     *
     * @param objectId the id of object to download
     * @return The given response with the manifest
     */
    @GET
    @Path("/ingests/{objectId}/manifests")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Secured(
        permission = INGESTS_ID_MANIFESTS_READ,
        description = "Récupérer le bordereau de versement pour une opération d'entrée donnée"
    )
    public Response downloadIngestManifestsAsStream(@PathParam("objectId") String objectId) {
        return downloadObjectAsync(objectId, IngestCollection.MANIFESTS);
    }

    private Response downloadObjectAsync(String objectId, IngestCollection collection) {
        try (IngestInternalClient ingestInternalClient = IngestInternalClientFactory.getInstance().getClient()) {
            final Response response = ingestInternalClient.downloadObjectAsync(objectId, collection);
            return new VitamAsyncInputStreamResponse(response);
        } catch (IllegalArgumentException e) {
            LOGGER.error("IllegalArgumentException was thrown : ", e);
            return Response.status(Status.BAD_REQUEST)
                .entity(
                    getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_BAD_REQUEST, e.getLocalizedMessage())
                    )
                )
                .build();
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Predicates Failed Exception", e);
            return Response.status(Status.PRECONDITION_FAILED)
                .entity(
                    getErrorStream(
                        VitamCodeHelper.toVitamError(
                            VitamCode.INGEST_EXTERNAL_PRECONDITION_FAILED,
                            e.getLocalizedMessage()
                        )
                    )
                )
                .build();
        } catch (final IngestInternalClientServerException e) {
            LOGGER.error("Internal Server Exception ", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                .entity(
                    getErrorStream(
                        VitamCodeHelper.toVitamError(
                            VitamCode.INGEST_EXTERNAL_INTERNAL_SERVER_ERROR,
                            e.getLocalizedMessage()
                        )
                    )
                )
                .build();
        } catch (final IngestInternalClientNotFoundException e) {
            LOGGER.error("Request resources does not exits", e);
            return Response.status(Status.NOT_FOUND)
                .entity(
                    getErrorStream(
                        VitamCodeHelper.toVitamError(VitamCode.INGEST_EXTERNAL_NOT_FOUND, e.getLocalizedMessage())
                    )
                )
                .build();
        }
    }

    private InputStream getErrorStream(VitamError vitamError) {
        try {
            return JsonHandler.writeToInpustream(vitamError);
        } catch (InvalidParseOperationException e) {
            return new ByteArrayInputStream("{ 'message' : 'Invalid VitamError message' }".getBytes());
        }
    }
}
