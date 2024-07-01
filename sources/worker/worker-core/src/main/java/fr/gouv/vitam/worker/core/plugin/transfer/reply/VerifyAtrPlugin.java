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
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.culture.archivesdefrance.seda.v2.ArchiveTransferReplyType;
import fr.gouv.culture.archivesdefrance.seda.v2.IdentifierType;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.common.model.logbook.LogbookOperation;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.common.xml.XMLInputFactoryUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.utils.PluginHelper.EventDetails;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.util.XMLCatalogResolver;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.common.model.StatusCode.OK;
import static fr.gouv.vitam.common.model.StatusCode.WARNING;
import static fr.gouv.vitam.common.utils.SupportedSedaVersions.GENERIC_VITAM_VALIDATOR;
import static fr.gouv.vitam.common.xml.ValidationXsdUtils.CATALOG_FILENAME;
import static fr.gouv.vitam.common.xml.ValidationXsdUtils.HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1;
import static fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess.ARCHIVE_TRANSFER;
import static fr.gouv.vitam.worker.core.utils.PluginHelper.buildItemStatus;

public class VerifyAtrPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VerifyAtrPlugin.class);
    public static final String PLUGIN_NAME = "VERIFY_ARCHIVAL_TRANSFER_REPLY";
    private static final URL SEDA_XSD_URL = Objects.requireNonNull(
        ValidationXsdUtils.class.getClassLoader().getResource(GENERIC_VITAM_VALIDATOR)
    );
    private static final URL CATALOG_URL = Objects.requireNonNull(
        ValidationXsdUtils.class.getClassLoader().getResource(CATALOG_FILENAME)
    );
    public static final String ATR_FOR_TRANSFER_REPLY = "ATR-for-transfer-reply-in-workspace.xml";
    private static final String TRANSFORM_XSLT_PATH = "transform.xsl";

    private final JAXBContext jaxbContext;
    private final LogbookOperationsClientFactory logbookOperationsClientFactory;

    private final TransformerFactory transformerFactory;

    public VerifyAtrPlugin() throws Exception {
        this(JAXBContext.newInstance(ArchiveTransferReplyType.class), LogbookOperationsClientFactory.getInstance());
    }

    @VisibleForTesting
    public VerifyAtrPlugin(JAXBContext jaxbContext, LogbookOperationsClientFactory logbookOperationsClientFactory) {
        this.jaxbContext = jaxbContext;
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.transformerFactory = TransformerFactory.newInstance();
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        XMLStreamReader xmlStreamReader = null;
        try (InputStream atr = getTransformedXmlAsInputStream(handler)) {
            xmlStreamReader = XMLInputFactoryUtils.newInstance().createXMLStreamReader(atr, "UTF-8");
            Unmarshaller unmarshaller = createUnmarshaller();
            ArchiveTransferReplyType transferReply = unmarshaller
                .unmarshal(xmlStreamReader, ArchiveTransferReplyType.class)
                .getValue();

            handler.addOutputResult(0, transferReply);

            if (
                hasExistingTransferOperation(transferReply.getMessageRequestIdentifier()) &&
                hasReplyCode(transferReply.getReplyCode())
            ) {
                return buildItemStatus(PLUGIN_NAME, OK, EventDetails.of("ATR file is valid and serialized."));
            }

            return buildItemStatus(
                PLUGIN_NAME,
                KO,
                EventDetails.of(
                    "Field MessageRequestIdentifier in ATR does not correspond to an existing transfer operation."
                )
            );
        } catch (UnmarshalException e) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, KO, EventDetails.of(e.getMessage()));
        } catch (
            JAXBException
            | ContentAddressableStorageNotFoundException
            | IOException
            | XMLStreamException
            | LogbookClientException
            | InvalidParseOperationException
            | ContentAddressableStorageServerException
            | SAXException
            | TransformerException e
        ) {
            LOGGER.error(e);
            return buildItemStatus(PLUGIN_NAME, FATAL, EventDetails.of(e.getMessage()));
        } finally {
            closeXmlReader(xmlStreamReader);
        }
    }

    private InputStream getTransformedXmlAsInputStream(HandlerIO handlerIO)
        throws TransformerException, IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {
        File originalATR = handlerIO.getFileFromWorkspace(ATR_FOR_TRANSFER_REPLY);
        File transformedATR = handlerIO.getNewLocalFile("transformed-" + ATR_FOR_TRANSFER_REPLY);
        Source xsl = new StreamSource(PropertiesUtils.getResourceAsStream(TRANSFORM_XSLT_PATH));
        Transformer transformer = transformerFactory.newTransformer(xsl);
        transformer.setErrorListener(
            new ErrorListener() {
                @Override
                public void warning(TransformerException exception) {
                    LOGGER.warn("An error occurred while processing ATR transformation", exception);
                }

                @Override
                public void error(TransformerException exception) throws TransformerException {
                    throw exception;
                }

                @Override
                public void fatalError(TransformerException exception) throws TransformerException {
                    throw exception;
                }
            }
        );

        transformer.transform(new StreamSource(originalATR), new StreamResult(transformedATR));

        return new FileInputStream(transformedATR);
    }

    private boolean hasReplyCode(String replyCode) {
        return replyCode.equals("OK") || replyCode.equals("WARNING");
    }

    private boolean hasExistingTransferOperation(IdentifierType messageRequestIdentifier)
        throws InvalidParseOperationException, LogbookClientException {
        if (messageRequestIdentifier == null || StringUtils.isBlank(messageRequestIdentifier.getValue())) {
            return false;
        }

        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            JsonNode result = client.selectOperationById(messageRequestIdentifier.getValue());
            RequestResponseOK<JsonNode> resultResponseOk = RequestResponseOK.getFromJsonNode(result);
            if (resultResponseOk.isEmpty() || !resultResponseOk.isOk()) {
                return false;
            }

            LogbookOperation transferOperation = JsonHandler.getFromJsonNode(
                resultResponseOk.getFirstResult(),
                LogbookOperation.class
            );
            if (!transferOperation.getEvType().equals(ARCHIVE_TRANSFER.name())) {
                return false;
            }

            List<LogbookEventOperation> events = transferOperation.getEvents();
            if (events.isEmpty()) {
                return false;
            }

            LogbookEventOperation lastTransferEventOperation = events.get(events.size() - 1);
            if (
                !lastTransferEventOperation.getOutcome().equals(OK.name()) &&
                !lastTransferEventOperation.getOutcome().equals(WARNING.name())
            ) {
                return false;
            }
        } catch (LogbookClientNotFoundException e) {
            return false;
        }

        return true;
    }

    private void closeXmlReader(XMLStreamReader xmlStreamReader) throws ProcessingException {
        try {
            if (xmlStreamReader != null) {
                xmlStreamReader.close();
            }
        } catch (XMLStreamException e) {
            throw new ProcessingException(e);
        }
    }

    private Unmarshaller createUnmarshaller() throws JAXBException, SAXException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setSchema(getSchema());
        return unmarshaller;
    }

    static Schema getSchema() throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1);
        schemaFactory.setResourceResolver(new XMLCatalogResolver(new String[] { CATALOG_URL.toString() }, false));
        return schemaFactory.newSchema(SEDA_XSD_URL);
    }
}
