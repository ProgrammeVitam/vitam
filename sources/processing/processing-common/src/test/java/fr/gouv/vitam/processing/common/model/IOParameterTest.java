package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import org.junit.Test;

public class IOParameterTest {

    private static final String WORKSPACE_FILE = "WORKSPACE:file";
    private static final String TEST = "test";
    private static final String IN = "in";

    @Test
    public void testGetterSetter() throws URISyntaxException {
        final IOParameter param = new IOParameter();
        param.setUri(new ProcessingUri(WORKSPACE_FILE)).setName(IN).setValue(TEST);

        param.setUri(new ProcessingUri(WORKSPACE_FILE)).setName(IN).setValue(TEST).setOptional(true);
        assertEquals(param.getUri().getPath(), "file");
        assertEquals(param.getUri().getPrefix(), UriPrefix.WORKSPACE);
        assertEquals(param.getName(), IN);
        assertEquals(param.getValue(), TEST);
    }

}
