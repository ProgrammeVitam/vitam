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
package fr.gouv.vitam.collect.internal.service;

import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.client.IngestRequestParameters;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;

import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;

public class IngestSipService {

    private IngestExternalClientFactory ingestExternalClientFactory = IngestExternalClientFactory.getInstance();
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestSipService.class);
    private final WorkspaceClientFactory workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);

    public String ingest(CollectModel collectModel, String digest) {


        try (IngestExternalClient client = ingestExternalClientFactory.getClient()) {
            Integer tenantId = ParameterHelper.getTenantParameter();
            InputStream is = getFileFromWorkspace(collectModel);


            IngestRequestParameters ingestRequestParameters =
                    new IngestRequestParameters(DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name())
                            .setManifestDigestAlgo("SHA-512")
                            .setManifestDigestValue(digest);
            ;
            RequestResponse response = client.ingest(new VitamContext(tenantId).setApplicationSessionId("APPLICATION_SESSION_ID").setAccessContract("ContratTNR"),is,ingestRequestParameters);
            if(response.isOk()){
                return response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            }
        } catch (IngestExternalException e) {
            e.printStackTrace();
        } catch (ContentAddressableStorageServerException e) {
            e.printStackTrace();
        } catch (ContentAddressableStorageNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private InputStream getFileFromWorkspace(CollectModel collectModel) throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        LOGGER.debug("Try to get Zip from workspace...");
        InputStream is = null;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(collectModel.getId())) {
                Response response = workspaceClient.getObject(collectModel.getId(), collectModel.getId() + ".zip");
                if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                    is = (InputStream) response.getEntity();
                    workspaceClient.close();
                }
            }
        }
        LOGGER.debug(" -> get zip from workspace finished");
        return is;
    }


}
