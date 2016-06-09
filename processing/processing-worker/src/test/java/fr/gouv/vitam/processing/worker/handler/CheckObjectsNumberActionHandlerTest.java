package fr.gouv.vitam.processing.worker.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.ContainerExtractionUtils;
import fr.gouv.vitam.processing.common.utils.ContainerExtractionUtilsFactory;
import fr.gouv.vitam.processing.common.utils.ExtractUriResponse;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

public class CheckObjectsNumberActionHandlerTest {

    private CheckObjectsNumberActionHandler checkObjectsNumberActionHandler;
    private static final String HANDLER_ID = "CheckObjectsNumber";

    private WorkParams workParams;

    private SedaUtilsFactory sedaFactory;
    private SedaUtils sedaUtils;

    private ContainerExtractionUtilsFactory containerExtractionUtilsFactory;
    private ContainerExtractionUtils containerExtractionUtils;

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private List<URI> uriDuplicatedListManifestKO = new ArrayList<>();
    private List<URI> uriListManifestOK = new ArrayList<>();
    private List<URI> uriOutNumberListManifestKO = new ArrayList<>();

    private List<URI> uriListWorkspaceOK = new ArrayList<>();
    private List<URI> uriOutNumberListWorkspaceKO = new ArrayList<>();

    private ExtractUriResponse extractUriResponseOK;
    private ExtractUriResponse extractDuplicatedUriResponseKO;
    private ExtractUriResponse extractOutNumberUriResponseKO;

    private List<String> messages = new ArrayList<>();


    @Before
    public void setUp() throws Exception {
        workParams = new WorkParams();
        workParams.setGuuid("").setContainerName("").setObjectName("")
            .setServerConfiguration(new ServerConfiguration().setUrlWorkspace(""));

        sedaFactory = mock(SedaUtilsFactory.class);
        sedaUtils = mock(SedaUtils.class);

        containerExtractionUtilsFactory = mock(ContainerExtractionUtilsFactory.class);
        containerExtractionUtils = mock(ContainerExtractionUtils.class);

        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        // URI LIST MANIFEST
        uriDuplicatedListManifestKO.add(new URI("content/file1.pdf"));
        uriDuplicatedListManifestKO.add(new URI("content/file1.pdf"));

        uriListManifestOK.add(new URI("content/file1.pdf"));
        uriListManifestOK.add(new URI("content/file2.pdf"));

        uriOutNumberListManifestKO.add(new URI("content/file1.pdf"));
        uriOutNumberListManifestKO.add(new URI("content/file2.pdf"));
        uriOutNumberListManifestKO.add(new URI("content/file3.pdf"));

        // URI LIST WORKSPACE

        uriListWorkspaceOK.add(new URI("content/file1.pdf"));
        uriListWorkspaceOK.add(new URI("content/file2.pdf"));

        uriOutNumberListWorkspaceKO.add(new URI("content/file1.pdf"));
        uriOutNumberListWorkspaceKO.add(new URI("content/file2.pdf"));
        uriOutNumberListWorkspaceKO.add(new URI("content/file3.pdf"));

        extractUriResponseOK = new ExtractUriResponse();
        extractUriResponseOK.setUriListManifest(uriListManifestOK);

        messages.add("Duplicated digital objects " + "content/file1.pdf");
        extractDuplicatedUriResponseKO = new ExtractUriResponse();
        extractDuplicatedUriResponseKO.setUriListManifest(uriDuplicatedListManifestKO)
            .setErrorDuplicateUri(Boolean.TRUE).setMessages(messages);

        extractOutNumberUriResponseKO = new ExtractUriResponse();
        extractOutNumberUriResponseKO.setUriListManifest(uriOutNumberListManifestKO);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenRaiseXMLStreamExceptionAndReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {
        when(sedaFactory.create()).thenReturn(sedaUtils);
        Mockito.doThrow(new XMLStreamException("")).when(sedaUtils).getAllDigitalObjectUriFromManifest(anyObject());

        when(containerExtractionUtilsFactory.create()).thenReturn(containerExtractionUtils);

        // when(workspaceClient.getObject(anyObject(), anyObject())).thenReturn(seda);
        when(workspaceClientFactory.create(anyObject())).thenReturn(workspaceClient);

        containerExtractionUtils = new ContainerExtractionUtils(workspaceClientFactory);

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);
        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);
        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenRaiseProcessingExceptionReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {

        when(sedaFactory.create()).thenReturn(sedaUtils);
        Mockito.doThrow(new ProcessingException("")).when(sedaUtils).getAllDigitalObjectUriFromManifest(anyObject());

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);
        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);
        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.FATAL);
    }

    @Test
    public void givenWorkpaceExistWhenExecuteThenReturnResponseOK()
        throws XMLStreamException, IOException, ProcessingException {

        when(sedaFactory.create()).thenReturn(sedaUtils);
        when(containerExtractionUtilsFactory.create()).thenReturn(containerExtractionUtils);

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest(anyObject())).thenReturn(extractUriResponseOK);
        when(containerExtractionUtils.getDigitalObjectUriListFromWorkspace(anyObject())).thenReturn(uriListWorkspaceOK);

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);


        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getMessages()).hasSize(1);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndDuplicatedURIManifest()
        throws XMLStreamException, IOException, ProcessingException {

        when(sedaFactory.create()).thenReturn(sedaUtils);
        when(containerExtractionUtilsFactory.create()).thenReturn(containerExtractionUtils);

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest(anyObject())).thenReturn(extractDuplicatedUriResponseKO);
        when(containerExtractionUtils.getDigitalObjectUriListFromWorkspace(anyObject())).thenReturn(uriListWorkspaceOK);

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getMessages()).isNotNull().isNotEmpty();
    }


    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndOutNumberManifest()
        throws XMLStreamException, IOException, ProcessingException {

        when(sedaFactory.create()).thenReturn(sedaUtils);
        when(containerExtractionUtilsFactory.create()).thenReturn(containerExtractionUtils);

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest(anyObject())).thenReturn(extractOutNumberUriResponseKO);
        when(containerExtractionUtils.getDigitalObjectUriListFromWorkspace(anyObject()))
            .thenReturn(uriListWorkspaceOK);

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getMessages()).isNotNull().isNotEmpty();
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKOAndOutNumberWorkspace()
        throws XMLStreamException, IOException, ProcessingException {

        when(sedaFactory.create()).thenReturn(sedaUtils);
        when(containerExtractionUtilsFactory.create()).thenReturn(containerExtractionUtils);

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest(anyObject())).thenReturn(extractUriResponseOK);
        when(containerExtractionUtils.getDigitalObjectUriListFromWorkspace(anyObject()))
            .thenReturn(uriOutNumberListWorkspaceKO);

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getMessages()).isNotNull().isNotEmpty();
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseKAndNotFoundFile()
        throws XMLStreamException, IOException, ProcessingException {

        when(sedaFactory.create()).thenReturn(sedaUtils);
        when(containerExtractionUtilsFactory.create()).thenReturn(containerExtractionUtils);

        checkObjectsNumberActionHandler =
            new CheckObjectsNumberActionHandler(sedaFactory, containerExtractionUtilsFactory);

        when(sedaUtils.getAllDigitalObjectUriFromManifest(anyObject())).thenReturn(extractUriResponseOK);
        when(containerExtractionUtils.getDigitalObjectUriListFromWorkspace(anyObject()))
            .thenReturn(uriOutNumberListWorkspaceKO);

        assertThat(CheckObjectsNumberActionHandler.getId()).isEqualTo(HANDLER_ID);

        EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getMessages()).isNotNull().isNotEmpty();
    }
}
