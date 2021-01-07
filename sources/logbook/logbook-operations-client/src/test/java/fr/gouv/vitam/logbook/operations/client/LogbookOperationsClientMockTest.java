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
package fr.gouv.vitam.logbook.operations.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import fr.gouv.vitam.logbook.common.model.TenantLogbookOperationTraceabilityResult;
import org.junit.Test;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.model.AuditLogbookOptions;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;

/**
 * Test for logbook operation client
 */
public class LogbookOperationsClientMockTest {
    private static final String request = "{ $query: {} }, $projection: {}, $filter: {} }";

    @Test
    public void createTest() {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        assertNotNull(client);

        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters();
        assertNotNull(logbookParameters);

        final Set<LogbookParameterName> mandatory = logbookParameters.getMandatoriesParameters();
        assertNotNull(mandatory);

        boolean catchException = false;
        try {
            mandatory.add(LogbookParameterName.objectIdentifier);
        } catch (final UnsupportedOperationException uoe) {
            catchException = true;
        }
        assertTrue(catchException);

        fillLogbookParamaters(logbookParameters);

        catchException = false;
        try {
            client.create(logbookParameters);
        } catch (final LogbookClientException lce) {
            catchException = true;
        }
        assertFalse(catchException);
    }

    @Test
    public void updateTest() {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        assertNotNull(client);

        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters();
        assertNotNull(logbookParameters);

        final Set<LogbookParameterName> mandatory = logbookParameters.getMandatoriesParameters();

        boolean catchException = false;
        try {
            mandatory.add(LogbookParameterName.objectIdentifier);
        } catch (final UnsupportedOperationException uoe) {
            catchException = true;
        }
        assertTrue(catchException);
        assertNotNull(logbookParameters);

        fillLogbookParamaters(logbookParameters);

        catchException = false;
        try {
            client.update(logbookParameters);
        } catch (final LogbookClientException lce) {
            catchException = true;
        }
        assertFalse(catchException);
    }

    @Test
    public void statusTest() throws LogbookClientException, VitamApplicationServerException {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        assertNotNull(client);
        client.checkStatus();
    }

    private void fillLogbookParamaters(LogbookParameters logbookParamaters) {
        logbookParamaters.setTypeProcess(LogbookTypeProcess.INGEST);
        logbookParamaters.putParameterValue(LogbookParameterName.eventIdentifier,
            LogbookParameterName.eventIdentifier.name());
        logbookParamaters
            .putParameterValue(LogbookParameterName.eventType, LogbookParameterName.eventType.name());
        logbookParamaters.putParameterValue(LogbookParameterName.eventDateTime,
            LocalDateUtil.now().toString());
        logbookParamaters.putParameterValue(LogbookParameterName.eventIdentifierProcess,
            LogbookParameterName.eventIdentifierProcess.name());
        logbookParamaters.putParameterValue(LogbookParameterName.outcome, LogbookParameterName.outcome.name());
        logbookParamaters
            .putParameterValue(LogbookParameterName.outcomeDetail, LogbookParameterName.outcomeDetail.name());
        logbookParamaters.putParameterValue(LogbookParameterName.outcomeDetailMessage,
            LogbookParameterName.outcomeDetailMessage.name());
        logbookParamaters.putParameterValue(LogbookParameterName.agentIdentifier,
            LogbookParameterName.agentIdentifier.name());
        logbookParamaters.putParameterValue(LogbookParameterName.agentIdentifierApplicationSession,
            LogbookParameterName.agentIdentifierApplicationSession.name());
        logbookParamaters.putParameterValue(LogbookParameterName.eventIdentifierRequest,
            LogbookParameterName.eventIdentifierRequest.name());
        logbookParamaters.putParameterValue(LogbookParameterName.agIdExt,
            LogbookParameterName.agIdExt.name());

        logbookParamaters.putParameterValue(LogbookParameterName.objectIdentifier,
            LogbookParameterName.objectIdentifier.name());
        logbookParamaters.putParameterValue(LogbookParameterName.objectIdentifierRequest,
            LogbookParameterName.objectIdentifierRequest.name());
        logbookParamaters.putParameterValue(LogbookParameterName.objectIdentifierIncome,
            LogbookParameterName.objectIdentifierIncome.name());
    }

    @Test
    public void selectTest() throws LogbookClientException, InvalidParseOperationException {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        assertEquals("aedqaaaaacaam7mxaaaamakvhiv4rsiaaa1",
            client.selectOperation(JsonHandler.getFromString(request)).get("$results").get(1).get("_id").asText());
        assertEquals("aeaqaaaaaefex4j4aao2qalmjv7h24yaaaaq",
            client.selectOperationById("eventIdentifier").get("$results").get(0)
                .get("_id").asText());
    }

    @Test
    public void bulkTest()
        throws LogbookClientAlreadyExistsException, LogbookClientBadRequestException, LogbookClientServerException,
        LogbookClientNotFoundException {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        final LogbookOperationParameters logbookParameters = LogbookParameterHelper.newLogbookOperationParameters();
        fillLogbookParamaters(logbookParameters);
        client.createDelegate(logbookParameters);
        client.updateDelegate(logbookParameters);
        client.commitCreateDelegate(LogbookParameterName.eventIdentifierProcess.name());

        client.updateDelegate(logbookParameters);
        client.updateDelegate(logbookParameters);
        client.commitUpdateDelegate(LogbookParameterName.eventIdentifierProcess.name());

        final List<LogbookOperationParameters> list = new ArrayList<>();
        list.add(logbookParameters);
        list.add(logbookParameters);
        client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), list);
        client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), list);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void bulkCreateEmptyQueueTest() throws Exception {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        client.bulkCreate(LogbookParameterName.eventIdentifierProcess.name(), null);
    }

    @Test(expected = LogbookClientBadRequestException.class)
    public void bulkUpdateEmptyQueueTest() throws Exception {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        client.bulkUpdate(LogbookParameterName.eventIdentifierProcess.name(), null);
    }

    @Test
    public void closeExecution() {
        LogbookOperationsClientFactory.changeMode(null);

        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        client.close();
    }

    @Test
    public void traceabilityTest() throws Exception {
        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        RequestResponse<TenantLogbookOperationTraceabilityResult> response =
            client.traceability(Collections.singletonList(0));
        assertNotNull(response);
        assertTrue(response instanceof RequestResponseOK);
    }

    @Test
    public void traceabilityTestObjectGroupLFC() throws Exception {
        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        RequestResponse response = client.traceabilityLfcObjectGroup();
        assertNotNull(response);
        assertTrue(response instanceof RequestResponseOK);
    }

    @Test
    public void traceabilityTestUnitLFC() throws Exception {
        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        RequestResponse response = client.traceabilityLfcUnit();
        assertNotNull(response);
        assertTrue(response instanceof RequestResponseOK);
    }


    @Test
    public void launchReindexationTest()
        throws LogbookClientServerException, InvalidParseOperationException {
        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        assertNotNull(client.reindex(new IndexParameters()));
    }

    @Test
    public void switchIndexesTest()
        throws LogbookClientServerException, InvalidParseOperationException {
        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        assertNotNull(client.switchIndexes(new SwitchIndexParameters()));
    }

    @Test
    public void traceabilityAuditTest()
        throws LogbookClientServerException, InvalidParseOperationException {
        final LogbookOperationsClient client =
            LogbookOperationsClientFactory.getInstance().getClient();
        client.traceabilityAudit(0, new AuditLogbookOptions());
    }

}
