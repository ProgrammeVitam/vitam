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
package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SaveAtrPluginTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private HandlerIO handler;

    @Mock
    private StorageClient storageClient;

    @Mock
    private StorageClientFactory storageClientFactory;

    @InjectMocks
    private SaveAtrPlugin saveAtrPlugin;

    private Map<String, File> tempFiles = new HashMap<>();

    @Before
    public void setup() {
        when(storageClientFactory.getClient()).thenReturn(storageClient);

        doAnswer(args -> {
            String filename = args.getArgument(0);
            File file = tempFolder.newFile(filename);
            tempFiles.put(filename, file);
            return file;
        })
            .when(handler)
            .getNewLocalFile((anyString()));
    }

    @Test
    public void should_save_atr_and_context_successfully() throws Exception {
        // Given
        doReturn(createATR()).when(handler).getInput(0);
        doReturn("container").when(handler).getContainerName();

        StoredInfoResult storedInfo = new StoredInfoResult();
        storedInfo.setOfferIds(Collections.emptyList());

        when(storageClient.storeFileFromWorkspace(anyString(), any(), anyString(), any())).thenReturn(storedInfo);

        // When
        ItemStatus execute = saveAtrPlugin.execute(null, handler);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(OK);

        // Saved ATR
        verify(storageClient).storeFileFromWorkspace(anyString(), any(), anyString(), any());
    }

    @Test
    public void should_end_FATAL_in_case_of_storage_error() throws Exception {
        // Given
        doReturn(createATR()).when(handler).getInput(0);
        doReturn("container").when(handler).getContainerName();

        when(storageClient.storeFileFromWorkspace(anyString(), any(), anyString(), any())).thenThrow(
            new StorageNotFoundClientException("ERROR")
        );

        // When
        ItemStatus execute = saveAtrPlugin.execute(null, handler);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(FATAL);
    }

    private ArchiveTransferReplyType createATR() {
        ArchiveTransferReplyType atr = new ArchiveTransferReplyType();
        atr.setMessageIdentifier("ATR_MESSAGE_IDENTIFIER");

        IdentifierType messageRequestIdentifier = new IdentifierType();
        messageRequestIdentifier.setValue("ATR_MESSAGE_REQUEST_IDENTIFIER");
        atr.setMessageRequestIdentifier(messageRequestIdentifier);
        return atr;
    }
}
