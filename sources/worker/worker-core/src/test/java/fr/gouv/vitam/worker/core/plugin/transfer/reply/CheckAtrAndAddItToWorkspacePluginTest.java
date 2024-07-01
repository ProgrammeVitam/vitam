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
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import fr.gouv.vitam.worker.core.plugin.transfer.reply.model.TransferReplyContext;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

public class CheckAtrAndAddItToWorkspacePluginTest {

    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private CheckAtrAndAddItToWorkspacePlugin checkAtrAndAddItToWorkspacePlugin =
        new CheckAtrAndAddItToWorkspacePlugin();

    @Test
    public void should_return_status_ko_when_ATR_not_OK_or_WARNING() throws Exception {
        // Given
        TestHandlerIO handler = new TestHandlerIO();
        handler.addOutputResult(0, createATR(KO.name()));

        // When
        ItemStatus execute = checkAtrAndAddItToWorkspacePlugin.execute(null, handler);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_status_ok_when_ATR_OK_or_WARNING() throws Exception {
        // Given
        TestHandlerIO handler = new TestHandlerIO();
        handler.addOutputResult(0, createATR(WARNING.name()));
        File file = tempFolder.newFile();
        handler.setNewLocalFile(file);
        handler.setOutputWithPath(file.toPath().toString());

        // When
        ItemStatus execute = checkAtrAndAddItToWorkspacePlugin.execute(null, handler);

        // Then
        assertThat(execute.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_return_status_copy_atr_to_workspace_when_ATR_OK_or_WARNING() throws Exception {
        // Given
        TestHandlerIO handler = new TestHandlerIO();
        ArchiveTransferReplyType atr = createATR(WARNING.name());
        handler.addOutputResult(0, atr);
        File file = tempFolder.newFile();
        handler.setNewLocalFile(file);
        handler.setOutputWithPath(file.toPath().toString());

        // When
        ItemStatus execute = checkAtrAndAddItToWorkspacePlugin.execute(null, handler);

        // Then
        assertThat(JsonHandler.getFromFile(file, TransferReplyContext.class)).isEqualTo(
            new TransferReplyContext(atr.getMessageRequestIdentifier().getValue(), atr.getMessageIdentifier())
        );
    }

    private ArchiveTransferReplyType createATR(String name) {
        ArchiveTransferReplyType atr = new ArchiveTransferReplyType();
        atr.setMessageIdentifier("ATR_MESSAGE_IDENTIFIER");
        atr.setReplyCode(name);

        IdentifierType messageRequestIdentifier = new IdentifierType();
        messageRequestIdentifier.setValue("ATR_MESSAGE_REQUEST_IDENTIFIER");
        atr.setMessageRequestIdentifier(messageRequestIdentifier);
        return atr;
    }
}
