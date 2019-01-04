package fr.gouv.vitam.processing.common.model;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;

import org.junit.Test;

import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;

public class IOParameterTest {

    private static final String WORKSPACE_FILE = "WORKSPACE:file";
    private static final String IN = "in";

    @Test
    public void testGetterSetter() {
        final IOParameter param = new IOParameter();
        param.setUri(new ProcessingUri(WORKSPACE_FILE)).setName(IN);

        param.setUri(new ProcessingUri(WORKSPACE_FILE)).setName(IN).setOptional(true);
        assertEquals(param.getUri().getPath(), "file");
        assertEquals(param.getUri().getPrefix(), UriPrefix.WORKSPACE);
        assertEquals(param.getName(), IN);
    }

}
