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
package fr.gouv.vitam.scheduler.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.VitamClientFactoryInterface;
import fr.gouv.vitam.common.client.VitamRequestBuilder;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;

class SchedulerClientRest extends DefaultClient implements SchedulerClient {
    /**
     * Constructor using given scheme (http)
     *
     * @param factory The client factory
     */
    SchedulerClientRest(VitamClientFactoryInterface<?> factory) {
        super(factory);
    }

    @Override
    public RequestResponse<JsonNode> findCurrentJobs() throws VitamClientException {
        try (Response response = make(VitamRequestBuilder.get().withPath("/current-jobs").withJsonAccept())) {
            check(response);
            return RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> findJobs() throws VitamClientException {
        try (Response response = make(VitamRequestBuilder.get().withPath("/jobs").withJsonAccept())) {
            check(response);
            return RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponseOK<JsonNode> jobState(String jobName) throws VitamClientException {
        try (Response response = make(
            VitamRequestBuilder.get().withPath("/job-state/" + jobName).withJsonContentType())) {
            check(response);
            return (RequestResponseOK<JsonNode>) RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> scheduleJob(byte[] job) throws VitamClientException {
        try (Response response = make(
            VitamRequestBuilder.post().withPath("/schedule-job").withBody(job).withJson())) {
            check(response);
            return RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> triggerJob(String jobName) throws VitamClientException {
        try (Response response = make(
            VitamRequestBuilder.post().withPath("/trigger-job/" + jobName).withJson())) {
            check(response);
            return RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> triggerJob(String jobName, JsonNode jobDataMap) throws VitamClientException {
        try (Response response = make(
            VitamRequestBuilder.post().withPath("/trigger-job/" + jobName).withBody(jobDataMap).withJson())) {
            check(response);
            return RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    @Override
    public RequestResponse<JsonNode> triggerJob(byte[] trigger) throws VitamClientException {
        try (Response response = make(
            VitamRequestBuilder.post().withPath("/trigger-job").withBody(trigger).withJson())) {
            check(response);
            return RequestResponseOK.parseFromResponse(response);
        } catch (VitamClientInternalException e) {
            throw new VitamClientException(e);
        }
    }

    private void check(Response response) throws VitamClientInternalException {
        Response.Status status = response.getStatusInfo().toEnum();
        if (SUCCESSFUL.equals(status.getFamily()) || REDIRECTION.equals(status.getFamily())) {
            return;
        }

        throw new VitamClientInternalException(String
            .format("Error with the response, get status: '%d' and reason '%s'.", response.getStatus(),
                fromStatusCode(response.getStatus()).getReasonPhrase()));
    }
}
