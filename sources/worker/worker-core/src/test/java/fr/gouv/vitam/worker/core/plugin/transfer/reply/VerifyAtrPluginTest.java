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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.Collections;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.ARCHIVE_TRANSFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

public class VerifyAtrPluginTest {

    private static final String ATR_WITH_CARDINALITY_ERROR_XML = "atr-with-cardinality-error.xml";
    private static final String ATR_WITH_UNIFIED_SEDA_XML = "atr-with-unified_seda.xml";
    private static final String ATR_WITH_SEDA_2_1_XML = "atr-with-seda-2.1.xml";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Mock
    private Unmarshaller unmarshaller;

    @Mock
    private JAXBContext jaxbContext;

    @InjectMocks
    private VerifyAtrPlugin verifyAtrPlugin;

    @Mock
    private HandlerIO handlerIO;

    private static final String messageRequestIdentifier = "AWESOME-ID";
    private static final String ATR_FOR_TRANSFER_REPLY_IN_WORKSPACE_XML = "ATR-for-transfer-reply-in-workspace.xml";
    private static final String TRANSFORMED_ATR_FOR_TRANSFER_REPLY_IN_WORKSPACE_XML =
        "transformed-ATR-for-transfer-reply-in-workspace.xml";

    @Before
    public void setup() throws Exception {
        given(logbookOperationsClientFactory.getClient()).willReturn(logbookOperationsClient);
        given(jaxbContext.createUnmarshaller()).willReturn(unmarshaller);
    }

    @Test
    public void should_verify_plugin_return_OK() throws Exception {
        // Given
        mockTransformOperations(OK.name(), true);
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willReturn(
            getLogbookOperation(OK)
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_verify_plugin_with_transfer_operation_warning_return_OK() throws Exception {
        // Given
        mockTransformOperations(WARNING.name(), true);
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willReturn(
            getLogbookOperation(StatusCode.WARNING)
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(OK);
    }

    @Test
    public void should_return_KO_when_ATR_has_ReplyCode_other_than_ko_or_warning() throws Exception {
        // Given
        mockTransformOperations(KO.name(), true);
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willReturn(
            getLogbookOperation(StatusCode.KO)
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_FATAL_when_unexpected_error() throws Exception {
        // Given
        mockTransformOperations(KO.name(), false);
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willThrow(
            new JAXBException("Error")
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(FATAL);
    }

    @Test
    public void should_return_KO_when_ATR_not_valid() throws Exception {
        // Given
        mockTransformOperations(KO.name(), false);
        given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willThrow(
            new UnmarshalException("Error")
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_when_ATR_not_valid_no_MOCK() throws Exception {
        // Given
        VerifyAtrPlugin verifyAtrPlugin = new VerifyAtrPlugin();
        given(handlerIO.getFileFromWorkspace(eq(ATR_FOR_TRANSFER_REPLY_IN_WORKSPACE_XML))).willReturn(
            PropertiesUtils.getResourceFile(ATR_WITH_CARDINALITY_ERROR_XML)
        );
        given(handlerIO.getNewLocalFile(eq(TRANSFORMED_ATR_FOR_TRANSFER_REPLY_IN_WORKSPACE_XML))).willReturn(
            tempFolder.newFile()
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
    }

    @Test
    public void should_return_KO_unknown_transfer_operation() throws Exception {
        // Given
        mockTransformOperations(OK.name(), true);
        given(logbookOperationsClient.selectOperationById(messageRequestIdentifier)).willThrow(
            new LogbookClientNotFoundException("")
        );

        // When
        ItemStatus result = verifyAtrPlugin.execute(null, handlerIO);

        // Then
        assertThat(result.getGlobalStatus()).isEqualTo(KO);
        assertThat(result.getEvDetailData()).isEqualTo(
            "{\"Event\":\"Field MessageRequestIdentifier in ATR does not correspond to an existing transfer operation.\"}"
        );
    }

    private JAXBElement<ArchiveTransferReplyType> getJaxbAtr(String replyCode) {
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

    private void mockTransformOperations(String replyCode, boolean shouldMockUnmarshallStream)
        throws ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException, IOException, JAXBException {
        given(handlerIO.getFileFromWorkspace(eq(ATR_FOR_TRANSFER_REPLY_IN_WORKSPACE_XML))).willReturn(
            PropertiesUtils.getResourceFile(ATR_WITH_SEDA_2_1_XML)
        );
        given(handlerIO.getNewLocalFile(eq(TRANSFORMED_ATR_FOR_TRANSFER_REPLY_IN_WORKSPACE_XML))).willReturn(
            PropertiesUtils.getResourceFile(ATR_WITH_UNIFIED_SEDA_XML)
        );

        if (shouldMockUnmarshallStream) {
            given(unmarshaller.unmarshal(any(XMLStreamReader.class), eq((ArchiveTransferReplyType.class)))).willReturn(
                getJaxbAtr(replyCode)
            );
        }
    }
}
