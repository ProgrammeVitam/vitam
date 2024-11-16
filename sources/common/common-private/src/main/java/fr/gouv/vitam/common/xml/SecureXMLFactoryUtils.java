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

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;

/**
 * Helper for secure xml factory initialization.
 * See <a href="https://docs.oracle.com/en/java/javase/13/security/java-api-xml-processing-jaxp-security-guide.html">Java API for XML Processing (JAXP) Security Guide</a>
 * See <a href="https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html">OWASP XML External Entity Prevention Cheat Sheet</a>
 * See <a href="https://semgrep.dev/docs/cheat-sheets/java-xxe">XML External entity prevention for Java</a>
 */
public final class SecureXMLFactoryUtils {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SecureXMLFactoryUtils.class);

    private static final String RNG_FACTORY = "com.thaiopensource.relaxng.jaxp.XMLSyntaxSchemaFactory";
    private static final String RNG_PROPERTY_KEY = "javax.xml.validation.SchemaFactory:" + XMLConstants.RELAXNG_NS_URI;
    private static final String HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1 = "http://www.w3.org/XML/XMLSchema/v1.1";
    private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String FEATURES_EXTERNAL_PARAMETER_ENTITIES =
        "http://xml.org/sax/features/external-parameter-entities";
    private static final String NONVALIDATING_LOAD_EXTERNAL_DTD =
        "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private SecureXMLFactoryUtils() {
        // Private constructor to prevent instanciation
    }

    public static XMLEventReader createSecureXMLEventReader(InputStream inputStream) throws XMLStreamException {
        return createSecureXMLInputFactory().createXMLEventReader(inputStream);
    }

    public static XMLStreamReader createSecureXMLStreamReader(InputStream inputStream) throws XMLStreamException {
        return createSecureXMLInputFactory().createXMLStreamReader(inputStream);
    }

    public static XMLStreamReader createSecureXMLStreamReader(Reader reader) throws XMLStreamException {
        return createSecureXMLInputFactory().createXMLStreamReader(reader);
    }

    private static XMLInputFactory createSecureXMLInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        // Disable external entities
        factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);

        return factory;
    }

    /**
     * @deprecated xsd files are properly validated but xml files are not. Please use {@link XsdValidator} which handles xml validation
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    static SchemaFactory internalCreateNotThatSecureXsdSchemaFactory() {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(HTTP_WWW_W3_ORG_XML_XML_SCHEMA_V1_1);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
            return factory;
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @deprecated Security flags are not effective. Please use {@link RngValidator} which handles xml validation
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    static SchemaFactory internalCreateNotThatSecureRngSchemaFactory() {
        try {
            System.setProperty(RNG_PROPERTY_KEY, RNG_FACTORY);
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.RELAXNG_NS_URI);
            // IMPORTANT : Seams ineffective. Please ensure all xml content is manually validated before usage
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return factory;
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static DocumentBuilderFactory createSecureDocumentBuilderFactory() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(NONVALIDATING_LOAD_EXTERNAL_DTD, false);
            return factory;
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static SAXParserFactory createSecureSAXParserFactory() {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
            factory.setXIncludeAware(false);
            factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(FEATURES_EXTERNAL_PARAMETER_ENTITIES, false);
            factory.setFeature(NONVALIDATING_LOAD_EXTERNAL_DTD, false);
            return factory;
        } catch (ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @deprecated Security flags are not effective. Please use {@link RngValidator} or {@link XsdValidator} which handles xml validation
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    static Validator internalCreateNotThatSecureSchemaValidator(Schema schema) {
        try {
            Validator validator = schema.newValidator();
            validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            return validator;
        } catch (SAXNotRecognizedException | SAXNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void validateXmlFile(File xmlFile) throws SAXException {
        try {
            createSecureSAXParserFactory()
                .newSAXParser()
                .parse(
                    xmlFile,
                    new DefaultHandler() {
                        @Override
                        public void warning(SAXParseException e) {
                            LOGGER.warn("validation warning: " + e.getLocalizedMessage(), e);
                        }

                        @Override
                        public void error(SAXParseException e) throws SAXException {
                            throw new SAXException("validation error : " + e.getLocalizedMessage(), e);
                        }

                        @Override
                        public void fatalError(SAXParseException e) throws SAXException {
                            throw new InvalidXmlException("Invalid XML file: " + e.getLocalizedMessage(), e);
                        }
                    }
                );
        } catch (ParserConfigurationException | IOException e) {
            throw new SAXException(e);
        }
    }

    public static void xsltTransform(
        InputStream xsltInputStream,
        InputStream xmlInputStream,
        OutputStream xmlOutputStream
    ) throws TransformerException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            Transformer transformer = factory.newTransformer(new StreamSource(xsltInputStream));
            transformer.setErrorListener(
                new ErrorListener() {
                    @Override
                    public void warning(TransformerException exception) {
                        LOGGER.warn("An error occurred while processing xslt transformation", exception);
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
            transformer.transform(new StreamSource(xmlInputStream), new StreamResult(xmlOutputStream));
        } catch (TransformerConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }
}
