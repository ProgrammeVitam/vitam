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
package fr.gouv.vitam.collect.internal.resource;

import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.collect.internal.service.CollectService;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/v1/transaction")
public class TransactionResource {
    private CollectService collectService;
    private final WorkspaceClientFactory workspaceClientFactory;
    private static final String FOLDER_SIP = "SIP";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransactionResource.class);

    public TransactionResource(CollectService collectService, CollectConfiguration collectConfiguration) {
        this.collectService = collectService;
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl());
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance();
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response initTransaction() throws InvalidParseOperationException {
        String requestId = collectService.createRequestId();
        collectService.createCollect(new CollectModel(requestId));
        RequestResponseOK<String> transactionResponse = new RequestResponseOK<String>()
                .addResult(requestId).setHttpCode(Response.Status.OK.getStatusCode());
        return Response.status(Response.Status.OK).entity(transactionResponse).build();
    }


    @Path("/{transactionId}/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@PathParam("transactionId") String transactionId, InputStream uploadedInputStream) throws InvalidParseOperationException {
        Optional<CollectModel> collectModel = collectService.findCollect(transactionId);
        if (collectModel.isEmpty()){
            LOGGER.debug("Unable to find transaction Id");
            return Response.status(Response.Status.BAD_REQUEST).entity(null).build();
        }
        pushSipStreamToWorkspace(uploadedInputStream, collectModel.get().getId());
        return Response.status(Response.Status.OK).entity(null).build();
    }


    public void pushSipStreamToWorkspace(final InputStream uploadedInputStream, String containerName) {

        LOGGER.debug("Try to push stream to workspace...");
        // call workspace
        MediaType mediaType = CommonMediaType.valueOf("application/zip");
        String archiveMimeType = CommonMediaType.mimeTypeOf(mediaType);
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            workspaceClient.createContainer(containerName);
            workspaceClient.uncompressObject(containerName, FOLDER_SIP, archiveMimeType, uploadedInputStream);
        } catch (ContentAddressableStorageException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeSilently(uploadedInputStream);
        }
        LOGGER.debug(" -> push stream to workspace finished");
    }
}
