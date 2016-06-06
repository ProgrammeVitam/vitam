/**
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
package fr.gouv.vitam.processing.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.processing.common.config.ServerConfiguration;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * 
 */
public class ContainerExtractionUtilsTest {

    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private ContainerExtractionUtils containerExtractionUtils;

    private final String SIP = "SIP";
    private final String SLASH = "/";
    private final String CONTENT = "Content";
    private final String folder = new StringBuilder().append(SIP).append(SLASH).append(CONTENT).toString();

    private WorkParams workParams =
        new WorkParams().setGuuid("id").setServerConfiguration(new ServerConfiguration().setUrlWorkspace("ws"))
            .setObjectName(folder);

    private List<URI> uriListWorkspace = new ArrayList<>();


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        uriListWorkspace.add(new URI("content/file1.pdf"));
        uriListWorkspace.add(new URI("content/file2.pdf"));
    }

    @Test
    public void givenWorkspaceExistWhenGetUriListThenReturnOK() throws ProcessingException {

        when(workspaceClientFactory.create(anyObject())).thenReturn(workspaceClient);
        when(workspaceClient.getListUriDigitalObjectFromFolder(anyObject(), anyObject())).thenReturn(uriListWorkspace);

        containerExtractionUtils = new ContainerExtractionUtils(workspaceClientFactory);
        uriListWorkspace = containerExtractionUtils.getDigitalObjectUriListFromWorkspace(workParams);

        assertThat(uriListWorkspace).isNotNull().isNotEmpty();
    }

}
