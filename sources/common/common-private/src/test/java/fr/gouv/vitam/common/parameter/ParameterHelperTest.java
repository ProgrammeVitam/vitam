/*
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
package fr.gouv.vitam.common.parameter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Logbook Paramaters Helper test
 */
public class ParameterHelperTest {

    @Test
    public void checkNullOrEmptyParameters() {
        final Map<TestParameterName, String> parameters = new HashMap<>();
        parameters.put(TestParameterName.outcomeDetail, "field1Value");
        parameters.put(TestParameterName.outcomeDetailMessage, "");

        assertNotNull(parameters);
        assertNotNull(parameters.get(TestParameterName.outcomeDetail));
        assertTrue(Strings.isNullOrEmpty(parameters.get(TestParameterName.outcomeDetailMessage)));

        final Set<TestParameterName> mandatories = new HashSet<>();
        mandatories.add(TestParameterName.outcomeDetail);
        mandatories.add(TestParameterName.outcomeDetailMessage);

        boolean catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);

        mandatories.remove(TestParameterName.outcomeDetailMessage);
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);

        mandatories.add(TestParameterName.outcomeDetailMessage);
        parameters.put(TestParameterName.outcomeDetailMessage, "field2");
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);

        parameters.put(TestParameterName.outcome, null);
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);

        mandatories.add(TestParameterName.outcome);
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);

        parameters.remove(TestParameterName.outcome);
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameters(parameters, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameter(TestParameterName.outcomeDetail, null, mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);
        catchException = false;
        try {
            ParameterHelper.checkNullOrEmptyParameter(TestParameterName.outcomeDetail, "test", mandatories);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);
        catchException = false;
        try {
            final TestParameter params = new TestParameter();
            ParameterHelper.checkNullOrEmptyParameters(params);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertTrue(catchException);
        catchException = false;
        try {
            final TestParameter params = new TestParameter();
            params.setAllMandatoryParameter();
            ParameterHelper.checkNullOrEmptyParameters(params);
        } catch (final IllegalArgumentException iae) {
            catchException = true;
        }
        assertFalse(catchException);
    }

    enum TestParameterName {
        eventIdentifier,
        eventType,
        eventDateTime,
        eventIdentifierProcess,
        eventTypeProcess,
        outcome,
        outcomeDetail,
        outcomeDetailMessage,
        agentIdentifier,
        agentIdentifierApplication,
        agentIdentifierApplicationSession,
        eventIdentifierRequest,
        agentIdentifierSubmission,
        agentIdentifierOriginating,
        objectIdentifier,
        objectIdentifierRequest,
        objectIdentifierIncome
    }

    class TestParameter implements VitamParameter<TestParameterName> {

        private final Set<TestParameterName> mandatory = new HashSet<>();
        private Map<TestParameterName, String> parameters;

        TestParameter() {
            mandatory.add(TestParameterName.eventIdentifier);
            mandatory.add(TestParameterName.eventType);
            mandatory.add(TestParameterName.eventIdentifierProcess);
            mandatory.add(TestParameterName.eventTypeProcess);
            mandatory.add(TestParameterName.outcome);
            mandatory.add(TestParameterName.outcomeDetailMessage);
            mandatory.add(TestParameterName.eventIdentifierRequest);
        }

        public void setAllMandatoryParameter() {
            parameters = new HashMap<>();
            parameters.put(TestParameterName.eventIdentifier, "evntId");
            parameters.put(TestParameterName.eventType, "evntType");
            parameters.put(TestParameterName.eventIdentifierProcess, "evntIdProcess");
            parameters.put(TestParameterName.eventTypeProcess, "evntTypeProcess");
            parameters.put(TestParameterName.outcome, "outcome");
            parameters.put(TestParameterName.outcomeDetailMessage, "outcomeDetailMessage");
            parameters.put(TestParameterName.eventIdentifierRequest, "eventIdentifierRequest");
        }

        @Override
        public Map<TestParameterName, String> getMapParameters() {
            return parameters;
        }

        @Override
        public Set<TestParameterName> getMandatoriesParameters() {
            return mandatory;
        }
    }
}
