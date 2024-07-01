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
package fr.gouv.vitam.functionaltest.cucumber.service;

import com.google.common.collect.Iterables;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import org.apache.commons.lang3.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Logbook service containing common code for logbook
 */
public class LogbookService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookService.class);

    /**
     * Get a Logbook operation by its id
     *
     * @param accessClient access client
     * @param tenantId tenant id
     * @param contractId access contract id
     * @param applicationSessionId application session id
     * @param operationId logbook operation id
     * @return RequestResponse
     * @throws VitamClientException exception
     */
    public RequestResponse<LogbookOperation> getLogbookOperation(
        AccessExternalClient accessClient,
        int tenantId,
        String contractId,
        String applicationSessionId,
        String operationId
    ) throws VitamClientException {
        return accessClient.selectOperationbyId(
            new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(applicationSessionId),
            operationId,
            new Select().getFinalSelectById()
        );
    }

    /**
     * check on logbook if the global status is OK (status of the last event, if last event is correct)
     *
     * @param accessClient access client
     * @param tenantId tenant id
     * @param contractId access contract id
     * @param applicationSessionId application session id
     * @param operationId logbook operation id
     * @param status expected status
     * @throws VitamClientException exception
     */
    public LogbookEventOperation checkFinalStatusLogbook(
        AccessExternalClient accessClient,
        int tenantId,
        String contractId,
        String applicationSessionId,
        String operationId,
        String status
    ) throws VitamClientException {
        RequestResponse<LogbookOperation> requestResponse = getLogbookOperation(
            accessClient,
            tenantId,
            contractId,
            applicationSessionId,
            operationId
        );

        if (!(requestResponse instanceof RequestResponseOK)) {
            LOGGER.error(String.format("logbook operation return a vitam error for operationId: %s", operationId));
            fail(String.format("logbook operation return a vitam error for operationId: %s", operationId));
        }

        RequestResponseOK<LogbookOperation> requestResponseOK = (RequestResponseOK<LogbookOperation>) requestResponse;

        LogbookOperation actual = requestResponseOK.getFirstResult();
        LogbookEventOperation last = Iterables.getLast(actual.getEvents());

        if (!StringUtils.equals("FATAL", status)) {
            assertThat(last.getEvType())
                .as("last event is type %s, but %s was expected.", last.getEvType(), actual.getEvType())
                .isEqualTo(actual.getEvType());
        }

        if (!status.contains("|")) {
            assertThat(last.getOutcome())
                .as(
                    "last event has status %s, but %s was expected. Event name is: %s",
                    last.getOutcome(),
                    status,
                    last.getEvType()
                )
                .isEqualTo(status);
            return last;
        }
        String[] statuses = status.split("\\|");
        boolean atLeastOneOK = false;
        for (String currStatus : statuses) {
            atLeastOneOK = last.getOutcome().equals(currStatus);
            if (atLeastOneOK) {
                break;
            }
        }
        assertThat(atLeastOneOK)
            .as(
                "last event has status %s, but %s was expected. Event name is: %s",
                last.getOutcome(),
                status,
                last.getEvType()
            )
            .isEqualTo(true);

        return last;
    }
}
