package fr.gouv.vitam.worker.core.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import fr.gouv.vitam.common.PropertiesUtils;

public class HandlerIOTest {

    @Test
    public void testHandlerIO() throws Exception {
        final HandlerIO io = new HandlerIO("");
        final File file = PropertiesUtils.getResourcesFile("sip.xml");
        io.addInput(file);
        io.addOutput(file);
        final HandlerIO ioClass = new HandlerIO("");
        ioClass.addInput(File.class);
        ioClass.addOutput(File.class);
        assertEquals(io.getInput().get(0), file);
        assertEquals(io.getOutput().get(0), file);
        assertTrue(HandlerIO.checkHandlerIO(io, ioClass));
        assertFalse(HandlerIO.checkHandlerIO(io, new HandlerIO("")));
        io.addInput("");
        ioClass.addInput(File.class);
        assertFalse(HandlerIO.checkHandlerIO(io, ioClass));
        io.addOutput("");
        ioClass.addOutput(File.class);
        assertFalse(HandlerIO.checkHandlerIO(io, ioClass));
    }

}
