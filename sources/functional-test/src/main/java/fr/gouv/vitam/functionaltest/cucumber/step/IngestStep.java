/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.cucumber.step;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.FILING_SCHEME;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.HOLDING_SCHEME;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.Fail;

import com.google.common.collect.Iterables;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.tools.SipTool;

public class IngestStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestStep.class);

    private Path sip;

    private World world;
    private static boolean deleteSip = false;
    private static boolean attachMode = false;
    private static String OUTCOME_DETAIL = "OutcomeDetail";

    public IngestStep(World world) {
        this.world = world;
    }

    /**
     * define a sip
     *
     * @param fileName name of a sip
     */
    @Given("^un fichier SIP nommé (.*)$")
    public void a_sip_named(String fileName) {
        this.sip = Paths.get(world.getBaseDirectory(), fileName);
    }

    /**
     * call vitam to upload the SIP
     *
     * @throws IOException
     * @throws IngestExternalException
     */
    @When("^je télécharge le SIP")
    public void upload_this_sip() throws IOException, VitamException, IOException {
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            RequestResponse response = world.getIngestClient()
                .upload(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream, DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name());
            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            world.setOperationId(operationId);
            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
            boolean process_timeout = vitamPoolingClient
                .wait(world.getTenantId(), operationId, ProcessState.COMPLETED, 1800, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                fail("Sip processing not finished. Timeout exeedeed.");
            }
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        }

    }

    /**
     * call vitam to upload the plan
     * 
     * @throws IOException
     * @throws IngestExternalException
     */
    @When("^je télécharge le plan")
    public void upload_this_plan() throws IOException, VitamException {
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {

            RequestResponse<Void> response = world.getIngestClient()
                .upload(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream, FILING_SCHEME.name(), ProcessAction.RESUME.name());

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            world.setOperationId(operationId);
            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
            boolean process_timeout = vitamPoolingClient
                .wait(world.getTenantId(), operationId, ProcessState.COMPLETED, 200, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                fail("Sip processing not finished. Timeout exeedeed.");
            }
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        }
        if (attachMode) {
            deleteSip = true;
        }
    }

    /**
     * call vitam to upload the tree
     *
     * @throws IOException
     * @throws IngestExternalException
     */
    @When("^je télécharge l'arbre")
    public void upload_this_tree() throws IOException, VitamException {
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            RequestResponse response = world.getIngestClient()
                .upload(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream, HOLDING_SCHEME.name(), ProcessAction.RESUME.name());

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            world.setOperationId(operationId);
            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
            boolean process_timeout = vitamPoolingClient
                .wait(world.getTenantId(), operationId, ProcessState.COMPLETED, 100, 1_000L, TimeUnit.MILLISECONDS);
            if (!process_timeout) {
                fail("Sip processing not finished. Timeout exeedeed.");
            }
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        }
        if (attachMode) {
            deleteSip = true;
        }
    }

    /**
     * check on logbook if the global status is OK (status of the last event)
     *
     * @param status
     * @throws VitamClientException
     * @throws InvalidParseOperationException
     */
    @Then("^le statut final du journal des opérations est (.*)$")
    public void the_logbook_operation_has_a_status(String status)
        throws VitamClientException, InvalidParseOperationException {
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient()
                .selectOperationbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                    world.getOperationId(), new Select().getFinalSelect());
        if (requestResponse instanceof RequestResponseOK) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;

            LogbookOperation actual = requestResponseOK.getFirstResult();
            LogbookEventOperation last = Iterables.getLast(actual.getEvents());
            assertThat(last.getOutcome()).as("last event has status %s, but %s was expected. Event name is: %s",
                last.getOutcome(), status, last.getEvType()).isEqualTo(status);
        } else {
            LOGGER.error(
                String.format("logbook operation return a vitam error for operationId: %s", world.getOperationId()));

            fail(String.format("logbook operation return a vitam error for operationId: %s", world.getOperationId()));
        }
    }

    /**
     * check if the status is valid for a list of event type according to logbook operation
     *
     * @param eventNames list of event
     * @param eventStatus status of event
     * @throws VitamClientException
     * @throws InvalidParseOperationException
     */
    @Then("^le[s]? statut[s]? (?:de l'événement|des événements) (.*) (?:est|sont) (.*)$")
    public void the_status_are(List<String> eventNames, String eventStatus)
        throws VitamClientException, InvalidParseOperationException {
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient()
                .selectOperationbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                    world.getOperationId(), new Select().getFinalSelect());

        if (requestResponse.isOk()) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;

            List<LogbookEventOperation> actual = requestResponseOK.getFirstResult().getEvents();
            try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
                for (String eventName : eventNames) {
                    List<LogbookEventOperation> events =
                        actual.stream().filter(event -> eventName.equals(event.getEvType()))
                            .filter(event -> !"STARTED".equals(event.getOutcome()))
                            .collect(Collectors.toList());

                    softly.assertThat(events).as("event %s is not present or finish.", eventName).hasSize(1);
                    LogbookEventOperation onlyElement = Iterables.getOnlyElement(events);

                    String currentStatus = onlyElement.getOutcome();
                    softly.assertThat(currentStatus)
                        .as("event %s has status %s but excepted status is %s.", eventName, currentStatus, eventStatus)
                        .isEqualTo(eventStatus);
                }
            }
        } else {
            VitamError error = (VitamError) requestResponse;
            LOGGER.error(String.format("logbook operation return a vitam error for operationId: %s, requestId is %s",
                world.getOperationId(), error.getCode()));
            Fail.fail("cannot find logbook with id: " + world.getOperationId());
        }
    }
    
    @Then("^l'outcome détail de l'événement (.*) est (.*)$")
    public void the_outcome_detail_is(String eventName, String eventOutDetail) throws Throwable {
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient()
                .selectOperationbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                    world.getOperationId(), new Select().getFinalSelect());

        if (requestResponse.isOk()) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;

            List<LogbookEventOperation> actual = requestResponseOK.getFirstResult().getEvents();
            
            try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
                List<LogbookEventOperation> events =
                    actual.stream().filter(event -> eventName.equals(event.getEvType()))
                        .filter(event -> !"STARTED".equals(event.getOutcome()))
                        .collect(Collectors.toList());
                
                softly.assertThat(events).as("event %s is not present or finish.", eventName).hasSize(1);
                LogbookEventOperation onlyElement = Iterables.getOnlyElement(events);
                
                String currentOutDetail = onlyElement.getOutDetail();
                softly.assertThat(currentOutDetail)
                    .as("event %s has status %s but excepted status is %s.", eventName, currentOutDetail, eventOutDetail)
                    .isEqualTo(eventOutDetail);
            }
        }
    }


    /**
     * check if the outcome detail is valid for an event type according to logbook operation
     *
     * @param eventName the event
     * @param eventResults otucome detail of the event
     * @throws VitamClientException
     * @throws InvalidParseOperationException
     */
    @Then("^le résultat de l'événement (.*) est (.*)$")
    public void the_results_are(String eventName, String eventResults)
        throws VitamClientException, InvalidParseOperationException {
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient()
                .selectOperationbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                    world.getOperationId(), new Select().getFinalSelect());

        if (requestResponse.isOk()) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;

            List<LogbookEventOperation> actual = requestResponseOK.getFirstResult().getEvents();
            try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
                List<LogbookEventOperation> events =
                    actual.stream().filter(event -> eventName.equals(event.getEvType()))
                        .filter(event -> !"STARTED".equals(event.getOutcome()))
                        .collect(Collectors.toList());

                softly.assertThat(events).as("event %s is not present or finish.", eventName).hasSize(1);
                LogbookEventOperation onlyElement = Iterables.getOnlyElement(events);

                String currentResult = onlyElement.getOutDetail();
                softly.assertThat(currentResult)
                    .as("event %s has outcome detail %s but excepted outcome detail is %s.", eventName, currentResult,
                        eventResults)
                    .isEqualTo(eventResults);
            }
        } else {
            VitamError error = (VitamError) requestResponse;
            LOGGER.error(String.format("logbook operation return a vitam error for operationId: %s, requestId is %s",
                world.getOperationId(), error.getCode()));
            Fail.fail("cannot find logbook with id: " + world.getOperationId());
        }
    }


    @When("je construit le sip de rattachement avec le template")
    public void build_the_attachenment() throws IOException {
        this.sip = SipTool.copyAndModifyManifestInZip(sip, SipTool.REPLACEMENT_STRING, world.getUnitId());
        attachMode = true;
    }

    /**
     * check if the atr is available
     * 
     * @throws VitamClientException
     */
    @Then("je peux télécharger son ATR")
    public void download_atr()
        throws VitamClientException {
        Response response = world.getIngestClient()
            .downloadObjectAsync(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                world.getOperationId(), IngestCollection.ARCHIVETRANSFERREPLY);
        InputStream inputStream = response.readEntity(InputStream.class);
        assertThat(inputStream).isNotNull();
        StreamUtils.closeSilently(inputStream);
        world.getIngestClient().consumeAnyEntityAndClose(response);
    }
    
    /**
     * check if the atr contains the outcome detail
     * 
     * @param message
     * @throws VitamClientException
     * @throws XMLStreamException
     * @throws IOException 
     */
    @Then("^fichier ATR contient (.*)$")
    public void check_atr(String message) throws VitamClientException, XMLStreamException, IOException {
        Response response = world.getIngestClient()
            .downloadObjectAsync(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                world.getOperationId(), IngestCollection.ARCHIVETRANSFERREPLY);
        InputStream inputStream = response.readEntity(InputStream.class);
        String result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        assertThat(result).contains(message);        
    }

    @After
    public void afterScenario() throws IOException {

        if (this.sip != null && deleteSip) {
            Files.delete(this.sip);
            deleteSip = false;
            attachMode = false;
        }
    }
}
