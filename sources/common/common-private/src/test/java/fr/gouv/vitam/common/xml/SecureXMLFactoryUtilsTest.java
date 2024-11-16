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

package fr.gouv.vitam.common.xml;

import fr.gouv.vitam.common.PropertiesUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SecureXMLFactoryUtilsTest {

    private static final String XML_FILE_WITH_XXE = "xml/xml_document_with_xxe.xml";
    private static final String XML_FILE_OK = "xml/xml_document_ok.xml";
    private static final String XSLT_FILE_OK = "xml/xslt_ok.xml";
    private static final String XSLT_FILE_WITH_XXE = "xml/xslt_with_xxe.xml";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public final void shouldSecureXmlEventReaderRejectXmlWithExternalEntity() throws IOException, XMLStreamException {
        XMLEventReader eventReader = SecureXMLFactoryUtils.createSecureXMLEventReader(
            PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE)
        );
        assertThatThrownBy(() -> eventReaderToText(eventReader))
            .isInstanceOf(XMLStreamException.class)
            .hasMessageContaining("\"xxe\"");
    }

    @Test
    public final void shouldUnsecureXmlEventReaderInjectXmlWithExternalEntity() throws IOException, XMLStreamException {
        XMLEventReader eventReader = XMLInputFactory.newInstance()
            .createXMLEventReader(PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE));
        String textContent = eventReaderToText(eventReader);
        assertThat(textContent).contains("root:x:0:0");
    }

    @Test
    public final void shouldSecureXmlStreamReaderRejectXmlWithExternalEntity() throws IOException, XMLStreamException {
        XMLStreamReader reader = SecureXMLFactoryUtils.createSecureXMLStreamReader(
            PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE)
        );
        assertThatThrownBy(() -> streamReaderToText(reader))
            .isInstanceOf(XMLStreamException.class)
            .hasMessageContaining("\"xxe\"");
    }

    @Test
    public final void shouldSecureXmlStreamReaderRejectXmlReaderWithExternalEntity()
        throws IOException, XMLStreamException {
        XMLStreamReader reader = SecureXMLFactoryUtils.createSecureXMLStreamReader(
            new InputStreamReader(PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE))
        );
        assertThatThrownBy(() -> streamReaderToText(reader))
            .isInstanceOf(XMLStreamException.class)
            .hasMessageContaining("\"xxe\"");
    }

    @Test
    public final void shouldUnsecureXmlStreamReaderInjectXmlWithExternalEntity()
        throws IOException, XMLStreamException {
        XMLStreamReader reader = XMLInputFactory.newInstance()
            .createXMLStreamReader(PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE));
        String text = streamReaderToText(reader);
        assertThat(text).contains("root:x:0:0");
    }

    @Test
    public final void shouldUnsecureXmlStreamReaderInjectXmlReaderWithExternalEntity()
        throws IOException, XMLStreamException {
        XMLStreamReader reader = XMLInputFactory.newInstance()
            .createXMLStreamReader(new InputStreamReader(PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE)));
        String text = streamReaderToText(reader);
        assertThat(text).contains("root:x:0:0");
    }

    @Test
    public void shouldCreateSecureDocumentBuilderFactoryParseValidXmlDocument() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = SecureXMLFactoryUtils.createSecureDocumentBuilderFactory();

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        Document parse = documentBuilder.parse(PropertiesUtils.getResourceAsStream(XML_FILE_OK));
        assertThat(parse.getDocumentElement().getTextContent()).isEqualTo("A");
    }

    @Test
    public void shouldCreateSecureDocumentBuilderFactoryParseFailsForXmlDocumentWithXXE() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = SecureXMLFactoryUtils.createSecureDocumentBuilderFactory();

        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        assertThatThrownBy(() -> documentBuilder.parse(PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE)))
            .isInstanceOf(SAXParseException.class)
            .hasMessageContaining("DOCTYPE is disallowed when the feature");
    }

    @Test
    public void shouldValidateXmlFileSucceedWhenFileValid() {
        assertThatCode(
            () -> SecureXMLFactoryUtils.validateXmlFile(PropertiesUtils.getResourceFile(XML_FILE_OK))
        ).doesNotThrowAnyException();
    }

    @Test
    public void shouldValidateXmlFileFailWhenFileHasXXE() {
        assertThatThrownBy(
            () -> SecureXMLFactoryUtils.validateXmlFile(PropertiesUtils.getResourceFile(XML_FILE_WITH_XXE))
        )
            .isInstanceOf(InvalidXmlException.class)
            .hasMessageContaining("DOCTYPE is disallowed when the feature");
    }

    @Test
    public void shouldCreateSecureTransformerSucceed() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        SecureXMLFactoryUtils.xsltTransform(
            PropertiesUtils.getResourceAsStream(XSLT_FILE_OK),
            PropertiesUtils.getResourceAsStream(XML_FILE_OK),
            outputStream
        );
        String outputXml = outputStream.toString(StandardCharsets.UTF_8);
        assertThat(outputXml).doesNotContain("root:x:0:0");
        assertThat(outputXml).contains("<MyNewElement>NewA</MyNewElement>");
    }

    @Test
    public void shouldRejectXmlTransformationForXmlWithExternalEntity() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        assertThatThrownBy(
            () ->
                SecureXMLFactoryUtils.xsltTransform(
                    PropertiesUtils.getResourceAsStream(XSLT_FILE_OK),
                    PropertiesUtils.getResourceAsStream(XML_FILE_WITH_XXE),
                    outputStream
                )
        ).hasMessageContaining("accessExternalDTD");
    }

    @Test
    public void shouldCreateSecureTransformerRejectInvalidXsltFile() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThatThrownBy(
            () ->
                SecureXMLFactoryUtils.xsltTransform(
                    PropertiesUtils.getResourceAsStream(XSLT_FILE_WITH_XXE),
                    PropertiesUtils.getResourceAsStream(XML_FILE_OK),
                    outputStream
                )
        ).hasMessageContaining("accessExternalDTD");
    }

    private static String eventReaderToText(XMLEventReader eventReader) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        while (eventReader.hasNext()) {
            final XMLEvent event = eventReader.nextEvent();
            if (event.isCharacters()) {
                sb.append(event.asCharacters().getData());
            }
        }
        return sb.toString();
    }

    private static String streamReaderToText(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder sb = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.CHARACTERS) { // Read text content between elements
                if (reader.hasText()) {
                    sb.append(reader.getText().trim());
                }
            }
        }
        return sb.toString();
    }
}
