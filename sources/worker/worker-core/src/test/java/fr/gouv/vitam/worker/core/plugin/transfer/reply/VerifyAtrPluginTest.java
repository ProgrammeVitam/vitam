/*
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
package fr.gouv.vitam.worker.core.plugin.transfer.reply;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.worker.core.plugin.preservation.TestHandlerIO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.util.Collections;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.ARCHIVE_TRANSFER;
import static fr.gouv.vitam.worker.core.plugin.transfer.reply.VerifyAtrPlugin.getSchema;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class VerifyAtrPluginTest {
    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private Unmarshaller unmarshaller;

    @InjectMocks
    private VerifyAtrPlugin verifyAtrPlugin;

    @Before
    public void setup() {
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
    }

    @Test
    public void should_verify_plugin_return_OK() throws Exception {
        // Given
        String messageRequestIdentifier = "AWESOME-ID";
        String replyCode = "OK";
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willReturn(getJaxbAtr(messageRequestIdentifier,replyCode));
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willReturn(getLogbookOperation(OK));
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(new ByteArrayInputStream("<ArchiveTransferReply></ArchiveTransferReply>".getBytes()));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_verify_plugin_with_transfer_operation_warning_return_OK() throws Exception {
        // Given
        String messageRequestIdentifier = "AWESOME-ID";
        String replyCode = "WARNING";
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willReturn(getJaxbAtr(messageRequestIdentifier,replyCode));
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willReturn(getLogbookOperation(StatusCode.WARNING));
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(new ByteArrayInputStream("<ArchiveTransferReply></ArchiveTransferReply>".getBytes()));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_return_KO_when_ATR_has_ReplyCode_other_than_ko_or_warning() throws Exception {
        // Given
        String messageRequestIdentifier = "AWESOME-ID";
        String replyCode = "KO";
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willReturn(getJaxbAtr(messageRequestIdentifier,replyCode));
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willReturn(getLogbookOperation(StatusCode.KO));
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(new ByteArrayInputStream("<ArchiveTransferReply></ArchiveTransferReply>".getBytes()));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_FATAL_when_unexpected_error() throws Exception {
        // Given
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willThrow(new JAXBException("Error"));
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(new ByteArrayInputStream("<ArchiveTransferReply></ArchiveTransferReply>".getBytes()));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    public void should_return_KO_when_ATR_not_valid() throws Exception {
        // Given
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willThrow(new UnmarshalException("Error"));
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(new ByteArrayInputStream("<ArchiveTransferReply></ArchiveTransferReply>".getBytes()));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_when_ATR_not_valid_no_MOCK() throws Exception {
        // Given
        VerifyAtrPlugin verifyAtrPlugin = new VerifyAtrPlugin(
            JAXBContext.newInstance(ArchiveTransferReplyType.class).createUnmarshaller(),
            getSchema(),
            logbookOperationsClientFactory
        );
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(getClass().getResourceAsStream("/atr-with-cardinality-error.xml"));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_unknown_transfer_operation() throws Exception {
        // Given
        String messageRequestIdentifier = "AWESOME-ID";
        String replyCode = "OK";
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willReturn(getJaxbAtr(messageRequestIdentifier,replyCode));
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willThrow(new LogbookClientNotFoundException(""));
        TestHandlerIO handler = new TestHandlerIO();
        handler.setInputStreamFromWorkspace(new ByteArrayInputStream("<ArchiveTransferReply></ArchiveTransferReply>".getBytes()));

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handler);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
        assertThat(result.getEvDetailData())
            .isEqualTo("{\"Event\":\"Field MessageRequestIdentifier in ATR does not correspond to an existing transfer operation.\"}");
    }

    private JAXBElement<ArchiveTransferReplyType> getJaxbAtr(String messageRequestIdentifier, String replyCode) {
        ArchiveTransferReplyType archiveTransferReplyType = new ArchiveTransferReplyType();
        IdentifierType identifier = new IdentifierType();
        identifier.setValue(messageRequestIdentifier);
        archiveTransferReplyType.setMessageRequestIdentifier(identifier);
        archiveTransferReplyType.setReplyCode(replyCode);
        return new JAXBElement<>(
            new QName(ArchiveTransferReplyType.class.getSimpleName()),
            ArchiveTransferReplyType.class,
            archiveTransferReplyType
        );
    }

    private JsonNode getLogbookOperation(StatusCode statusCode) throws InvalidParseOperationException {
        LogbookOperation logbookOperation = new LogbookOperation();
        logbookOperation.setEvType(ARCHIVE_TRANSFER.name());
        LogbookEventOperation lastEvent = new LogbookEventOperation();
        lastEvent.setOutcome(statusCode.name());
        logbookOperation.setEvents(Collections.singletonList(lastEvent));
        RequestResponseOK<JsonNode> responseOK = new RequestResponseOK<>();
        responseOK.addResult(JsonHandler.toJsonNode(logbookOperation));
        return JsonHandler.toJsonNode(responseOK);
    }
}
