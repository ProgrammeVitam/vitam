package fr.gouv.vitam.processing.worker.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.BinaryObjectInfo;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;

public class StoreObjectGroupActionHandlerTest {

    StoreObjectGroupActionHandler handler;
    private static final String HANDLER_ID = "StoreObjectGroup";
    private SedaUtilsFactory factory;
    private SedaUtils sedaUtils;

    @Before
    public void setUp() {
        factory = mock(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseKO()
        throws XMLStreamException, IOException, ProcessingException {
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils)
            .retrieveStorageInformationForObjectGroup(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new StoreObjectGroupActionHandler(factory);
        assertEquals(StoreObjectGroupActionHandler.getId(), HANDLER_ID);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""))
                .setGuuid("objectGroupId")
                .setObjectName("objectGroupId.json").setContainerName("containerName").setCurrentStep("INGEST");
        final EngineResponse response = handler.execute(params);
        assertEquals(response.getStatus(), StatusCode.KO);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK()
        throws XMLStreamException, IOException, ProcessingException, URISyntaxException {
        Mockito.doReturn(retrieveListOfInfo()).when(sedaUtils).retrieveStorageInformationForObjectGroup(anyObject());
        when(factory.create()).thenReturn(sedaUtils);
        handler = new StoreObjectGroupActionHandler(factory);
        final WorkParams params =
            new WorkParams().setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""))
                .setGuuid("objectGroupId").setObjectName("objectGroupId.json").setContainerName("containerName")
                .setCurrentStep("INGEST");
        final EngineResponse response = handler.execute(params);
        assertEquals(response.getStatus(), StatusCode.OK);
    }

    public Map<String,BinaryObjectInfo> retrieveListOfInfo() throws URISyntaxException {
        Map<String, BinaryObjectInfo> infos = new HashMap<>();
        BinaryObjectInfo info = new BinaryObjectInfo();
        info.setId("bdoId");
        info.setUri(new URI("content/totototototototo"));
        infos.put("guid1",info);
        return infos;
    }

}
