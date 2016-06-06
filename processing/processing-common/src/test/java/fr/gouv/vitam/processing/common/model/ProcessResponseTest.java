package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

public class ProcessResponseTest {

    @Test
    public void testConstructor() {
        assertEquals(StatusCode.WARNING, new ProcessResponse().getStatus());
        assertTrue(new ProcessResponse().getMessages().isEmpty());
        assertTrue(new ProcessResponse().getStepResponses().isEmpty());

        assertEquals(StatusCode.OK.value(), new ProcessResponse().setStatus(StatusCode.OK).getStatus().value());
        ArrayList<EngineResponse> list = new ArrayList<EngineResponse>();
        list.add(new ProcessResponse().setStatus(StatusCode.WARNING));
        assertEquals(StatusCode.WARNING, new ProcessResponse().getGlobalProcessStatusCode(list));
        assertTrue(new ProcessResponse().setMessages(new ArrayList<>()).getMessages().isEmpty());
        assertTrue(new ProcessResponse().setStepResponses(new HashMap<>()).getStepResponses().isEmpty());
    }
}
