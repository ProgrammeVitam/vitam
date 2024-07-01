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
package fr.gouv.vitam.logbook.common.parameters;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Test class for LogbookOperationParameters
 */
public class LogbookOperationParametersTest {

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Test
    public void getMapParameters() {
        final LogbookOperationParameters params = LogbookParameterHelper.newLogbookOperationParameters();
        assertNotNull(params);
        for (final LogbookParameterName value : LogbookParameterName.values()) {
            params.putParameterValue(value, value.name());
        }
        assertEquals(params.getMapParameters().size(), LogbookParameterName.values().length);

        assertEquals(
            params.getMapParameters().get(LogbookParameterName.eventType),
            LogbookParameterName.eventType.name()
        );
        final LogbookOperationParameters params2 = LogbookParameterHelper.newLogbookOperationParameters();
        params2.setFromParameters(params);
        for (final LogbookParameterName value : LogbookParameterName.values()) {
            assertEquals(params.getParameterValue(value), params2.getParameterValue(value));
        }
        final Map<String, String> map = new HashMap<>();
        for (final LogbookParameterName value : LogbookParameterName.values()) {
            map.put(value.name(), params.getParameterValue(value));
        }
        final LogbookOperationParameters params3 = LogbookParameterHelper.newLogbookOperationParameters();
        params3.setMap(map);
        for (final LogbookParameterName value : LogbookParameterName.values()) {
            assertEquals(params.getParameterValue(value), params3.getParameterValue(value));
        }
    }

    @RunWithCustomExecutor
    @Test
    public void getMandatoriesParameters() {
        final LogbookOperationParameters params = LogbookParameterHelper.newLogbookOperationParameters();
        assertNotNull(params);

        final Set<LogbookParameterName> mandatories = params.getMandatoriesParameters();
        assertNotNull(mandatories);

        for (final LogbookParameterName value : mandatories) {
            assertEquals(value.name(), value.toString());
        }

        final Set<LogbookParameterName> mandatoryToAdd = new TreeSet<>();
        mandatoryToAdd.add(LogbookParameterName.objectIdentifier);
        final LogbookOperationParameters params2 = new LogbookOperationParameters(mandatoryToAdd);
        assertNotNull(params2);
        assertEquals(1, params2.getMandatoriesParameters().size());
        assertEquals(null, params.getStatus());
        for (final StatusCode outcome : StatusCode.values()) {
            params.setStatus(outcome);
            assertEquals(outcome, params.getStatus());
        }
        try {
            params.putParameterValue(LogbookParameterName.outcome, "incorrect");
            assertEquals(LogbookParameterName.outcome, params.getStatus());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        assertEquals("incorrect", params.getParameterValue(LogbookParameterName.outcome));
        assertEquals(null, params.getTypeProcess());
        for (final LogbookTypeProcess process : LogbookTypeProcess.values()) {
            params.setTypeProcess(process);
            assertEquals(process, params.getTypeProcess());
        }
        try {
            params.putParameterValue(LogbookParameterName.eventTypeProcess, "incorrect");
            assertEquals(LogbookParameterName.eventTypeProcess, params.getTypeProcess());
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        assertEquals("incorrect", params.getParameterValue(LogbookParameterName.eventTypeProcess));
        assertEquals(null, params.getParameterValue(LogbookParameterName.eventDateTime));
        final GUID aa = GUIDFactory.newEventGUID(0);
        final GUID cc = GUIDFactory.newEventGUID(0);
        LogbookParameterHelper.newLogbookOperationParameters(
            aa,
            "aa",
            aa,
            LogbookTypeProcess.AUDIT,
            StatusCode.OK,
            "Informative Message",
            cc
        );
        try {
            LogbookParameterHelper.newLogbookOperationParameters(
                aa,
                "",
                aa,
                LogbookTypeProcess.AUDIT,
                StatusCode.OK,
                "Informative Message",
                cc
            );
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            LogbookParameterHelper.newLogbookOperationParameters(
                aa,
                "aa",
                null,
                LogbookTypeProcess.AUDIT,
                StatusCode.OK,
                "Informative Message",
                cc
            );
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            LogbookParameterHelper.newLogbookOperationParameters(
                aa,
                "aa",
                aa,
                LogbookTypeProcess.AUDIT,
                null,
                "Informative Message",
                cc
            );
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
        try {
            LogbookParameterHelper.newLogbookOperationParameters(
                aa,
                "aa",
                aa,
                null,
                StatusCode.OK,
                "Informative Message",
                cc
            );
            fail("Should raized an exception");
        } catch (final IllegalArgumentException e) {
            // ignore
        }
    }

    @Test
    public void getEventDateTime() {
        final LogbookOperationParameters params = LogbookParameterHelper.newLogbookOperationParameters();
        assertNotNull(params);
        final LocalDateTime dateTime = LocalDateUtil.now();
        params.getMapParameters().put(LogbookParameterName.eventDateTime, dateTime.toString());
        assertEquals(dateTime, params.getEventDateTime());
    }

    @Test
    public void testConstructorWithEmptyParameters() {
        final LogbookOperationParameters lop = new LogbookOperationParameters(new HashMap());
        assertEquals(true, lop.getMapParameters().isEmpty());
    }

    @Test
    public void testConstructorAndFinalMessage() {
        final LogbookOperationParameters lop = LogbookParameterHelper.newLogbookOperationParameters();
        lop.setFinalStatus("handler", "subtask", StatusCode.STARTED, null, null);
        assertEquals(StatusCode.STARTED, lop.getStatus());
        lop.setFinalStatus("handler", "subtask", StatusCode.OK, " Detail=", "test");
        assertEquals(StatusCode.OK, lop.getStatus());
        lop.setFinalStatus("handler", null, StatusCode.KO, null, null);
        assertEquals(StatusCode.KO, lop.getStatus());
        lop.setFinalStatus("handler", null, StatusCode.FATAL, " Detail=", "test");
        assertEquals(StatusCode.FATAL, lop.getStatus());
    }
}
