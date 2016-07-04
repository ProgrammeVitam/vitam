/*******************************************************************************
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
 *******************************************************************************/
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

    private final List<URI> uriDuplicatedListManifestKO = new ArrayList<>();
    private final List<URI> uriListManifestOK = new ArrayList<>();
    private final List<URI> uriOutNumberListManifestKO = new ArrayList<>();

    private final List<URI> uriListWorkspaceOK = new ArrayList<>();
    private final List<URI> uriOutNumberListWorkspaceKO = new ArrayList<>();

    private ExtractUriResponse extractUriResponseOK;
    private ExtractUriResponse extractDuplicatedUriResponseKO;
    private ExtractUriResponse extractOutNumberUriResponseKO;

    private final List<String> messages = new ArrayList<>();


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
            .setErrorDuplicateUri(Boolean.TRUE).setDetailMessages(messages);

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
        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
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
        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
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


        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.OK);
        assertThat(response.getOutcomeMessages()).hasSize(1);
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

        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getOutcomeMessages()).isNotNull().isNotEmpty();
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

        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getOutcomeMessages()).isNotNull().isNotEmpty();
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

        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getOutcomeMessages()).isNotNull().isNotEmpty();
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

        final EngineResponse response = checkObjectsNumberActionHandler.execute(workParams);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StatusCode.KO);
        assertThat(response.getOutcomeMessages()).isNotNull().isNotEmpty();
    }
}
