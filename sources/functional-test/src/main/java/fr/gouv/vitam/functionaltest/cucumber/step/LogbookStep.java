package fr.gouv.vitam.functionaltest.cucumber.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Iterables;

import cucumber.api.java.en.Then;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;

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
public class LogbookStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookStep.class);

    private World world;

    public LogbookStep(World world) {
        this.world = world;
    }

    /**
     * check on logbook if the global status is OK (status of the last event, if last event is correct)
     *
     * @param status expected status
     * @throws VitamClientException VitamClientException
     */
    @Then("^le statut final du journal des opérations est (.*)$")
    public void the_logbook_operation_has_a_status(String status)
        throws VitamClientException {
        world.getLogbookService().checkFinalStatusLogbook(world.getAccessClient(), world.getTenantId(),
            world.getContractId(), world.getApplicationSessionId(), world.getOperationId(), status);
    }

    /**
     * Check if logbook does not contains given status
     * 
     * @param status forbidden status
     * @throws VitamClientException VitamClientException
     */
    @Then("^le journal des opérations ne contient pas de statut (.*)$")
    public void the_logbook_operation_has_not_the_status(String status)
        throws VitamClientException {
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient()
                .selectOperationbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                    world.getOperationId(), new Select().getFinalSelectById());
        if (requestResponse instanceof RequestResponseOK) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;
            LogbookOperation master = requestResponseOK.getFirstResult();
            assertThat(master.getOutcome()).as("master has forbidden status %s",
                master.getOutcome(), master.getEvType()).isNotEqualTo(status);
            for (LogbookEventOperation event : master.getEvents()) {
                assertThat(event.getOutcome()).as("event has forbidden status %s. Event name is: %s",
                    event.getOutcome(), event.getEvType()).isNotEqualTo(status);
            }
        }
    }

    /**
     * Check logbook operation consistency : no double evId, same evProcType, evId = evProcId for master, max status
     * level is in the last event
     * 
     * @throws VitamClientException
     */
    @Then("^le journal des opérations est cohérent$")
    public void the_logbook_operation_is_consistent()
        throws VitamClientException {
        RequestResponse<LogbookOperation> requestResponse =
            world.getAccessClient()
                .selectOperationbyId(new VitamContext(world.getTenantId()).setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                    world.getOperationId(), new Select().getFinalSelectById());

        if (requestResponse instanceof RequestResponseOK) {
            RequestResponseOK<LogbookOperation> requestResponseOK =
                (RequestResponseOK<LogbookOperation>) requestResponse;
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
                    .as("event has evIdProc value %s, but %s was expected. Event name is: %s", event.getEvIdProc(),
                        masterEvId, event.getEvType())
                    .isEqualTo(masterEvId);
                // evTypeProc of event should be the same as master
                assertThat(event.getEvTypeProc())
                    .as("event has evTypeProc value %s, but %s was expected. Event name is: %s",
                        event.getEvTypeProc(), masterEvTypeProc, event.getEvType())
                    .isEqualTo(masterEvTypeProc);

                evIds.add(event.getEvId());
                StatusCode status = StatusCode.valueOf(event.getOutcome());
                if (status.getStatusLevel() > maxStatus.getStatusLevel()) {
                    maxStatus = status;
                }
            }
            // evIds should be unique between events
            assertThat(evIds.size()).as("all evId values should be differents but where not")
                .isEqualTo(master.getEvents().size() + 1);

            // final status should be maximal status of events
            assertThat(finalStatus)
                .as("the final outcome is %s, but an outcome %s was seen in an event", finalStatus, maxStatus)
                .isGreaterThanOrEqualTo(maxStatus);

        } else {
            LOGGER.error(
                String.format("logbook operation return a vitam error for operationId: %s", world.getOperationId()));
            fail(String.format("logbook operation return a vitam error for operationId: %s", world.getOperationId()));
        }

    }
}
