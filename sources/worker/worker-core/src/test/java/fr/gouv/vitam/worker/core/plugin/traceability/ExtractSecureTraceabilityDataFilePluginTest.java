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

package fr.gouv.vitam.worker.core.plugin.traceability;

import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.InputStream;

import static fr.gouv.vitam.common.model.WorkspaceConstants.TRACEABILITY_OPERATION_DIRECTORY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExtractSecureTraceabilityDataFilePluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private WorkspaceClient workspaceClient;

    private ExtractSecureTraceabilityDataFilePlugin extractSecureTraceabilityDataFilePlugin;

    private static final String CONTAINER_NAME = "CONTAINER_NAME";
    private static final String OBJECT_NAME = "OBJECT_NAME";
    private static final String RANDOM_STRING = "tmp";

    private static final String zipFolder = TRACEABILITY_OPERATION_DIRECTORY + File.separator + OBJECT_NAME;

    @Before
    public void setUp() {
        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        extractSecureTraceabilityDataFilePlugin = new ExtractSecureTraceabilityDataFilePlugin(workspaceClientFactory);
    }

    @Test
    public void should_extract_zip_file_without_error() throws Exception {
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handler = mock(HandlerIO.class);
        File file = File.createTempFile(RANDOM_STRING, RANDOM_STRING);
        when(param.getContainerName()).thenReturn(CONTAINER_NAME);
        when(param.getObjectName()).thenReturn(OBJECT_NAME);
        when(handler.getInput(eq(0))).thenReturn(file);

        ItemStatus itemStatus = extractSecureTraceabilityDataFilePlugin.execute(param, handler);

        verify(workspaceClient).uncompressObject(
            eq(CONTAINER_NAME),
            eq(zipFolder),
            eq(CommonMediaType.ZIP),
            any(InputStream.class)
        );
        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
    }

    @Test
    public void should_delete_zip_folder_if_exists() throws Exception {
        WorkerParameters param = mock(WorkerParameters.class);
        HandlerIO handler = mock(HandlerIO.class);

        File file = File.createTempFile(RANDOM_STRING, RANDOM_STRING);

        when(param.getContainerName()).thenReturn(CONTAINER_NAME);
        when(param.getObjectName()).thenReturn(OBJECT_NAME);
        when(handler.getInput(eq(0))).thenReturn(file);
        when(workspaceClient.isExistingFolder(eq(CONTAINER_NAME), eq(zipFolder))).thenReturn(true);

        ItemStatus itemStatus = extractSecureTraceabilityDataFilePlugin.execute(param, handler);

        verify(workspaceClient).deleteObject(eq(CONTAINER_NAME), eq(zipFolder));
        assertEquals(StatusCode.OK, itemStatus.getGlobalStatus());
    }
}
