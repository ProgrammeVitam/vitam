/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.processing.management.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.processing.common.ProcessingEntry;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkflowNotFoundException;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

/**
 * 
 */
public class ProcessingManagementClient {

    private Client client;
    private String url;
    private static final String RESOURCE_PATH = "/processing/api/v0.0.3";


    /**
     * @param url of metadata server
     */
    public ProcessingManagementClient(String url) {
        final ClientConfig clientConfig=new ClientConfig();
        clientConfig.register(JacksonJsonProvider.class);
        clientConfig.register(JacksonFeature.class);

        client = ClientBuilder.newClient(clientConfig);
        this.url = url + RESOURCE_PATH;
    }

    /**
     * @return : status of metadata server 200 : server is alive
     */
    public Response status() {
        return client.target(url).path("status").request().get();
    }


    /**
     * executeVitamProcess : processing operation of a workflow
     * 
     * @param container : name of container
     * @param workflow : id of workflow
     * @return : Engine response containe message and status
     * @throws ProcessingException
     */
    public String executeVitamProcess(String container, String workflow) throws ProcessingException, InvalidParseOperationException {
        ParametersChecker.checkParameter("container is a mandatory parameter", container);
        ParametersChecker.checkParameter("workflow is a mandatory parameter", workflow);

        Response response = client.target(url).path("operations").request(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(new ProcessingEntry(container, workflow), MediaType.APPLICATION_JSON), Response.class);

        if (response.getStatus() == Status.NOT_FOUND.getStatusCode()) {
            throw new WorkflowNotFoundException("Workflow Not Found");
        } else if (response.getStatus() == Status.PRECONDITION_FAILED.getStatusCode()) {
            throw new IllegalArgumentException("Illegal Argument");
        } else if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
            throw new ProcessingException("Unauthorized Operation");
        }

        return response.readEntity(String.class);
    }
}