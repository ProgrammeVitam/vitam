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
package fr.gouv.vitam.processing.common.model;

import fr.gouv.vitam.common.model.StatusCode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProcessResponseTest {

    private static String TEST = "test";

    @Test
    public void testConstructor() {
        final ProcessResponse processResponse = new ProcessResponse();
        assertEquals(StatusCode.WARNING, processResponse.getStatus());
        assertTrue(processResponse.getOutcomeMessages().isEmpty());
        assertTrue(processResponse.getStepResponses().isEmpty());

        assertEquals(StatusCode.OK.name(), processResponse.setStatus(StatusCode.OK).getStatus().name());
        final ArrayList<EngineResponse> list = new ArrayList<>();
        list.add(processResponse.setStatus(StatusCode.WARNING));
        assertEquals(StatusCode.WARNING, processResponse.getGlobalProcessStatusCode(list));

        assertFalse(
            processResponse.setOutcomeMessages("id", OutcomeMessage.CHECK_CONFORMITY_KO).getOutcomeMessages().isEmpty()
        );

        assertEquals(0, new ProcessResponse().getErrorNumber());
        final ArrayList<String> detailMessages = new ArrayList<>();
        detailMessages.add(TEST);
        assertEquals(1, processResponse.setErrorNumber(detailMessages.size()).getErrorNumber());
        final Map<String, List<EngineResponse>> stepResponses = new HashMap<>();
        final List<EngineResponse> processResponses = new ArrayList<>();
        processResponses.add(processResponse.setStatus(StatusCode.KO));
        processResponses.add(new ProcessResponse().setStatus(StatusCode.OK));

        stepResponses.put("id", processResponses);
        assertFalse(processResponse.setStepResponses(stepResponses).getStepResponses().isEmpty());

        assertEquals(StatusCode.KO.name(), processResponse.getValue());
        assertEquals(StatusCode.FATAL.name(), new ProcessResponse().getValue());

        processResponse.setMessageIdentifier(TEST);
        assertEquals(TEST, processResponse.getMessageIdentifier());
        assertEquals("", new ProcessResponse().getMessageIdentifier());
        assertEquals(
            "id KO : 1\n" + ". Nombre total d'erreurs : 1",
            ProcessResponse.getGlobalProcessOutcomeMessage(processResponses)
        );
        assertEquals(TEST, ProcessResponse.getMessageIdentifierFromResponse(processResponses));

        processResponse.setProcessId(TEST);
        assertEquals(TEST, processResponse.getProcessId());
    }
}
