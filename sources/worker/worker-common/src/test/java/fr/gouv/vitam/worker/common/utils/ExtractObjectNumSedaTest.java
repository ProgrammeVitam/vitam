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
package fr.gouv.vitam.worker.common.utils;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExtractObjectNumSedaTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private static final String SIP = "sip1.xml";
    private WorkspaceClient client;
    private WorkspaceClientFactory workspaceClientFactory;
    private final InputStream seda;
    private SedaUtils utils;

    public ExtractObjectNumSedaTest() throws FileNotFoundException {
        seda = PropertiesUtils.getResourceAsStream(SIP);
    }

    @Before
    public void setUp() {
        client = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        when(workspaceClientFactory.getClient()).thenReturn(client);
    }

    @Test
    public void givenListUriNotEmpty() throws Exception {
        when(client.getObject(any(), any())).thenReturn(Response.status(Status.OK).entity(seda).build());
        final HandlerIO handlerIO = mock(HandlerIO.class);
        when(handlerIO.getWorkspaceClientFactory()).thenReturn(workspaceClientFactory);
        when(handlerIO.isExistingFileInWorkspace(any())).thenReturn(true);
        when(handlerIO.getJsonFromWorkspace(any())).thenReturn(
            JsonHandler.toJsonNode(
                new SedaIngestParams(
                    SupportedSedaVersions.SEDA_2_1.getVersion(),
                    SupportedSedaVersions.SEDA_2_1.getNamespaceURI()
                )
            )
        );
        when(handlerIO.getInputStreamFromWorkspace(any())).thenReturn(seda);
        utils = SedaUtilsFactory.getInstance().createSedaUtilsWithSedaIngestParams(handlerIO);
        final ExtractUriResponse extractUriResponse = utils.getAllDigitalObjectUriFromManifest();

        assertThat(extractUriResponse.getUriSetManifest()).isNotNull().isNotEmpty();
    }
}
