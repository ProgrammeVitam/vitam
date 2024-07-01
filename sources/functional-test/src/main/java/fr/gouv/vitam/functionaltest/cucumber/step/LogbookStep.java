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
package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEvent;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookLifecycle;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.assertj.core.api.Fail;
import org.assertj.core.api.SoftAssertionError;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * step defining logbook behaviors
 */
public class LogbookStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookStep.class);

    private boolean isRequestResponseLifecycle;
    private RequestResponse requestResponse;

    public LogbookStep(World world) {
        super(world);
    }

    @When("^je recherche le journal des opérations")
    public void download_logbook_operation() {
        try {
            requestResponse = world
                .getLogbookService()
                .getLogbookOperation(
                    world.getAccessClient(),
                    world.getTenantId(),
                    world.getContractId(),
                    world.getApplicationSessionId(),
                    world.getOperationId()
                );
            isRequestResponseLifecycle = false;
        } catch (VitamClientException e) {
            LOGGER.error(
                String.format("logbook operation could not be retrieved for operationId: %s", world.getOperationId())
            );
            fail(String.format("logbook operation could not be retrieved for operationId: %s", world.getOperationId()));
        }
    }

    @When("^je recherche le JCV de l'unité archivistique dont le titre est (.*)")
    public void download_logbook_lifecycle_unit(String unitTitle) {
        try {
            String unitId = world
                .getAccessService()
                .findUnitGUIDByTitleAndOperationId(
                    world.getAccessClient(),
                    world.getTenantId(),
                    world.getContractId(),
                    world.getApplicationSessionId(),
                    world.getOperationId(),
                    unitTitle
                );
            requestResponse = world
                .getAccessClient()
                .selectUnitLifeCycleById(
                    new VitamContext(world.getTenantId())
                        .setAccessContract(world.getContractId())
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    unitId,
                    new Select().getFinalSelectById()
                );
            isRequestResponseLifecycle = true;
        } catch (VitamClientException | InvalidCreateOperationException e) {
            LOGGER.error(
                String.format(
                    "logbook lifecycle unit could not be retrieved for operationId and unit title: %s",
                    world.getOperationId(),
                    unitTitle
                )
            );
            fail(
                String.format(
                    "logbook lifecycle unit could not be retrieved for operationId and unit title: %s",
                    world.getOperationId(),
                    unitTitle
                )
            );
        }
    }

    @When("^je recherche le JCV du groupe d'objet de l'unité archivistique dont le titre est (.*)")
    public void download_logbook_lifecycle_object_for_unit(String unitTitle) {
        try {
            String unitId = world
                .getAccessService()
                .findUnitGUIDByTitleAndOperationId(
                    world.getAccessClient(),
                    world.getTenantId(),
                    world.getContractId(),
                    world.getApplicationSessionId(),
                    world.getOperationId(),
                    unitTitle
                );
            RequestResponse<JsonNode> requestResponseUnit = world
                .getAccessClient()
                .selectUnitbyId(
                    new VitamContext(world.getTenantId())
                        .setAccessContract(world.getContractId())
                        .setApplicationSessionId(world.getApplicationSessionId()),
                    new SelectMultiQuery().getFinalSelectById(),
                    unitId
                );
            if (requestResponseUnit.isOk()) {
                RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponseUnit;
                JsonNode unit = requestResponseOK.getResults().get(0);
                if (unit.get(PROJECTIONARGS.OBJECT.exactToken()).asText().isEmpty()) {
                    throw new VitamClientException("Unit does not have object");
                }
                requestResponse = world
                    .getAccessClient()
                    .selectObjectGroupLifeCycleById(
                        new VitamContext(world.getTenantId())
                            .setAccessContract(world.getContractId())
                            .setApplicationSessionId(world.getApplicationSessionId()),
                        unit.get(PROJECTIONARGS.OBJECT.exactToken()).asText(),
                        new Select().getFinalSelectById()
                    );
                isRequestResponseLifecycle = true;
            }
        } catch (VitamClientException | InvalidCreateOperationException e) {
            LOGGER.error(
                String.format(
                    "logbook lifecycle object group could not be retrieved for operationId and unit title: %s",
                    world.getOperationId(),
                    unitTitle
                )
            );
            fail(
                String.format(
                    "logbook lifecycle object group could not be retrieved for operationId and unit title: %s",
                    world.getOperationId(),
                    unitTitle
                )
            );
        }
    }

    /**
     * check on logbook if the global status is OK (status of the last event, if last event is correct)
     *
     * @param status expected status
     * @throws VitamClientException VitamClientException
     */
    @Then("^le statut final du journal des opérations est (.*)$")
    public void the_logbook_operation_has_a_status(String status) throws VitamClientException {
        LogbookEventOperation lastEvent = world
            .getLogbookService()
            .checkFinalStatusLogbook(
                world.getAccessClient(),
                world.getTenantId(),
                world.getContractId(),
                world.getApplicationSessionId(),
                world.getOperationId(),
                status
            );
        world.setLogbookEvent(lastEvent);
    }

    @Then("^le champ '(.*)' de l'évenement final est : (.*)$")
    public void the_final_logbook_event_has_message(String field, String message)
        throws VitamClientException, InvalidParseOperationException {
        LogbookEvent logbookEvent = world.getLogbookEvent();
        assertThat(logbookEvent).isNotNull();
        JsonNode event = JsonHandler.toJsonNode(logbookEvent);
        assertThat(event.get(field).textValue()).contains(message);
    }

    /**
     * Check logbook operation consistency : no double evId, same evProcType, evId = evProcId for master, max status
     * level is in the last event
     *
     * @throws VitamClientException
     */
    @Then("^le journal des opérations est cohérent$")
    public void the_logbook_operation_is_consistent() throws VitamClientException {
        if (requestResponse instanceof RequestResponseOK) {
            RequestResponseOK<LogbookOperation> requestResponseOK = (RequestResponseOK<
                    LogbookOperation
                >) requestResponse;
            LogbookOperation master = requestResponseOK.getFirstResult();
            LogbookEventOperation lastEvent = Iterables.getLast(master.getEvents());

            StatusCode maxStatus = StatusCode.valueOf(master.getOutcome());
            StatusCode finalStatus = StatusCode.valueOf(lastEvent.getOutcome());
            String masterEvId = master.getEvId();
            String masterEvTypeProc = master.getEvTypeProc();
            Set<String> evIds = new HashSet<>();
            evIds.add(masterEvId);

            for (LogbookEventOperation event : master.getEvents()) {
                // evIdProc of the event should be evId of THE MASTER
                assertThat(event.getEvIdProc())
                    .as(
                        "event has evIdProc value %s, but %s was expected. Event name is: %s",
                        event.getEvIdProc(),
                        masterEvId,
                        event.getEvType()
                    )
                    .isEqualTo(masterEvId);
                // evTypeProc of event should be the same as master
                assertThat(event.getEvTypeProc())
                    .as(
                        "event has evTypeProc value %s, but %s was expected. Event name is: %s",
                        event.getEvTypeProc(),
                        masterEvTypeProc,
                        event.getEvType()
                    )
                    .isEqualTo(masterEvTypeProc);

                evIds.add(event.getEvId());
                StatusCode status = StatusCode.valueOf(event.getOutcome());
                if (status.getStatusLevel() > maxStatus.getStatusLevel()) {
                    maxStatus = status;
                }
            }
            // evIds should be unique between events
            assertThat(evIds.size())
                .as("all evId values should be differents but where not")
                .isEqualTo(master.getEvents().size() + 1);

            // final status should be maximal status of events
            assertThat(finalStatus)
                .as("the final outcome is %s, but an outcome %s was seen in an event", finalStatus, maxStatus)
                .isGreaterThanOrEqualTo(maxStatus);
        } else {
            LOGGER.error(
                String.format("logbook operation return a vitam error for operationId: %s", world.getOperationId())
            );
            fail(String.format("logbook operation return a vitam error for operationId: %s", world.getOperationId()));
        }
    }

    /**
     * Check if logbook does not contains given status
     *
     * @param status forbidden status
     * @throws VitamClientException VitamClientException
     */
    @Then("^le journal ne contient pas de statut (.*)$")
    public void the_logbook_has_not_the_status(String status) throws VitamClientException {
        if (requestResponse instanceof RequestResponseOK) {
            if (isRequestResponseLifecycle) {
                the_logbook_lifecycle_has_not_the_status(status);
            } else {
                the_logbook_operation_has_not_the_status(status);
            }
        }
    }

    @Then("^l'outcome détail de l'événement (.*) est (.*)$")
    public void the_outcome_detail_is(String eventName, String eventOutDetail) throws Throwable {
        if (requestResponse.isOk()) {
            if (isRequestResponseLifecycle) {
                the_logbook_lifecycle_outcome_detail_is(eventName, eventOutDetail);
            } else {
                the_logbook_operation_outcome_detail_is(eventName, eventOutDetail);
            }
        }
    }

    /**
     * check if the status is valid for a list of event type according to logbook
     *
     * @param eventNames list of event
     * @param eventStatus status of event
     * @throws VitamClientException
     * @throws InvalidParseOperationException
     */
    @Then("^le[s]? statut[s]? (?:de l'événement|des événements) (.*) (?:est|sont) (.*)$")
    public void the_status_are(List<String> eventNames, String eventStatus)
        throws VitamClientException, InvalidParseOperationException {
        if (requestResponse.isOk()) {
            if (isRequestResponseLifecycle) {
                the_logbook_lifecycle_status_are(eventNames, eventStatus);
            } else {
                the_logbook_operation_status_are(eventNames, eventStatus);
            }
        } else {
            VitamError error = (VitamError) requestResponse;
            LOGGER.error(
                String.format(
                    "logbook operation return a vitam error for operationId: %s, requestId is %s",
                    world.getOperationId(),
                    error.getCode()
                )
            );
            Fail.fail("cannot find logbook with id: " + world.getOperationId());
        }
    }

    /**
     * check if the outcome detail is valid for an event type according to logbook
     *
     * @param eventName the event
     * @param eventResults outcome detail of the event
     * @throws VitamClientException
     * @throws InvalidParseOperationException
     */
    @Then("^le résultat de l'événement (.*) est (.*)$")
    public void the_results_are(String eventName, String eventResults)
        throws VitamClientException, InvalidParseOperationException {
        if (requestResponse.isOk()) {
            if (isRequestResponseLifecycle) {
                the_logbook_lifecycle_results_are(eventName, eventResults);
            } else {
                the_logbook_operation_results_are(eventName, eventResults);
            }
        } else {
            VitamError error = (VitamError) requestResponse;
            LOGGER.error(
                String.format(
                    "logbook operation return a vitam error for operationId: %s, requestId is %s",
                    world.getOperationId(),
                    error.getCode()
                )
            );
            Fail.fail("cannot find logbook with id: " + world.getOperationId());
        }
    }

    private void the_logbook_operation_results_are(String eventName, String eventResults) throws SoftAssertionError {
        RequestResponseOK<LogbookOperation> requestResponseOK = (RequestResponseOK<LogbookOperation>) requestResponse;

        List<LogbookEventOperation> actual = requestResponseOK.getFirstResult().getEvents();
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            List<LogbookEvent> events = actual
                .stream()
                .filter(event -> eventName.equals(event.getEvType()))
                .filter(event -> !"STARTED".equals(event.getOutcome()))
                .collect(Collectors.toList());

            the_logbook_event_result_check(eventName, eventResults, softly, events);
        }
    }

    private void the_logbook_lifecycle_results_are(String eventName, String eventResults) throws SoftAssertionError {
        RequestResponseOK<LogbookLifecycle> requestResponseOK = (RequestResponseOK<LogbookLifecycle>) requestResponse;

        List<LogbookEvent> actual = requestResponseOK.getFirstResult().getEvents();
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            List<LogbookEvent> events = actual
                .stream()
                .filter(event -> eventName.equals(event.getEvType()))
                .filter(event -> !"STARTED".equals(event.getOutcome()))
                .collect(Collectors.toList());

            the_logbook_event_result_check(eventName, eventResults, softly, events);
        }
    }

    private void the_logbook_event_result_check(
        String eventName,
        String eventResults,
        AutoCloseableSoftAssertions softly,
        List<LogbookEvent> events
    ) {
        softly.assertThat(events).as("event %s is not present or finish.", eventName).hasSize(1);
        LogbookEvent onlyElement = Iterables.getOnlyElement(events);

        String currentResult = onlyElement.getOutDetail();
        softly
            .assertThat(currentResult)
            .as(
                "event %s has outcome detail %s but excepted outcome detail is %s.",
                eventName,
                currentResult,
                eventResults
            )
            .isEqualTo(eventResults);
    }

    private void the_logbook_operation_has_not_the_status(String status) {
        RequestResponseOK<LogbookOperation> requestResponseOK = (RequestResponseOK<LogbookOperation>) requestResponse;
        LogbookOperation master = requestResponseOK.getFirstResult();
        assertThat(master.getOutcome())
            .as("master has forbidden status %s", master.getOutcome(), master.getEvType())
            .isNotEqualTo(status);
        for (LogbookEventOperation event : master.getEvents()) {
            assertThat(event.getOutcome())
                .as("event has forbidden status %s. Event name is: %s", event.getOutcome(), event.getEvType())
                .isNotEqualTo(status);
        }
    }

    private void the_logbook_lifecycle_has_not_the_status(String status) {
        RequestResponseOK<LogbookLifecycle> requestResponseOK = (RequestResponseOK<LogbookLifecycle>) requestResponse;
        LogbookLifecycle master = requestResponseOK.getFirstResult();
        assertThat(master.getOutcome())
            .as("master has forbidden status %s", master.getOutcome(), master.getEvType())
            .isNotEqualTo(status);
        for (LogbookEvent event : master.getEvents()) {
            assertThat(event.getOutcome())
                .as("event has forbidden status %s. Event name is: %s", event.getOutcome(), event.getEvType())
                .isNotEqualTo(status);
        }
    }

    private void the_logbook_operation_status_are(List<String> eventNames, String eventStatus)
        throws SoftAssertionError {
        RequestResponseOK<LogbookOperation> requestResponseOK = (RequestResponseOK<LogbookOperation>) requestResponse;

        List<LogbookEventOperation> actual = requestResponseOK.getFirstResult().getEvents();
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            for (String eventName : eventNames) {
                List<LogbookEvent> events = actual
                    .stream()
                    .filter(event -> eventName.equals(event.getEvType()))
                    .filter(event -> !"STARTED".equals(event.getOutcome()))
                    .collect(Collectors.toList());

                the_logbook_event_status_check(eventStatus, softly, eventName, events);
            }
        }
    }

    private void the_logbook_lifecycle_status_are(List<String> eventNames, String eventStatus)
        throws SoftAssertionError {
        RequestResponseOK<LogbookLifecycle> requestResponseOK = (RequestResponseOK<LogbookLifecycle>) requestResponse;

        List<LogbookEvent> actual = requestResponseOK.getFirstResult().getEvents();
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            for (String eventName : eventNames) {
                List<LogbookEvent> events = actual
                    .stream()
                    .filter(event -> eventName.equals(event.getEvType()))
                    .filter(event -> !"STARTED".equals(event.getOutcome()))
                    .collect(Collectors.toList());

                the_logbook_event_status_check(eventStatus, softly, eventName, events);
            }
        }
    }

    private void the_logbook_event_status_check(
        String eventStatus,
        AutoCloseableSoftAssertions softly,
        String eventName,
        List<LogbookEvent> events
    ) {
        softly.assertThat(events).as("event %s is not present or finish.", eventName).hasSize(1);
        LogbookEvent onlyElement = Iterables.getOnlyElement(events);

        String currentStatus = onlyElement.getOutcome();
        softly
            .assertThat(currentStatus)
            .as("event %s has status %s but excepted status is %s.", eventName, currentStatus, eventStatus)
            .isEqualTo(eventStatus);
    }

    private void the_logbook_operation_outcome_detail_is(String eventName, String eventOutDetail)
        throws SoftAssertionError {
        RequestResponseOK<LogbookOperation> requestResponseOK = (RequestResponseOK<LogbookOperation>) requestResponse;

        List<LogbookEventOperation> actual = requestResponseOK.getFirstResult().getEvents();

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            List<LogbookEvent> events = actual
                .stream()
                .filter(event -> eventName.equals(event.getEvType()))
                .filter(event -> !"STARTED".equals(event.getOutcome()))
                .collect(Collectors.toList());

            the_logbook_event_outcome_detail_check(eventName, eventOutDetail, softly, events);
        }
    }

    private void the_logbook_lifecycle_outcome_detail_is(String eventName, String eventOutDetail)
        throws SoftAssertionError {
        RequestResponseOK<LogbookLifecycle> requestResponseOK = (RequestResponseOK<LogbookLifecycle>) requestResponse;

        List<LogbookEvent> actual = requestResponseOK.getFirstResult().getEvents();

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            List<LogbookEvent> events = actual
                .stream()
                .filter(event -> eventName.equals(event.getEvType()))
                .filter(event -> !"STARTED".equals(event.getOutcome()))
                .collect(Collectors.toList());

            the_logbook_event_outcome_detail_check(eventName, eventOutDetail, softly, events);
        }
    }

    private void the_logbook_event_outcome_detail_check(
        String eventName,
        String eventOutDetail,
        AutoCloseableSoftAssertions softly,
        List<LogbookEvent> events
    ) {
        softly.assertThat(events).as("event %s is not present or finish.", eventName).hasSize(1);
        LogbookEvent onlyElement = Iterables.getOnlyElement(events);

        String currentOutDetail = onlyElement.getOutDetail();
        softly
            .assertThat(currentOutDetail)
            .as("event %s has status %s but excepted status is %s.", eventName, currentOutDetail, eventOutDetail)
            .isEqualTo(eventOutDetail);
    }
}
